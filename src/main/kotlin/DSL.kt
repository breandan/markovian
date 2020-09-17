import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.plot.PlotSvgExport
import jetbrains.letsPlot.geom.geom_area
import jetbrains.letsPlot.geom.geom_density
import jetbrains.letsPlot.geom.geom_path
import jetbrains.letsPlot.ggplot
import jetbrains.letsPlot.ggsize
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.intern.toSpec
import jetbrains.letsPlot.label.ggtitle
import jetbrains.letsPlot.lets_plot
import org.hipparchus.special.Gamma
import org.matheclipse.core.eval.ExprEvaluator
import org.matheclipse.core.interfaces.IExpr
import java.io.File
import java.util.*
import kotlin.math.*
import kotlin.system.measureTimeMillis
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

val util = ExprEvaluator(false, 100)

val rand = kotlin.random.Random(3)
@ExperimentalTime
fun main() {
    // TODO: Use regularization to prevent exponents from exploding?
    val a = randomKumaraswamy()
    val b = randomKumaraswamy()
    val c = randomKumaraswamy()
    val d = randomKumaraswamy()

//    val mixture = util.eval("$a + $b + $c + $d")
//    val mixture = util.eval("(($a+$b) * ($c+$d))")
//    val mixture = util.eval("(($a+$b) * ($c+$d))*(($b+$a) * ($c+$d))*(($c+$a) * ($b+$d))")
    val mixture = util.eval("1.5*5*x^(1.5-1)*(1-x^1.5)^(5-1) + (5*2*x^(5-1)*(1-x^5)^(2-1))")
    val mixPlot = mixture.let { println("Mixture: $it"); it.plot2D("Exact PDF") }
    val integral = measureTimedValue { util.eval("Integrate($mixture, x) + 1") }
        .let { println("Integration time: ${it.duration}"); it.value }
    val zero = util.eval("f(x_):=$integral; f(0)").also { println("Zero:$it") }
    val norm = util.eval("f(x_):=$integral - $zero; f(1)").evalDouble().also { println("Norm:$it") }
    val cdf = util.eval("($integral - $zero) / $norm")
    cdf.also { println("CDF: $it"); it.plot2D("Exact CDF") }
    measureTimeMillis {
        compare({ binarySearch(zero = rand.nextDouble(), exp = cdf) })
    }.also { println("Inversion sampling time: $it ms") }

//    measureTimeMillis {
//        compare({
//            val t = Random().nextGaussian()
//            val p = if(rand.nextBoolean()) 1 else -1
//            p * (t + 5) + (1-p) *(t-5)})
//    } // + setOf(-5, -3, -1, 1, 3, 5).random() * 5 }
//        .also { println("Gaussian sampling time: $it ms") }
}

tailrec fun newtonSolver(
    exp: IExpr,
    zero: Double = 0.0,
    guess: Double = binarySearch(exp = exp, zero = zero),
    exp_dx: IExpr = util.eval("D($exp, x)"),
    nextGuess: Double = guess - util.eval("f(x_):=($exp)/($exp_dx); f($guess)").evalDouble(),
): Double =
    if (abs(util.eval("f(x_):=$exp - $zero; f($guess)").evalDouble()) < 0.001) guess
    else newtonSolver(zero = zero, guess = nextGuess, exp = exp, exp_dx = exp_dx)

// Only works on monotonically increasing functions (e.g. CDF)
tailrec fun binarySearch(
    exp: IExpr,
    zero: Double,
    iter: Int = 1,
    range: ClosedFloatingPointRange<Double> = 0.0..1.0,
    guess: Double = (range.endInclusive - range.start) / 2.0,
    delta: Double = 0.5.pow(iter) * (range.endInclusive - range.start).absoluteValue,
    eval: Double = util.eval("f(x_):=$exp; f($guess)").evalDouble(),
    error: Double = zero - eval
): Double = if (error.absoluteValue < 0.01 || iter > 200) guess
else if (error < 0) binarySearch(iter = iter + 1, guess = guess - delta, exp = exp, zero = zero)
else binarySearch(iter = iter + 1, guess = guess + delta, exp = exp, zero = zero)

private fun IExpr.plot2D(title: String, norm: Double = 1.0) {
    val labels = arrayOf("y")
    val xs = (0.01..1.0 step 0.01).toList()
    val ys = listOf(xs.map { util.eval("f(x_):=$this; f($it)").evalDouble() / norm })
    val data = (labels.zip(ys) + ("x" to xs)).toMap()
    val geoms = labels.map { geom_area(size = 2.0, color = "dark_green", fill = "light_green") { x = "x"; y = "y" } }
    val plot = geoms.foldRight(ggplot(data)) { it, acc -> acc + it } + ggtitle(title)
    plot.display()
}

infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0) { "Step must be positive, was: $step." }
    val sequence = generateSequence(start) { previous ->
        if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
        val next = previous + step
        if (next > endInclusive) null else next
    }
    return sequence.asIterable()
}

enum class Domain { INT, RATIONAL, DOUBLE }

// https://en.wikipedia.org/wiki/Kumaraswamy_distribution
fun randomKumaraswamy(v: String = "x", domain: Domain = Domain.RATIONAL) =
    when(domain) {
        Domain.INT -> "${rand.nextInt(2, 10)}".let {
            it to "${rand.nextInt(2, 10)}"
//            if (rand.nextBoolean()) "1" to it else it to "1"
        }//"${rand.nextInt(1, 5)}"
        Domain.RATIONAL -> rand.nextInt(2, 10).let { a -> "$a/${rand.nextInt(1, a)}" }.let {
//            it to "1"
            if (rand.nextBoolean()) "1" to it else it to "1"
//            it to rand.nextInt(2, 10).let { a -> "$a/${rand.nextInt(1, a)}" }
        }
        Domain.DOUBLE -> "${rand.nextDouble() * 5.0 + 1}".take(3) to "${rand.nextDouble() * 5.0 + 1}".take(3)
    }.let { (a, b) ->
        "($a*$b*$v^($a-1)*(1-$v^$a)^($b-1))"
    }

// https://escholarship.org/content/qt0wz7n7nm/qt0wz7n7nm.pdf#page=5
fun randomGottschling(v: String = "x") =
//    rand.nextInt(2, 10).let { a -> "$a/${rand.nextInt(1, a)}" }
    rand.nextDouble()
    .let { l ->
//        val g1 = Gamma.gamma((l + 1)/l)
//        val g2 = Gamma.gamma(1/(2*l))
//        "${(g1/g2)* sqrt(l / PI)}($l*$v^2 + 1)^(${-0.5*(1.0+1.0/l)})"
        "(Gamma(($l + 1)/$l)/Gamma(1/(2*$l))) * sqrt($l / PI)($l*$v^2 + 1)^(-(1/2)*(1+1/$l))"
    }

fun randomLogistic(v: String = "x") =
    (rand.nextInt(1, 10) to rand.nextInt(1, 2)).let { (u, s) ->
        "(1/(4*$s))*sech(($v-$u)/(2*$s))^2"
    }

// https://core.ac.uk/download/pdf/82415331.pdf
fun randomHarmonic(v: String = "x") =
    (rand.nextInt(1, 10) to rand.nextInt(1, 10)).let { (i, j) ->
        "$v^$i * log($v)^$j"
    }

fun randomExpontential(v: String = "x") = (rand.nextInt(0, 10) to rand.nextInt(0, 10)).let { (i, j) ->
    "$i * E^($v-$j)"
}

fun randomPolynomial(v: String = "x") = (rand.nextDouble() to rand.nextInt(1, 3)).let { (i, j) ->
    "$i * $v^$j"
}

fun randomSigmoid(v: String = "x") = (rand.nextDouble()).let { i ->
    "ln(1 + E^($v))"
}

val POPCOUNT = 10000

fun compare(vararg samplers: () -> Double) =
    compare(*samplers.map { f -> List(POPCOUNT) { f() } }.toTypedArray())

fun compare(vararg samples: List<Double>) {
    val data = mapOf<String, Any>(
        "x" to samples.fold(listOf<Double>()) { acc, function ->
            acc + function
        }
    )

    var p = lets_plot(data)
    p += geom_density(color = "dark_green", alpha = .3, fill = "light_green") { x = "x" }
    p + ggsize(2000, 1000)
    p.display()
}


fun Plot.display() =
    File.createTempFile("test", ".svg").also {
        val plotSize = DoubleVector(1000.0, 500.0)
        val plot = PlotSvgExport.buildSvgImageFromRawSpecs(this@display.toSpec(), plotSize)
        it.writeText(plot)
    }.also {
        ProcessBuilder(browserCmd, it.path).start()
    }

val browserCmd = System.getProperty("os.name").toLowerCase().let { os ->
    when {
        "win" in os -> "rundll32 url.dll,FileProtocolHandler"
        "mac" in os -> "open"
        "nix" in os || "nux" in os -> "x-www-browser"
        else -> throw Exception("Unable to open browser for unknown OS: $os")
    }
}
