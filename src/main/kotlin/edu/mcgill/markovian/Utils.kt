package edu.mcgill.markovian

import kotlinx.coroutines.*
import kotlin.math.pow
import kotlin.random.Random

// Generates a rational number in (0, 1). Approximates nextDouble() as n -> \infty
tailrec fun Random.nextRational(n: Int = 100): String {
  val (j, i) = nextInt(3, n).let { it to nextInt(1, it - 1) }
  return if (1 < i.gcd(j)) nextRational(n) else "$i/$j"
}

tailrec fun Int.gcd(b: Int): Int =
  if (this == b) this
  else if (this > b) (this - b).gcd(b)
  else gcd(b - this)

// https://medium.com/@elizarov/the-reason-to-avoid-globalscope-835337445abc
fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> =
  runBlocking { map { async { f(it) } }.awaitAll() }

fun List<Double>.variance() =
  average().let { mean -> map { (it - mean).pow(2) } }.average()