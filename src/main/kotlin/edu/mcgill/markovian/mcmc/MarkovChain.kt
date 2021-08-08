package edu.mcgill.markovian.mcmc

import com.google.common.util.concurrent.AtomicLongMap
import edu.mcgill.markovian.*
import edu.mcgill.markovian.concurrency.*
import org.apache.datasketches.frequencies.*
import org.apache.datasketches.frequencies.ErrorType.NO_FALSE_POSITIVES
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import kotlin.math.abs
import kotlin.random.Random

/**
 * Marginalizes/sums out all dimensions not contained in [dims],
 * producing a rank-(dims.size) tensor consisting of [dims].
 */
fun NDArray<Double, DN>.sumOnto(vararg dims: Int = intArrayOf(0)) =
  (0 until dim.d).fold(this to 0) { (t, r), b ->
    if (b in dims) t to r + 1
    else mk.math.sum<Double, DN, DN>(t, r) to r
  }.first

/**
 * Each entry in [dimToIdx] defines a unique N-1 dimensional hyperplane,
 * which, when intersected, produce a rank-(N-dimToIdx.size) tensor.
 */
fun NDArray<Double, DN>.disintegrate(dimToIdx: Map<Int, Int>) =
  // TODO: Is this really disintegration or something else?
  // http://www.stat.yale.edu/~jtc5/papers/ConditioningAsDisintegration.pdf
  // https://en.wikipedia.org/wiki/Disintegration_theorem
  (0 until dim.d).fold(this to 0) { (t, r), b ->
    if (b in dimToIdx) t.view(dimToIdx[b]!!, r).asDNArray() to r
    else t to r + 1
  }.first

fun <T> Sequence<T>.toMarkovChain(memory: Int = 3) =
  MarkovChain(train = this, memory = memory)

// One-to-one hashmap of Ts to indices
class Bijection<T>(
  val list: List<T>,
  val map: Map<T, Int> = list.zip(list.indices).toMap(),
  val rmap: Map<Int, T> = map.entries.associate { (k, v) -> v to k }
) : Map<T, Int> by map {
  // 𝒪(1) surrogate for List<T>.indexOf(...)
  operator fun get(key: Int): T = rmap[key]!!
  override fun get(key: T): Int = map[key]!!
  operator fun contains(value: Int) = value in rmap
}

val maxUniques: Int = 2000

// TODO: Support continuous state spaces?
// https://www.colorado.edu/amath/sites/default/files/attached-files/2_28_2018.pdf
open class MarkovChain<T>(
  train: Sequence<T> = sequenceOf(),
  val memory: Int = 3,
  val counter: Counter<T> = Counter(train, memory)
) {
  private val mgr = ResettableLazyManager()

  private val dictionary: Bijection<T> by resettableLazy(mgr) {
    counter.rawCounts.getFrequentItems(NO_FALSE_POSITIVES)
      // Is taking maxTokens-most frequent unigrams always the right choice?
      .take(maxUniques).map { it.item }.let { Bijection(it) }
  }

  val size: Int by resettableLazy(mgr) { dictionary.size }

  /**
   * Transition tensor representing the probability of observing
   * a subsequence t₁t₂...tₙ, i.e.:
   *
   * P(T₁=t₁,T₂=t₂,…,Tₙ=tₙ) = P(tₙ|tₙ₋₁)P(tₙ₋₁|tₙ₋₂)…P(t₂|t₁)
   *
   * Where the tensor rank n=[memory], T₁...ₙ are random variables
   * and t₁...ₙ are their concrete instantiations. This tensor is
   * a hypercube with shape [size]ⁿ, indexed by [dictionary].
   */

  val tt: NDArray<Double, DN> by resettableLazy(mgr) {
    mk.dnarray<Double, DN>(IntArray(memory) { size }) { 0.0 }
      .also { mt: NDArray<Double, DN> ->
        counter.memCounts.getFrequentItems(NO_FALSE_POSITIVES)
          .map { it.item to it.estimate.toInt() }
          .filter { (item, _) -> item.all { it in dictionary } }
          .forEach { (item, count) ->
            val idx = item.map { dictionary[it] }.toIntArray()
            if (idx.size == memory) mt[idx] = count.toDouble()
          }
      }.let { it / it.sum() } // Normalize across all entries in tensor
    // TODO: May be possible to precompute fiber/slice PMFs via tensor renormalization?
    // https://mathoverflow.net/questions/393427/generalization-of-sinkhorn-s-theorem-to-stochastic-tensors
    // https://arxiv.org/pdf/1702.08142.pdf
    // TODO: Look into copulae
    // https://en.wikipedia.org/wiki/Copula_(probability_theory)
  }

  // Maps the coordinates of a transition tensor fiber to a memoized distribution
  val dists: MutableMap<List<Int>, Dist> by resettableLazy(mgr) { mutableMapOf() }

  // https://www.cs.utah.edu/~jeffp/papers/merge-summ.pdf
  operator fun plus(mc: MarkovChain<T>) = apply {
    fun <T> AtomicLongMap<T>.addAll(that: AtomicLongMap<T>) =
      that.asMap().forEach { (k, v) -> addAndGet(k, v) }
    counter.rawCounts.merge(mc.counter.rawCounts)
    counter.memCounts.merge(mc.counter.memCounts)
    mgr.reset()
  }

  fun sample(
    seed: () -> T = {
      dictionary[Dist(tt.sumOnto().toList()).sample()]
    },
    next: (T) -> T = { it: T ->
      val dist = Dist(tt.view(dictionary[it]).asDNArray().sumOnto().toList())
      dictionary[dist.sample()]
    },
    memSeed: () -> Sequence<T> = {
      generateSequence(seed, next).take(memory - 1)
    },
    memNext: (Sequence<T>) -> (Sequence<T>) = { curr ->
      val idxs = curr.map { dictionary[it] }.toList()
      val dist = dists.getOrPut(idxs) {
        // seems to work? I wonder why we don't need to use multiplication
        // to express conditional probability? Just disintegration?
        // https://blog.wtf.sg/posts/2021-03-14-smoothing-with-backprop
        // https://homes.sice.indiana.edu/ccshan/rational/disintegrator.pdf
        val slices = idxs.indices.zip(idxs).toMap()
        // Intersect conditional slices to produce a 1D count fiber
        val intersection = tt.disintegrate(slices).toList()
        // Turns 1D count fiber into a probability vector
        Dist(intersection)
      }

      curr.drop(1) + dictionary[dist.sample()]
    }
  ) = generateSequence(memSeed, memNext).map { it.last() }

  /**
   * Treats each subsequence of length [memory] as a single token
   * and counts the number of time it occurs in the sequence using
   * the Count-min sketch implemented by [ItemsSketch].
   */
  class Counter<T>(
    count: Sequence<T> = sequenceOf(),
    memory: Int,
    // Counts raw instances of T
    val rawCounts: ItemsSketch<T> = ItemsSketch(pow2(log2(maxUniques) + 5)),
    val memUniques: Int = pow2(log2(memory * maxUniques) + 2),
    // Counts unique subsequences of Ts up length memory
    val memCounts: ItemsSketch<List<T>> =
      ItemsSketch<List<T>>(memUniques).also { memMap ->
        (0 until memory)
          .flatMap { count.drop(it).chunked(memory) }
          .forEach { buffer: List<T> ->
            memMap.update(buffer)
            buffer.forEach { rawCounts.update(it) }
          }
      },
  )
}

class Dist(
  counts: Collection<Number>,
  val sum: Double = counts.sumOf { it.toDouble() },
  // https://en.wikipedia.org/wiki/Probability_mass_function
  val pmf: List<Double> = counts.map { i -> i.toDouble() / sum },
  // https://en.wikipedia.org/wiki/Cumulative_distribution_function
  val cdf: List<Double> = pmf.runningReduce { acc, d -> d + acc }
) {
  // Computes KS-transform using binary search
  fun sample(
    random: Random = Random.Default,
    target: Double = random.nextDouble()
  ): Int = cdf.binarySearch { it.compareTo(target) }
    .let { if (it < 0) abs(it) - 1 else it }
}