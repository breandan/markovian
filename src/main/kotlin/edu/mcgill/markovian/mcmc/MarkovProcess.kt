package edu.mcgill.markovian.mcmc

import edu.mcgill.kaliningraph.*

class MarkovProcess(states: Set<State>):
  Graph<MarkovProcess, Transition, State>(states) {
  override fun new(vertices: Set<State>): MarkovProcess {
    TODO("Not yet implemented")
  }
}

class Transition(source: State, target: State):
  Edge<MarkovProcess, Transition, State>(source, target) {
  override fun new(source: State, target: State): Transition {
    TODO("Not yet implemented")
  }
}

class State(
  id: String,
  override val edgeMap: (State) -> Collection<Transition>
): Vertex<MarkovProcess, Transition, State>(id) {

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
