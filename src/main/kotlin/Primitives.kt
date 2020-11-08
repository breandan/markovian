@file:Suppress("NonAsciiCharacters")

import umontreal.ssj.probdist.*
import kotlin.reflect.KProperty

// TODO: https://en.wikipedia.org/wiki/Plate_notation
// https://github.com/todesking/platebuilder
abstract class Distribution : (Double) -> Double {
    open val name: String = ""

    open operator fun getValue(nothing: Nothing?, property: KProperty<*>): Distribution = new(property.name)
    abstract val density: ContinuousDistribution

    abstract fun new(name: String): Distribution
    override fun invoke(p1: Double): Double = density.inverseF(p1)
    fun pdf(x: Double) = density.density(x)

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
        y: Double, lo: Double = -10.0, hi: Double = 10.0,
        mid: Double = lo + (hi - lo) / 2
    ): Double = when {
        hi - lo < precision -> mid
        cdf(mid) < y -> invcdf(y, mid, hi)
        else -> invcdf(y, lo, mid)
    }
}

//class Dirichlet : Distribution()
class Uniform(override val name: String = "", val lo: Double = 0.0, val hi: Double = 1.0) : Distribution() {
    override val density: ContinuousDistribution = UniformDist(lo, hi)
    override fun new(name: String): Uniform = Uniform(name, lo, hi)
}

class Beta(override val name: String = "", val a: Double = 2.0, val b: Double = 2.0) : Distribution() {
    override val density: ContinuousDistribution = BetaDist(a, b)
    override fun new(name: String): Beta = Beta(name, a, b)
}

class Gaussian(override val name: String = "", val μ: Double = 0.1, val σ: Double = 1.0) : Distribution() {
    override val density: ContinuousDistribution = NormalDist(μ, σ)
    override fun new(name: String): Gaussian = Gaussian(name, μ, σ)
}

const val precision = 0.00000001

fun main() {
    val g0 by Gaussian()
    val b0 by Beta()
    val g1 = { it: Double -> g0(it) * 1.4 + 3.0 }
    val g2 = { it: Double -> (g0(it) + g1(it)) / 2.0 }

    compare(g0, g1, g2, b0).display()
}