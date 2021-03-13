package edu.mcgill.markovian.mcmc

import com.google.common.util.concurrent.AtomicLongMap
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.io.File
import kotlin.math.abs
import kotlin.random.Random
import kotlin.streams.asStream
import kotlin.time.*

@ExperimentalTime
fun main(args: Array<String>) {
  println("Training data: ${args[0]}")
  val mc = measureTimedValue {
    val data = File(args[0]).walkTopDown()
      .filter { it.extension == "py" }
      .joinToString { it.readText() }.asSequence()

    data.toMarkovChain(memory = 3)
  }.let {
    println("Training time: ${it.duration}")
    it.value
  }

  println("Tokens: " + mc.size)
//  measureTimedValue {
//    println("Ergodic:" + mc.isErgodic())
//  }.also { println("Ergodicity time: ${it.duration}") }

  measureTimedValue {
    mc.sample().take(200).flatten().toList()
  }.also {
    println("Sample: " + it.value.joinToString(""))
    println("Sampling time: ${it.duration}")
  }
}

fun <T> Sequence<T>.toMarkovChain(memory: Int = 1) =
  MarkovChain {
    (0 until memory)
      .flatMap { drop(it).chunked(memory) }
      .asSequence()
  }

class MarkovChain<T>(
  maxTokens: Int = 2000,
  train: () -> Sequence<T>,
) {
  val counter: Counter<T> = Counter(train)
  private val keys: List<T> =
    counter.entries.asSequence()
      // Take top K most frequent tokens
      .sortedByDescending { it.value }
      .take(maxTokens).map { it.key.first }
      .distinct().toList()

  val size: Int = keys.size
  val tm: NDArray<Double, D2> = // Transition matrix
    mk.d2array(size, size) { 0.0 }.also { mt ->
      keys.indices.toSet().let { it * it }
        .forEach { (i, j) ->
          mt[i, j] = this[i, j].toDouble()
        }
    }.let { it / it.sum() }
  val cdfs: List<CDF> = // Computes row-wise CDFs
    (0 until size).map { tm[it].toList().cdf() }

  fun sample(
    seed: () -> T = {
      keys[mk.math.sumD2(tm, 1)
        .toList().cdf().sample()]
    },
    next: (T) -> T = { it: T ->
      keys[cdfs[keys.indexOf(it)].sample()]
    }
  ): Sequence<T> = generateSequence(seed, next)

  fun isErgodic(): Boolean =
    mk.linalg.pow(tm, (size - 1) * (size - 1) + 1)
      .all { 0.0 < it }

  operator fun get(i: Int, j: Int): Long =
    counter[keys[i] to keys[j]] ?: 0

  class Counter<T>(
    count: () -> Sequence<T>,
    counts: AtomicLongMap<Pair<T, T>> =
      AtomicLongMap.create<Pair<T, T>>().also {
        count().zipWithNext().asStream().parallel()
          .forEach { (prev, next) ->
            it.incrementAndGet(prev to next)
          }
      }
  ): Map<Pair<T, T>, Long> by counts.asMap()
}

// Returns the Cartesian product of two sets
operator fun <T> Set<T>.times(s: Set<T>) =
  flatMap { ti -> s.map { ti to it }.toSet() }.toSet()

fun Collection<Number>.cdf() = CDF(
  sumOf { it.toDouble() }
    .let { sum -> map { i -> i.toDouble() / sum } }
    .runningReduce { acc, d -> d + acc }
)

class CDF(val cdf: List<Double>): List<Double> by cdf

// Computes KS-transform using binary search
fun CDF.sample(random: Random = Random.Default,
               target: Double = random.nextDouble()) =
  cdf.binarySearch { it.compareTo(target) }
    .let { if (it < 0) abs(it) - 1 else it }