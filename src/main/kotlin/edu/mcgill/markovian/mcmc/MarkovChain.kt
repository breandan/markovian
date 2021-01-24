package edu.mcgill.markovian.mcmc

import com.google.common.util.concurrent.AtomicLongMap
import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.*
import kotlin.random.Random

fun main() {
  val data =
    "lorem ipsum dolor sit amet, consectetur adipiscing elit. aliquam tempus nisi eu nisl gravida, in pretium tellus cursus. duis facilisis malesuada ligula et interdum. donec ac libero et dui tempus bibendum. donec porttitor mollis accumsan. sed urna turpis, consectetur sit amet gravida vitae, pellentesque in libero. quisque erat lorem, tincidunt eu vestibulum eu, dapibus nec felis. ut eu purus tortor. nulla eros leo, porttitor vel elit eget, tempor blandit metus. proin congue lobortis pretium. nulla eget pellentesque risus. nam ultrices quis tellus ut tincidunt. morbi vestibulum ipsum eu elementum scelerisque. fusce aliquam lobortis urna vel rhoncus. duis ut rhoncus purus, id auctor odio. donec lobortis ac enim in placerat. donec placerat nec lectus a bibendum.".asSequence()
  val mc = data.toMarkovChain()
  val sample = mc.sample().take(100).toList()
  println("Sample: " + sample.joinToString(""))
  println("Ergodic: " + mc.isErgodic())
}

fun <T> Sequence<T>.toMarkovChain() =
  MarkovChain<T>().also { mc ->
    reduce { prev, curr ->
      mc.observe(prev to curr)
      curr
    }
  }

class MarkovChain<T> {
  val keys
    get() = counts.asMap().keys
      .flatMap { listOf(it.first, it.second) }
  val size get() = keys.size
  val counts = AtomicLongMap.create<Pair<T, T>>()

  fun sample() =
    transitionMatrix().let { tm ->
      generateSequence(
        seedFunction = {
          this[mk.math.sumD2(tm, 1).sample()]
        },
        nextFunction = {
          this[tm[keys.indexOf(it)]
            .asDNArray().sample()]
        })
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
  flatMap { l -> s.map { r -> l to r }.toSet() }.toSet()

fun Ndarray<Double, *>.sample() =
  asD1Array().toList().sample()

fun List<Number>.sample(): Int =
  map { it.toDouble() }.sum()
    .let { sum -> map { i -> i.toDouble() / sum } }
    .runningReduce { acc, d -> d + acc }
    .let { cdf ->
      val rand = Random.nextDouble()
      cdf.indices.minByOrNull { i ->
        (cdf[i] - rand).let { if(it < 0) 2.0 else it }
      }!!
    }