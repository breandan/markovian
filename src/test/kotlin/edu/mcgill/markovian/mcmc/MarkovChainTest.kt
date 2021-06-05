package edu.mcgill.markovian.mcmc

import org.junit.Test
import kotlin.random.Random

class MarkovChainTest {
  @Test
  fun testCategorical() {
    List(100) { ('a'..'z').random() }.asSequence()
      .toMarkovChain().sample().take(100)
      .let { println(it.joinToString("")) }
  }

  @Test
  fun testNumerical() {
    // TODO: why does it converge?
    List(100) { Random.nextDouble() }.asSequence()
      .toMarkovChain().sample().take(100)
      .let { println(it.joinToString(",")) }
  }
}