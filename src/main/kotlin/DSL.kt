import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.plot.PlotSvgExport
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
import org.matheclipse.core.graphics.Show2SVG
import org.matheclipse.core.interfaces.IExpr
import java.io.File
import java.util.*
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

val util = ExprEvaluator(false, 100)

val rand = kotlin.random.Random(1)
fun main() {
    measureTimeMillis {
        val a = randomKumaraswamy()
        val b = randomKumaraswamy()
        val c = randomKumaraswamy()

        val mixture = "$a*($b + $c)".also { println("Mixture: $it") }
        val integral = util.eval("integrate($mixture, x)").also { println("Integral: $it") }.also { it.plot2D() }
        val variate = rand.nextDouble().toString().also { println("Variate: $it") }
        val result = util.eval("solve({$integral-$variate==0, 0<=x, x<=1}, {x})").also { println("Result: $it") }
    }.also { println("Time: $it ms") }

//    compare(
////        {
////        util.eval("x = ${rand.nextDouble()}")
////        util.eval("RandomVariate(NormalDistribution(0,1), 10^1)")
////            .toString().drop(1).dropLast(1).split(",").first() .toDouble()
////    },
////        util.eval("integrate(PDF(NormalDistribution(0, 1), x))").toString().toDouble() },
////        { //0.5 * rand.nextGaussian() + 0.5 * (rand.nextGaussian() + 4)
////        {
////            util.eval("integrate(0.25 * sech((x-10)/2)^2 + 0.25 * sech((x+10)/2)^2, x)")
////                .also { println(it) }
////                .toString().toDouble()
////        }
//        List(POPCOUNT) {
//            val t = rand.nextGaussian()
//            val p = if(rand.nextBoolean()) 1 else -1
//            p * (t + 5) + (1-p) *(t-5)
//        } // + setOf(-5, -3, -1, 1, 3, 5).random() * 5 }
//            .also { println(it.count { it < 0 }) }
//    )


}

private fun IExpr.plot2D() {
    val labels = arrayOf("y")
    val xs = (0.0..1.0 step 0.01).toList()
    val ys = listOf(xs.map { util.eval("f(x_):=$this; f($it)").also { println(it) }.evalDouble() })
    val data = (labels.zip(ys) + ("x" to xs)).toMap()
    val colors = listOf("dark_green", "gray", "black", "red", "orange", "dark_blue")
    val geoms = labels.zip(colors).map { geom_path(size = 2.0, color = it.second) { x = "x"; y = "y" } }
    val plot = geoms.foldRight(ggplot(data)) { it, acc -> acc + it } + ggtitle("CDF")
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
fun randomKumaraswamy(domain: Domain = Domain.INT) =
    when(domain) {
       Domain.INT -> (("${rand.nextInt(2, 5)}") to ("${rand.nextInt(2, 5)}"))
       Domain.RATIONAL -> (("${rand.nextInt(2, 5)}/${rand.nextInt(2, 5)}") to ("${rand.nextInt(2, 5)}/${rand.nextInt(2, 5)}"))
       Domain.DOUBLE ->  (("${rand.nextDouble()* 5.0}") to ("${rand.nextDouble() * 5.0}"))
    }.let { (a, b) ->
        "($a*$b*x^($a-1)*(1-x^$a)^($b-1))"
    }

// https://escholarship.org/content/qt0wz7n7nm/qt0wz7n7nm.pdf#page=5
fun randomGottschling()=
    rand.nextDouble().let { l ->
        val g1 = Gamma.gamma((l + 1)/l)
        val g2 = Gamma.gamma(1/(2*l))
        "${(g1/g2)* sqrt(l / PI)}($l*x^2 + 1)^(${-0.5*(1.0+1.0/l)})"
    }

// https://core.ac.uk/download/pdf/82415331.pdf
fun randomHarmonic() =
    (rand.nextInt(-10, 10) to rand.nextInt(0, 10)).let { (i, j) ->
        "x^$i * log(x)^$j"
    }

val POPCOUNT = 1000

fun compare(vararg samplers: () -> Double) =
    compare(*samplers.map { f -> List(POPCOUNT) { f() } }.toTypedArray())

fun compare(vararg samples: List<Double>) {
    val data = mapOf<String, Any>(
        "rating" to samples.fold(listOf<Double>()) { acc, function ->
            acc + function
        },
        "cond" to samples.foldIndexed(listOf<String>()) { i, acc, function ->
            acc + function.map { "$i" }
        }
    )

    var p = lets_plot(data)
    p += geom_density(color = "dark_green", alpha = .3) { x = "rating"; fill = "cond" }
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
