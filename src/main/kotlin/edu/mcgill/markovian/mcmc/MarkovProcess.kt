package edu.mcgill.markovian.mcmc

import edu.mcgill.kaliningraph.*
import edu.mcgill.kaliningraph.typefamily.IGF

interface IMP: IGF<MarkovProcess, Transition, State> {
  override fun Edge(s: State, t: State) = Transition(s, t)

  override fun Graph(vertices: Set<State>) = MarkovProcess(vertices)

  override fun Vertex(
    newId: String,
    edgeMap: (State) -> Set<Transition>
  ) = State(newId, edgeMap)

}


class MarkovProcess(states: Set<State>):
  IMP, Graph<MarkovProcess, Transition, State>(states) {
}

class Transition(source: State, target: State):
  IMP, Edge<MarkovProcess, Transition, State>(source, target) {
}

class State(
  id: String,
  override val edgeMap: (State) -> Collection<Transition>
): IMP, Vertex<MarkovProcess, Transition, State>(id) {

  override fun Edge(s: State, t: State): Transition {
    TODO("Not yet implemented")
  }

  override fun Graph(vertices: Set<State>): MarkovProcess {
    TODO("Not yet implemented")
  }

  override fun Vertex(
    newId: String,
    edgeMap: (State) -> Set<Transition>
  ): State {
    TODO("Not yet implemented")
  }
}
