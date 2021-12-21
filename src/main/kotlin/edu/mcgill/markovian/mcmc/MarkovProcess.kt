package edu.mcgill.markovian.mcmc

import ai.hypergraph.kaliningraph.types.Edge
import ai.hypergraph.kaliningraph.types.Graph
import ai.hypergraph.kaliningraph.types.IGF
import ai.hypergraph.kaliningraph.types.Vertex

interface MPFamily: IGF<MarkovProcess, Transition, State> {
  override val E: (s: State, t: State) -> Transition
    get() = { s, t -> Transition(s, t) }
  override val G: (vertices: Set<State>) -> MarkovProcess
    get() = { vertices: Set<State> -> MarkovProcess(vertices) }
  override val V: (old: State, edgeMap: (State) -> Set<Transition>) -> State
    get() = { old: State, edgeMap: (State) -> Set<Transition> -> State(old.id, edgeMap ) }
}

class MarkovProcess(states: Set<State>):
  MPFamily, Graph<MarkovProcess, Transition, State>(states)

class Transition(source: State, target: State):
  MPFamily, Edge<MarkovProcess, Transition, State>(source, target)

class State(
  id: String,
  override val edgeMap: (State) -> Set<Transition>
): MPFamily, Vertex<MarkovProcess, Transition, State>(id)