package edu.mcgill.markovian.mcmc

import com.google.common.util.concurrent.AtomicLongMap
import edu.mcgill.markovian.concurrency.*
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.*

fun NDArray<Double, DN>.sumOnto(vararg dims: Int = intArrayOf(0)) =
  (0 until dim.d).fold(this to 0) { (t, r), b ->
    if (b in dims) t to r + 1
    else mk.math.sum<Double, DN, DN>(t, r) to r
  }.first

fun NDArray<Double, DN>.disintegrate(dimToIdx: Map<Int, Int>) =
  (0 until dim.d).fold(this to 0) { (t, r), b ->
    if (b in dimToIdx) t.view(dimToIdx[b]!!, r).asDNArray() to r
    else t to r + 1
  }.first

@ExperimentalTime
fun main() {
  val a = mk.ndarray(
    mk[
      mk[mk[1.0, 2.0, 3.0], mk[4.0, 5.0, 6.0], mk[1.0, 1.0, 1.0]],
      mk[mk[7.0, 8.0, 9.0], mk[10.0, 11.0, 12.0], mk[2.0, 2.0, 2.0]],
      mk[mk[13.0, 14.0, 15.0], mk[16.0, 17.0, 18.0], mk[3.0, 3.0, 3.0]]
    ]
  )
  println(a.asDNArray().disintegrate(mapOf(1 to 1, 2 to 2)))
  println(a.asDNArray().slice(mapOf(1 to Slice(1, 2, 1), 2 to Slice(2, 3, 1))))
}

fun <T> Sequence<T>.toMarkovChain(memory: Int = 3) =
  MarkovChain(train = this, memory = memory)

// One-to-one hashmap of Ts to indices
class Bijection<T>(
  val list: List<T>,
  val map: Map<T, Int> = list.zip(list.indices).toMap(),
  val revMap: Map<Int, T> = map.entries.associate { (k, v) -> v to k }
): Map<T, Int> by map {
  // 𝒪(1) surrogate for List<T>.indexOf(...)
  operator fun get(key: Int): T = revMap[key]!!
  override fun get(key: T): Int = map[key]!!
  operator fun contains(value: Int) = value in revMap
}

open class MarkovChain<T>(
  val maxTokens: Int = 2000,
  train: Sequence<T> = sequenceOf(),
  val memory: Int = 3,
  val counter: Counter<T> = Counter(train, memory)
) {
  private val mgr = ResettableLazyManager()

  private val dictionary: Bijection<T> by resettableLazy(mgr) {
    counter.rawCounts.asMap().entries.asSequence()
      // Take top K most frequent tokens
      .sortedByDescending { it.value }
      .take(maxTokens).map { it.key }
      .distinct().toList().let { Bijection(it) }
  }

  val size: Int by resettableLazy(mgr) { dictionary.size }

  /**
   * Transition tensor representing the probability of observing
   * a subsequence t₁t₂...tₙ, i.e.:
   *
   * P(T₁=t₁,T₂=t₂,…,Tₙ=tₙ) = P(tₙ|tₙ₋₁)P(tₙ₋₁|tₙ₋₂)…P(t₂|t₁)
   *
   * Where n is the tensor rank, T₁...ₙ are random variables and
   * t₁...ₙ are their concrete instantiations. This tensor is a
   * hypercube with shape [size]ⁿ, indexed by [dictionary].
   */

  val tt: NDArray<Double, DN> by resettableLazy(mgr) {
    mk.dnarray<Double, DN>(IntArray(memory) { size }) { 0.0 }
      .also { mt: NDArray<Double, DN> ->
        counter.memCounts.asMap().entries
          .filter { it.key.all { it in dictionary } }
          .forEach { (k, v) ->
            val idx = k.map { dictionary[it] }.toIntArray()
            if (idx.size == memory) mt[idx] = v.toDouble()
          }
      }.let { it / it.sum() }
  }

  // Computes row-wise CDFs
  val cdfs: MutableMap<List<Int>, CDF> by resettableLazy(mgr) { mutableMapOf() }
  // TODO: Preconditioning? https://mathoverflow.net/questions/393427/generalization-of-sinkhorn-s-theorem-to-stochastic-tensors

  fun <T> AtomicLongMap<T>.addAll(that: AtomicLongMap<T>) =
    that.asMap().forEach { (k, v) -> addAndGet(k, v) }

  operator fun plus(mc: MarkovChain<T>) = apply {
    counter.rawCounts.addAll(mc.counter.rawCounts)
    counter.memCounts.addAll(mc.counter.memCounts)
    mgr.reset()
  }

  fun sample(
    seed: () -> T = {
      dictionary[tt.sumOnto().toList().cdf().sample()]
    },
    next: (T) -> T = { it: T ->
      val cdf = cdfs.getOrPut(listOf(dictionary[it])) {
        tt.view(dictionary[it]).asDNArray().sumOnto().toList().cdf()
      }
      dictionary[cdf.sample()]
    },
    memSeed: () -> Sequence<T> = {
      generateSequence(seed, next).take(memory - 1)
    },
    memNext: (Sequence<T>) -> (Sequence<T>) = {
      val idxs = it.map { dictionary[it] }.toList()
      val cdf = cdfs.getOrPut(idxs) {
        // seems to work? I wonder why we don't need
        // to use multiplication to express conditional
        // probability? Just disintegration?
        // https://blog.wtf.sg/posts/2021-03-14-smoothing-with-backprop/
        // https://homes.sice.indiana.edu/ccshan/rational/disintegrator.pdf
        val slices = idxs.map { Slice(it, it + 1, 1) }
        val volume = slices.indices.zip(slices).toMap()
        tt.slice(volume).toList().cdf()
      }

      it.drop(1) + dictionary[cdf.sample()]
    }
  ) = generateSequence(memSeed, memNext).map { it.last() }

//  fun isErgodic(): Boolean =
//    mk.linalg.pow(tt, (size - 1) * (size - 1) + 1)
//      .all { 0.0 < it }

  operator fun get(i: IntArray) = counter[i.map { dictionary[it] }] ?: 0

  /**
   * Treats each subsequence of length [memory] as a single
   * token and counts the number of time it occurs in the sequence.
   */
  class Counter<T>(
    count: Sequence<T> = sequenceOf(),
    memory: Int,
    val rawCounts: AtomicLongMap<T> = AtomicLongMap.create(),
    val memCounts: AtomicLongMap<List<T>> =
      AtomicLongMap.create<List<T>>().also { memMap ->
        (0 until memory)
          .flatMap { count.drop(it).chunked(memory) }
          .forEach { buffer: List<T> ->
            memMap.incrementAndGet(buffer)
            buffer.forEach { rawCounts.incrementAndGet(it) }
          }
      }
  ): Map<List<T>, Long> by memCounts.asMap()
}

fun Collection<Number>.cdf() = CDF(
  sumOf { it.toDouble() }
    .let { sum -> map { i -> i.toDouble() / sum } }
    .runningReduce { acc, d -> d + acc }
)

class CDF(val cdf: List<Double>): List<Double> by cdf

// Computes KS-transform using binary search
fun CDF.sample(
  random: Random = Random.Default,
  target: Double = random.nextDouble()
): Int = cdf.binarySearch { it.compareTo(target) }
  .let { if (it < 0) abs(it) - 1 else it }