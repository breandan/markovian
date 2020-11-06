import ch.idsia.crema.factor.credal.vertex.VertexFactor
import ch.idsia.crema.inference.ve.CredalVariableElimination
import ch.idsia.crema.model.ObservationBuilder
import ch.idsia.crema.model.Strides
import ch.idsia.crema.model.graphical.SparseModel
import ch.idsia.crema.user.bayesian.BayesianNetwork

fun main() {
    val p = 0.2
    val eps = 0.0001

    /*  CN defined with vertex Factor  */

    // Define the model (with vertex factors)
    val model = SparseModel<VertexFactor>()
    val A = model.addVariable(3)
    val B = model.addVariable(2)
    model.addParent(B, A)

    // Define a credal set of the parent node
    val fu = VertexFactor(model.getDomain(A), Strides.empty()).apply {
        addVertex(doubleArrayOf(0.0, 1 - p, p))
        addVertex(doubleArrayOf(1 - p, 0.0, p))
    }
    model.setFactor(A, fu)


    // Define the credal set of the child
    val fx = VertexFactor(model.getDomain(B), model.getDomain(A)).apply {
        addVertex(doubleArrayOf(1.0, 0.0), 0)
        addVertex(doubleArrayOf(1.0, 0.0), 1)
        addVertex(doubleArrayOf(0.0, 1.0), 2)
    }
    model.setFactor(B, fx)

    model.sampleVertex()

    // Run exact inference
    val inf = CredalVariableElimination(model)
    val vertexFactor = inf.query(A, ObservationBuilder.observe(B, 0))
    println(vertexFactor)
}