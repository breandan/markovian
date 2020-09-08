import org.matheclipse.core.eval.ExprEvaluator
import org.matheclipse.core.expression.F
import org.matheclipse.core.interfaces.IAST
import org.matheclipse.core.interfaces.IExpr
import org.matheclipse.core.interfaces.ISymbol
import org.matheclipse.parser.client.SyntaxError
import org.matheclipse.parser.client.math.MathException

fun main() = try {
    val util = ExprEvaluator(false, 100)

    // Convert an expression to the internal Java form:
    // Note: single character identifiers are case sensitive
    // (the "D()" function identifier must be written as upper case
    // character)
    val javaForm: String = util.toJavaForm("D(sin(x)*cos(x),x)")
    // prints: D(Times(Sin(x),Cos(x)),x)
    println("Out[1]: $javaForm")

    // Use the Java form to create an expression with F.* static
    // methods:
    val x: ISymbol = F.Dummy("x")
    var function: IAST = F.D(F.Times(F.Sin(x), F.Cos(x)), x)
    var result: IExpr = util.eval(function)
    // print: Cos(x)^2-Sin(x)^2
    println("Out[2]: $result")

    // Note "diff" is an alias for the "D" function
    result = util.eval("diff(sin(x)*cos(x),x)")
    // print: Cos(x)^2-Sin(x)^2
    println("Out[3]: $result")

    // evaluate the last result (% contains "last answer")
    result = util.eval("%+cos(x)^2")
    // print: 2*Cos(x)^2-Sin(x)^2
    println("Out[4]: $result")

    // evaluate an Integrate[] expression
    result = util.eval("integrate(sin(x)^5,x)")
    // print: 2/3*Cos(x)^3-1/5*Cos(x)^5-Cos(x)
    println("Out[5]: $result")

    // set the value of a variable "a" to 10
    result = util.eval("a=10")
    // print: 10
    println("Out[6]: $result")

    // do a calculation with variable "a"
    result = util.eval("a*3+b")
    // print: 30+b
    println("Out[7]: $result")

    // Do a calculation in "numeric mode" with the N() function
    // Note: single character identifiers are case sensistive
    // (the "N()" function identifier must be written as upper case
    // character)
    result = util.eval("N(sinh(5))")
    // print: 74.20321057778875
    println("Out[8]: $result")

    // define a function with a recursive factorial function definition.
    // Note: fac(0) is the stop condition.
    result = util.eval("fac(x_Integer):=x*fac(x-1);fac(0)=1")
    // now calculate factorial of 10:
    result = util.eval("fac(10)")
    // print: 3628800
    println("Out[9]: $result")
    function = F.Function(F.Divide(F.Gamma(F.Plus(F.C1, F.Slot1)), F.Gamma(F.Plus(F.C1, F.Slot2))))
    // eval function ( Gamma(1+#1)/Gamma(1+#2) ) & [23,20]
    result = util.evalFunction(function, "23", "20")
    // print: 10626
    println("Out[10]: $result")
} catch (e: SyntaxError) {
    // catch Symja parser errors here
    println(e.message)
} catch (me: MathException) {
    // catch Symja math errors here
    println(me.message)
} catch (ex: Exception) {
    println(ex.message)
} catch (soe: StackOverflowError) {
    println(soe.message)
} catch (oome: OutOfMemoryError) {
    println(oome.message)
}