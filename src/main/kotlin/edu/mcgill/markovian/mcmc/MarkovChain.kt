package edu.mcgill.markovian.mcmc

import com.google.common.util.concurrent.AtomicLongMap
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import kotlin.math.abs
import kotlin.random.Random
import kotlin.streams.asStream
import kotlin.system.measureTimeMillis

fun main() {
  val data =
    "lorem ipsum dolor sit amet, consectetur adipiscing elit. aliquam tempus nisi eu nisl gravida, in pretium tellus cursus. duis facilisis malesuada ligula et interdum. donec ac libero et dui tempus bibendum. donec porttitor mollis accumsan. sed urna turpis, consectetur sit amet gravida vitae, pellentesque in libero. quisque erat lorem, tincidunt eu vestibulum eu, dapibus nec felis. ut eu purus tortor. nulla eros leo, porttitor vel elit eget, tempor blandit metus. proin congue lobortis pretium. nulla eget pellentesque risus. nam ultrices quis tellus ut tincidunt. morbi vestibulum ipsum eu elementum scelerisque. fusce aliquam lobortis urna vel rhoncus. duis ut rhoncus purus, id auctor odio. donec lobortis ac enim in placerat. donec placerat nec lectus a bibendum.".asSequence()

  val mc = data.toMarkovChain(memory = 2)
  println("Ergodic: " + mc.isErgodic())
  println("Tokens: " + mc.size)
  measureTimeMillis {
    val sample = mc.sample().take(100).flatten()
    println("Sample: " + sample.joinToString(""))
  }.also { println("Sampling time: $it ms") }
}

fun <T> Sequence<T>.toMarkovChain(memory: Int = 1) =
  MarkovChain<List<T>>().also { mc ->
    chunked(memory).asStream().parallel()
      .reduce { prev, curr ->
        mc.observe(prev to curr)
        curr
      }
  }

class MarkovChain<T>(
  val counts: AtomicLongMap<Pair<T, T>> = AtomicLongMap.create()
) {
  val keys
    get() = counts.asMap().keys
      .map { listOf(it.first, it.second) }
      .flatten().distinct()
  val size get() = keys.size

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

  fun isErgodic() =
    transitionMatrix().let { it ->
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

  operator fun get(pair: Pair<T, T>) = counts.get(pair)
  operator fun get(i: Int, j: Int) =
    keys.toList().let { this[it[i] to it[j]] }
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