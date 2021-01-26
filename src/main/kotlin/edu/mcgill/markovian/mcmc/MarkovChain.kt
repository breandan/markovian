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
  MarkovChain((0 until memory)
    .flatMap { s -> drop(s).chunked(memory) }
    .asSequence())

class MarkovChain<T>(
  sequence: Sequence<T>,
  val maxTokens: Int = 2000
) {
  val counts = AtomicLongMap.create<Pair<T, T>>()
  init {
    sequence.zipWithNext().asStream().parallel()
      .forEach { (tp, tn) ->
        counts.incrementAndGet(tp to tn)
      }
  }

  private val keys by lazy {
    counts.asMap().entries.asSequence()
      .sortedByDescending { it.value }
      .take(maxTokens).map { it.key }
      .map { listOf(it.first, it.second) }
      .flatten().distinct().toList()
  }
  val size by lazy { keys.size }
  val tm by lazy { //Transition matrix
    mk.d2array(size, size) { 0.0 }.also { mt ->
      keys.indices.toSet().let { it * it }
        .forEach { (i, j) ->
          mt[i, j] = this[i, j].toDouble()
        }
    }.let { it / it.sum() }
  }
  val cdfs by lazy { // Computes row-wise CDFs
    (0 until size).map { tm[it].toList().cdf() }
  }

  fun sample() = generateSequence(
    seedFunction = {
      this[mk.math.sumD2(tm, 1)
        .toList().cdf().sample()]
    },
    nextFunction = {
      this[cdfs[keys.indexOf(it)].sample()]
    }
  )

  fun isErgodic() =
    mk.linalg.pow(tm + mk.identity(size), size)
      .all { 0.0 < it }

  private operator fun get(index: Int) = keys[index]

  operator fun get(i: Int, j: Int) =
    counts[keys[i] to keys[j]]
}

// Returns the Cartesian product of two sets
operator fun <T> Set<T>.times(s: Set<T>) =
  flatMap { ti -> s.map { ti to it }.toSet() }.toSet()

fun List<Number>.cdf(): CDF = CDF(
  map { it.toDouble() }.sum()
    .let { sum -> map { i -> i.toDouble() / sum } }
    .runningReduce { acc, d -> d + acc }
)

class CDF(val cdf: List<Double>): List<Double> by cdf

// Computes KS-transform using binary search
fun CDF.sample(rand: Double = Random.nextDouble()) =
  cdf.binarySearch { it.compareTo(rand) }
    .let { if (it < 0) abs(it) - 1 else it }