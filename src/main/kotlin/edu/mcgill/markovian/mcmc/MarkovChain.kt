package edu.mcgill.markovian.mcmc

import com.google.common.util.concurrent.AtomicLongMap
import java.util.stream.Stream

class MarkovChain<T> {
  operator fun get(pair: Pair<T, T>) = Entry(this, pair)

  operator fun set(pair: Pair<T, T>, value: Entry<T>) =
    alm.addAndGet(pair, value.inc).also {

    }

  data class Entry<T>(
    val mc: MarkovChain<T>,
    val pair: Pair<T, T>,
    val inc: Long = 1,
  ) {
    val cnt: Long = mc.alm[pair]
    operator fun inc() = this
  }

  private val alm = AtomicLongMap.create<Pair<T, T>>()
  val keys
    get() = alm.asMap().keys
}

fun main() {
  val data = listOf('a', 'b', 'c', 'd', 'e', 'f', 'g')
  val t = data.stream().markovChain()
  val types = data.distinct()
  val prod = types.toSet().let { it * it }
  prod.forEach { println("$it:${t[it]}") }
}

fun <T> Stream<T>.markovChain() =
  MarkovChain<T>().also { mc ->
    reduce { prev, curr ->
      mc[prev to curr]++
      curr
    }
  }

// Returns the Cartesian product of two sets
operator fun <T> Set<T>.times(s: Set<T>) =
  flatMap { l -> s.map { r -> l to r }.toSet() }.toSet()