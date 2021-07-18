package edu.mcgill.markovian.mcmc

import org.jetbrains.kotlinx.multik.api.*
import org.jetbrains.kotlinx.multik.ndarray.data.*
import org.jetbrains.kotlinx.multik.ndarray.operations.toList
import org.junit.*
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

  @Test
  fun testDisintegrationMakesSense() {
    val a = mk.ndarray(
      mk[
        mk[mk[1.0, 2.0, 3.0], mk[4.0, 5.0, 6.0], mk[1.0, 1.0, 1.0]],
        mk[mk[7.0, 8.0, 9.0], mk[10.0, 11.0, 12.0], mk[2.0, 2.0, 2.0]],
        mk[mk[13.0, 14.0, 15.0], mk[16.0, 17.0, 18.0], mk[3.0, 3.0, 3.0]]
      ]
    )

    Assert.assertEquals(
      a.asDNArray().disintegrate(
        mapOf(
        //D    I
          1 to 1,
          2 to 2
        )
      ),
      a.asDNArray().slice(
        mapOf(
        //D                  I         I+1
          1 to Slice(start = 1, stop = 2, step = 1),
          2 to Slice(start = 2, stop = 3, step = 1)
        )
      ).toList()
    )
  }
}