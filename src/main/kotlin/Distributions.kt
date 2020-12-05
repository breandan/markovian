@file:Suppress("NonAsciiCharacters")

import umontreal.ssj.probdist.*
import kotlin.math.pow
import kotlin.reflect.KProperty

class Uniform(
  override val name: String = "",
  val lo: Double = 0.0,
  val hi: Double = 1.0,
  override val μ: Double = (hi - lo) / 2.0,
  override val σ: Double = (hi - lo).pow(2) / 12.0
) : Distribution<Uniform>() {
  override val density: ContinuousDistribution = UniformDist(lo, hi)
  override fun new(name: String): Uniform = Uniform(name, lo, hi)
}

class Beta(
  override val name: String = "",
  val α: Double = 2.0,
  val β: Double = 2.0,
  override val μ: Double = α / (α + β),
  override val σ: Double = α * β / ((α + β).pow(2) * (α + β + 1))
) : Distribution<Beta>() {
  override val density: ContinuousDistribution = BetaDist(α, β)
  override fun new(name: String): Beta = Beta(name, α, β)
}

abstract class Distribution<T: Distribution<T>> : (Double) -> Double {
  open val name: String = ""
  abstract val μ: Double
  abstract val σ: Double

  open operator fun getValue(nothing: Nothing?, property: KProperty<*>): T =
    new(property.name)
  open val density: ContinuousDistribution = UniformDist()

  abstract fun new(name: String): T
  override fun invoke(p1: Double): Double = density.inverseF(p1)
  open fun pdf(x: Double) = density.density(x)

//    abstract operator fun plus(distribution: T): T
//    abstract operator fun times(distribution: T): T

  /**
   * When observing new data, we should:
   *
   *  1. Recurse and bind on a match.
   *  2. Update our prior belief.
   *  3. Propagate uncertainty forward.
   */

  fun observe(vararg pairs: Pair<T, List<Double>>): T = TODO()

  // TODO: Combinators: average, convolution, product, sum...

  tailrec fun cdf(
    z: Double,
    sum: Double = 0.0,
    term: Double = z,
    i: Int = 3
  ): Double =
    when {
      z < -8.0 -> 0.0
      z > 8.0 -> 1.0
      sum + term == sum -> 0.5 + sum * pdf(z)
      else -> cdf(z, sum + term, term * z * z / i, i + 2)
    }

  // Binary search root-finder
  tailrec fun invcdf(
    y: Double, lo: Double = -4.0, hi: Double = 8.0,
    mid: Double = lo + (hi - lo) / 2
  ): Double = when {
    hi - lo < precision -> mid
    cdf(mid) < y -> invcdf(y, mid, hi)
    else -> invcdf(y, lo, mid)
  }

  override fun equals(other: Any?) =
    other is Distribution<*> &&
      javaClass == other.javaClass &&
      μ == other.μ && σ == other.σ

  override fun toString() = "${javaClass.simpleName}($μ, $σ)"
}