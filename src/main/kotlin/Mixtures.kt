@file:Suppress("NonAsciiCharacters")

import umontreal.ssj.probdist.*
import kotlin.math.*

// TODO: https://en.wikipedia.org/wiki/Plate_notation
// cf. https://github.com/todesking/platebuilder

// https://en.wikipedia.org/wiki/Propagation_of_uncertainty#Example_formulae
// https://en.wikipedia.org/wiki/Exponential_family#Table_of_distributions
// https://en.wikipedia.org/wiki/Cumulant
// http://indico.ictp.it/event/a0143/contribution/2/material/0/0.pdf

class Gaussian(
  override val name: String = "",
  override val μ: Double = 0.1,
  override val σ: Double = 1.0
): GaussianMixture(name, listOf()) {
  override val density: ContinuousDistribution = NormalDist(μ, σ)
  override fun new(name: String): Gaussian = Gaussian(name, μ, σ)
  override val components by lazy { listOf(this) }

  // TODO: can we get the graph compiler to infer this?
  // https://math.stackexchange.com/questions/1112866/product-of-two-gaussian-pdfs-is-a-gaussian-pdf-but-product-of-two-gaussian-vari
  override operator fun times(that: Gaussian) =
    combine(this, that) { μ1, μ2, σ1, σ2 ->
      Gaussian(
        "$name * ${that.name}",
        μ = (μ1 * σ2 * σ2 + μ2 * σ1 * σ1) / (σ1 * σ1 + σ2 * σ2),
        σ = (σ1 * σ2).pow(2) / (σ1 * σ1 + σ2 * σ2)
      )
    }

  companion object {
    fun combine(
      g1: Gaussian, g2: Gaussian,
      f: (Double, Double, Double, Double) -> Gaussian
    ) = f(g1.μ, g2.μ, g1.σ, g2.σ)
  }

  operator fun times(c: Double) = Gaussian(μ = c * μ, σ = c * c * σ)
  operator fun plus(c: Double) = Gaussian(μ = c + μ, σ = σ)
}

// https://arxiv.org/pdf/1901.06708.pdf

open class GaussianMixture(
  override val name: String = "",
  open val components: List<GaussianMixture>
): Distribution<GaussianMixture>() {
  // Uniform by default
  val weights: List<Double> by lazy {
    List(components.size) { 1.0 / components.size.toDouble() }
  }

  // https://stats.stackexchange.com/a/16609
  override val μ: Double by lazy { partition { it.μ } }
  override val σ: Double by lazy { partition { it.σ * it.σ + it.μ * it.μ } + μ * μ }

  override val density: ContinuousDistribution =
    object: ContinuousDistribution() {
      override fun cdf(x: Double) = partition { it.density.cdf(x) }
      override fun getParams() = TODO("Not yet implemented")
      override fun density(x: Double) = partition { it.density.density(x) }
    }

  operator fun plus(that: Gaussian) =
    GaussianMixture(components = components + listOf(that))

  open operator fun times(that: Gaussian): GaussianMixture =
    GaussianMixture(components = components.map { it * that })

  open operator fun times(that: GaussianMixture): GaussianMixture =
    GaussianMixture(components = components.map { it * that })

  fun partition(selector: (GaussianMixture) -> Double) =
    components.map { selector(it) }.zip(weights).sumByDouble { (a, b) -> a * b }

  override fun new(name: String) = GaussianMixture(name, components)
}

const val precision = 0.00000001

fun main() {
  val g0 = Gaussian("", 0.1, 1.0)
  val g1 = Gaussian("", 5.0, 1.0)
  val g2 = Gaussian("", 10.0, 1.0)
//    val g2 = g0 * g1
  val g3 = g0 + g1 + g2

  val g4 = Gaussian("", 5.0, 2.0)

  val g5 = g3 * g4

  // TODO: test distributivity holds
  compare(g0, g1, g2, g3, g4, g5).display()
}