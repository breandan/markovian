package edu.mcgill.markovian.mcmc

import ai.hypergraph.kaliningraph.typefamily.Edge
import ai.hypergraph.kaliningraph.typefamily.Graph
import ai.hypergraph.kaliningraph.typefamily.Vertex

class MarkovProcess(states: Set<State>):
  Graph<MarkovProcess, Transition, State>(states)

class Transition(source: State, target: State):
  Edge<MarkovProcess, Transition, State>(source, target)

class State(
  id: String,
  override val edgeMap: (State) -> Set<Transition>
): Vertex<MarkovProcess, Transition, State>(id)