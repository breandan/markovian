package edu.mcgill.markovian.mcmc

import com.google.common.util.concurrent.AtomicLongMap
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import java.io.File
import kotlin.math.abs
import kotlin.random.Random
import kotlin.streams.asStream
import kotlin.system.measureTimeMillis

fun main(args: Array<String>) {
  println("Training data: ${args[0]}")
  val data = File(args[0]).walkTopDown()
    .filter { it.extension == "py" }
    .joinToString { it.readText() }.asSequence()

  val mc = data.toMarkovChain(memory = 3)
  println("Tokens: " + mc.size)
  measureTimeMillis {
    val sample = mc.sample().take(200).flatten()
    println("Sample: " + sample.joinToString(""))
  }.also { println("Sampling time: $it ms") }
}

fun <T> Sequence<T>.toMarkovChain(memory: Int = 1) =
  MarkovChain<List<T>>().also { mc ->
    (0 until memory)
      .flatMap { drop(it).chunked(memory) }
      .asSequence().asStream().parallel()
      .reduce { prev, curr ->
        mc.observe(prev to curr)
        curr
      }
  }

class MarkovChain<T>(val maxTokens: Int = 2000) {
  private val keys by lazy {
    counts.asMap().entries.asSequence()
      .sortedByDescending { it.value }
      .take(maxTokens).map { it.key }
      .map { listOf(it.first, it.second) }
      .flatten().distinct().toList()
  }
  val size by lazy { keys.size }
  val counts = AtomicLongMap.create<Pair<T, T>>()

  fun sample() =
    transitionMatrix().let { it to it.cdfs() }
      .let { (tm, cdfs) ->
        generateSequence(
          seedFunction = {
            this[mk.math.sumD2(tm, 1)
              .toList().cdf().sample()]
          },
          nextFunction = {
            this[cdfs[keys.indexOf(it)].sample()]
          }
        )
      }

  fun isErgodic() = transitionMatrix().let { it ->
    mk.linalg.pow(it + mk.identity(size), size)
      .all { 0.0 < it }
  }

  private operator fun get(index: Int) = keys[index]

  fun transitionMatrix() =
    mk.d2array(size, size) { 0.0 }.also { mt ->
      keys.indices.toSet().let { it * it }
        .forEach { (i, j) ->
          mt[i, j] = this[i, j].toDouble()
        }
    }.let { it / it.sum() }

  fun observe(pair: Pair<T, T>) =
    counts.incrementAndGet(pair)

  operator fun get(i: Int, j: Int) =
    counts[keys[i] to keys[j]]
}

// Returns the Cartesian product of two sets
operator fun <T> Set<T>.times(s: Set<T>) =
  flatMap { ti -> s.map { ti to it }.toSet() }.toSet()

fun Ndarray<Double, D2>.cdfs() =
  (0 until shape[0]).map { this[it].toList().cdf() }

fun List<Number>.cdf(): CDF = CDF(
  map { it.toDouble() }.sum()
    .let { sum -> map { i -> i.toDouble() / sum } }
    .runningReduce { acc, d -> d + acc }
)

class CDF(val cdf: List<Double>): List<Double> by cdf

fun CDF.sample(rand: Double = Random.nextDouble()) =
  cdf.binarySearch { it.compareTo(rand) }
    .let { if (it < 0) abs(it) - 1 else it }