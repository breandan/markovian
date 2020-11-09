import edu.mcgill.kaliningraph.DEFAULT_RANDOM
import jetbrains.datalore.base.geometry.DoubleVector
import jetbrains.datalore.plot.PlotSvgExport
import jetbrains.letsPlot.geom.geom_abline
import jetbrains.letsPlot.geom.geom_contourf
import jetbrains.letsPlot.geom.geom_density
import jetbrains.letsPlot.ggsize
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.intern.toSpec
import jetbrains.letsPlot.lets_plot
import java.io.File

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

const val POPCOUNT = 10000

fun compare(vararg samplers: (Double) -> Double) =
    compare(
        *samplers.map { f ->
            (1..POPCOUNT).pmap { f(DEFAULT_RANDOM.nextDouble()) }
        }.toTypedArray()
    )

fun compare(vararg samples: List<Double>) =
    lets_plot(
        mapOf<String, Any>(
            "x" to samples.fold(listOf<Double>()) { acc, function ->
                acc + function
            },
            "" to samples.mapIndexed { i, s -> List(s.size) { "PDF$i" } }.flatten()
        )
    ).let {
        it + geom_density(alpha = .3) { x = "x"; fill = "" } + ggsize(500, 250)
    }
