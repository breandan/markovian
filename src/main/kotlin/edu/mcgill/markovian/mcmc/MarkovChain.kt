package edu.mcgill.markovian.mcmc

import com.google.common.util.concurrent.AtomicLongMap
import edu.mcgill.markovian.concurrency.*
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.io.File
import kotlin.math.abs
import kotlin.random.Random
import kotlin.time.*

@ExperimentalTime
fun main(args: Array<String>) {
  val a = mk.ndarray(mk[mk[mk[1, 2, 3], mk[4, 5, 6]], mk[mk[7, 8, 9], mk[10, 11, 12]]])
  println(mk.math.cumSum(a, 1))
//  println("Training data: ${args[0]}")
//  val mc = measureTimedValue {
//    val data = File(args[0]).walkTopDown()
//      .filter { it.extension == "py" }
//      .joinToString { it.readText() }.asSequence()
//
//    data.toMarkovChain()
//  }.let {
//    println("Training time: ${it.duration}")
//    it.value
//  }
//
//  println("Tokens: " + mc.size)
////  measureTimedValue {
////    println("Ergodic:" + mc.isErgodic())
////  }.also { println("Ergodicity time: ${it.duration}") }
//
//  measureTimedValue {
//    mc.sample().take(200).toList()
//  }.also {
//    println("Sample: " + it.value.joinToString(""))
//    println("Sampling time: ${it.duration}")
//  }
}

fun <T> Sequence<T>.toMarkovChain() = MarkovChain(train = this)

open class MarkovChain<T>(
  maxTokens: Int = 2000,
  train: Sequence<T> = sequenceOf(),
  val memory: Int = 3,
  val counter: Counter<T> = Counter(train, memory)
) {
  private val mgr = resettableManager()

  private val keys: List<T> by resettableLazy(mgr) {
    counter.rawCounts.asMap().entries.asSequence()
      // Take top K most frequent tokens
      .sortedByDescending { it.value }
      .take(maxTokens).map { it.key }
      .distinct().toList()
  }

  val size: Int by resettableLazy(mgr) { keys.size }

  // Transition tensor
  val tt: NDArray<Double, DN> by resettableLazy(mgr) {
    mk.dnarray<Double, DN>(IntArray(memory) { size }) { 0.0 }
      .also { mt ->
        keys.indices.toSet().let {
          val idx = setOf(keys.indices.toList())
          (1..memory).fold(idx) { a, _ -> a * idx }
        }.forEach { i ->
          mt[i.toIntArray()] = this[i].toDouble()
        }
      }.let { it / it.sum() }
  }

  // Computes row-wise CDFs
  val cdfs: MutableMap<List<Int>, CDF> by resettableLazy(mgr) {
    mutableMapOf()
  }

  fun <T> AtomicLongMap<T>.addAll(that: AtomicLongMap<T>) =
    that.asMap().forEach { (k, v) -> addAndGet(k, v) }

  operator fun plus(mc: MarkovChain<T>) = apply {
    counter.rawCounts.addAll(mc.counter.rawCounts)
    counter.memCounts.addAll(mc.counter.memCounts)
    mgr.reset()
  }

  fun sample(
    seed: () -> T = {
      keys[mk.math.cumSum(tt).toList().cdf().sample()]
    },
    next: (T) -> T = { it: T ->
      val cdf = cdfs.getOrPut(listOf(keys.indexOf(it))) {
        mk.math.cumSum(tt, keys.indexOf(it)).toList().cdf()
      }
      keys[cdf.sample()]
    }
  ): Sequence<T> = generateSequence(seed, next)

//  fun isErgodic(): Boolean =
//    mk.linalg.pow(tt, (size - 1) * (size - 1) + 1)
//      .all { 0.0 < it }

  operator fun get(i: List<Int>): Long =
    counter[i.map { keys[it] }] ?: 0

  class Counter<T>(
    count: Sequence<T> = sequenceOf(),
    memory: Int,
    val rawCounts: AtomicLongMap<T> = AtomicLongMap.create(),
    val memCounts: AtomicLongMap<List<T>> =
      AtomicLongMap.create<List<T>>().also {
        (0 until memory)
          .flatMap { count.drop(it).chunked(memory) }
          .forEach { buffer ->
            it.incrementAndGet(buffer)
            buffer.forEach { rawCounts.incrementAndGet(it) }
          }
      }
  ): Map<List<T>, Long> by memCounts.asMap()
}

// Returns the Cartesian product of two sets
operator fun <T> Set<List<T>>.times(s: Set<List<T>>) =
  flatMap { ti -> s.map { listOf(ti, it).flatten() }.toSet() }.toSet()

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