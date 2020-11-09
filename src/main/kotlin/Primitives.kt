@file:Suppress("NonAsciiCharacters")

import umontreal.ssj.probdist.*
import kotlin.math.pow
import kotlin.reflect.KProperty

// TODO: https://en.wikipedia.org/wiki/Plate_notation
// cf. https://github.com/todesking/platebuilder

abstract class Distribution : (Double) -> Double {
    open val name: String = ""
    abstract val μ: Double
    abstract val σ: Double

    open operator fun getValue(nothing: Nothing?, property: KProperty<*>): Distribution = new(property.name)
    abstract val density: ContinuousDistribution

    abstract fun new(name: String): Distribution
    override fun invoke(p1: Double): Double = density.inverseF(p1)
    fun pdf(x: Double) = density.density(x)

    /**
     * When observing new data, we should:
     *
     *  1. Recurse and bind on a match.
     *  2. Update our prior belief.
     *  3. Propagate uncertainty forward.
     */

    fun observe(vararg pairs: Pair<Distribution, List<Double>>): Distribution =
        TODO()

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
        y: Double, lo: Double = -10.0, hi: Double = 10.0,
        mid: Double = lo + (hi - lo) / 2
    ): Double = when {
        hi - lo < precision -> mid
        cdf(mid) < y -> invcdf(y, mid, hi)
        else -> invcdf(y, lo, mid)
    }
}

//class Dirichlet : Distribution()
class Uniform(
    override val name: String = "",
    val lo: Double = 0.0,
    val hi: Double = 1.0,
    override val μ: Double = (hi - lo) / 2.0,
    override val σ: Double = (hi - lo).pow(2) / 12.0
) : Distribution() {
    override val density: ContinuousDistribution = UniformDist(lo, hi)
    override fun new(name: String): Uniform = Uniform(name, lo, hi)
}

class Beta(
    override val name: String = "",
    val α: Double = 2.0,
    val β: Double = 2.0,
    override val μ: Double = α / (α + β),
    override val σ: Double = α * β / ((α + β).pow(2) * (α + β + 1))
) : Distribution() {
    override val density: ContinuousDistribution = BetaDist(α, β)
    override fun new(name: String): Beta = Beta(name, α, β)
}

class Gaussian(
    override val name: String = "",
    override val μ: Double = 0.1,
    override val σ: Double = 1.0
) : Distribution() {
    override val density: ContinuousDistribution = NormalDist(μ, σ)
    override fun new(name: String): Gaussian = Gaussian(name, μ, σ)

    // TODO: can we get the graph compiler to infer this?
    infix operator fun times(that: Gaussian) = combine(this, that) { μ1, μ2, σ1, σ2 ->
        Gaussian(
            "$name * ${that.name}",
            μ = (μ1 * σ2 * σ2 + μ2 * σ1 * σ1) / (σ1 * σ1 + σ2 * σ2),
            σ = (σ1 * σ2).pow(2) / (σ1 * σ1 + σ2 * σ2)
        )
    }

    infix operator fun plus(that: Gaussian) = combine(this, that) { μ1, μ2, σ1, σ2 ->
        Gaussian(
            "($name + ${that.name})",
            μ = μ1 + μ2,
            σ = σ1 * σ1 + σ2 * σ2
        )
    }
}

fun combine(g1: Gaussian, g2: Gaussian, f: (Double, Double, Double, Double) -> Gaussian) =
    f(g1.μ, g2.μ, g1.σ, g2.σ)

const val precision = 0.00000001

fun main() {
    val g0 = Gaussian()
    val g1 = Gaussian("", 3.0, 1.4)
    val g2 = g0 * g1
    val g3 = g0 + g1

    compare(g0, g1, g2, g3).display()
}