package plate

import kotlin.reflect.KProperty

// https://github.com/todesking/platebuilder
// https://en.wikipedia.org/wiki/Plate_notation

class Notation {
}

fun main() {
    val d by Dirichlet()
    val u by Uniform()
}

open class Distribution {
    open operator fun getValue(nothing: Nothing?, property: KProperty<*>): Distribution = this
}

class Dirichlet : Distribution()
class Gaussian : Distribution()
class Uniform : Distribution()
class Poisson : Distribution()