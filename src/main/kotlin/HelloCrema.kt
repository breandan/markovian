import ch.idsia.crema.factor.credal.vertex.VertexFactor
import ch.idsia.crema.inference.ve.CredalVariableElimination
import ch.idsia.crema.model.ObservationBuilder
import ch.idsia.crema.model.Strides
import ch.idsia.crema.model.graphical.SparseModel

fun main() {
    val p = 0.2
    val eps = 0.0001

    /*  CN defined with vertex Factor  */

    // Define the model (with vertex factors)
    val model = SparseModel<VertexFactor>()
    val A = model.addVariable(3)
    val B = model.addVariable(2)
    model.addParent(B, A)

    // Define a credal set of the partent node
    val fu = VertexFactor(model.getDomain(A), Strides.empty())
    fu.addVertex(doubleArrayOf(0.0, 1 - p, p))
    fu.addVertex(doubleArrayOf(1 - p, 0.0, p))
    model.setFactor(A, fu)


    // Define the credal set of the child
    val fx = VertexFactor(model.getDomain(B), model.getDomain(A))
    fx.addVertex(doubleArrayOf(1.0, 0.0), 0)
    fx.addVertex(doubleArrayOf(1.0, 0.0), 1)
    fx.addVertex(doubleArrayOf(0.0, 1.0), 2)
    model.setFactor(B, fx)

    // Run exact inference
    val inf = CredalVariableElimination(model)
    inf.query(A, ObservationBuilder.observe(B, 0))
}