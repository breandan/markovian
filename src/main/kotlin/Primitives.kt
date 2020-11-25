@file:Suppress("NonAsciiCharacters")

import umontreal.ssj.probdist.*
import umontreal.ssj.probdistmulti.DirichletDist
import umontreal.ssj.randvarmulti.DirichletGen
import kotlin.math.*
import kotlin.reflect.KProperty

// TODO: https://en.wikipedia.org/wiki/Plate_notation
// cf. https://github.com/todesking/platebuilder

// https://en.wikipedia.org/wiki/Propagation_of_uncertainty#Example_formulae
// https://en.wikipedia.org/wiki/Exponential_family#Table_of_distributions
// https://en.wikipedia.org/wiki/Cumulant
// http://indico.ictp.it/event/a0143/contribution/2/material/0/0.pdf

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

    fun observe(vararg pairs: Pair<T, List<Double>>): T =
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

//class Dirichlet : Distribution()
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

class Gaussian(
    override val name: String = "",
    override val μ: Double = 0.1,
    override val σ: Double = 1.0
) : Distribution<Gaussian>() {
    override val density: ContinuousDistribution = NormalDist(μ, σ)
    override fun new(name: String): Gaussian = Gaussian(name, μ, σ)

    // TODO: can we get the graph compiler to infer this?
    operator fun times(that: Gaussian) =
        combine(this, that) { μ1, μ2, σ1, σ2 ->
            Gaussian(
                "$name * ${that.name}",
                μ = (μ1 * σ2 * σ2 + μ2 * σ1 * σ1) / (σ1 * σ1 + σ2 * σ2),
                σ = (σ1 * σ2).pow(2) / (σ1 * σ1 + σ2 * σ2)
            )
        }

    operator fun times(c: Double) = Gaussian(μ = c * μ, σ = c * c * σ)

    operator fun plus(c: Double) = Gaussian(μ = c + μ, σ = σ)

    operator fun plus(that: Gaussian) = GaussianMixture(this, that)
//        combine(this, that) { μ1, μ2, σ1, σ2 ->
//        Gaussian(
//            "($name + ${that.name})",
//            μ = μ1 + μ2,
//            σ = σ1 * σ1 + σ2 * σ2
//        )
//    }
}

// https://arxiv.org/pdf/1901.06708.pdf

open class GaussianMixture(
    val inputs: List<GaussianMixture>,
): Distribution<GaussianMixture>() {
    companion object {
        fun unaryMixture(input: Gaussian) = object: GaussianMixture() {
            override val μ: Double = input.μ
            override val σ: Double = input.σ
            override val density: ContinuousDistribution = input.density
        }

        fun asGaussian(input: GaussianMixture) =
            Gaussian("", input.μ, input.σ).also { println(it) }
    }

    constructor(vararg inputs: Gaussian):
      this(inputs = inputs.map { unaryMixture(it)})

    // Uniform by default
    val weights: List<Double> =
      List(inputs.size) { 1.0 / inputs.size.toDouble() }

    // https://stats.stackexchange.com/a/16609
    override val μ: Double = partition { it.μ }
    override val σ: Double =
        partition { it.σ * it.σ } +
          partition { it.μ * it.μ } -
          partition { it.μ }.pow(2)

    override val density: ContinuousDistribution =
        object: ContinuousDistribution() {
            override fun cdf(x: Double) = partition { it.density.cdf(x) }
            override fun getParams() = TODO("Not yet implemented")
            override fun density(x: Double) = TODO("Not yet implemented")
        }

    operator fun plus(that: Gaussian) =
        GaussianMixture(inputs + listOf(unaryMixture(that)))

    operator fun times(that: Gaussian): GaussianMixture =
        if (inputs.isEmpty()) unaryMixture(asGaussian(this) * that)
        else GaussianMixture(inputs.map { it * that })

    fun partition(selector: (GaussianMixture) -> Double) =
        inputs.map { selector(it) }.zip(weights)
            .map { (a, b) -> a * b }.sum()

    override fun new(name: String) = TODO("Not yet implemented")
}

fun combine(g1: Gaussian, g2: Gaussian,
            f: (Double, Double, Double, Double) -> Gaussian) =
    f(g1.μ, g2.μ, g1.σ, g2.σ)

const val precision = 0.00000001

fun main() {
    val g0 = Gaussian("", 0.1, 1.0)
    val g1 = Gaussian("", 5.0, 1.0)
    val g2 = Gaussian("", 10.0, 1.0)
//    val g2 = g0 * g1
    val g3 = g0 + g1 + g2

    val g4 = Gaussian("", 5.0, 2.0)

    val g5 = g3 * g4

    compare(g0, g1, g2, g3, g4, g5).display()
}