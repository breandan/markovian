package edu.mcgill.markovian.mcmc

import edu.mcgill.kaliningraph.*

class MarkovProcess(states: Set<State>):
  Graph<MarkovProcess, Transition, State>(states)

class Transition(source: State, target: State):
  Edge<MarkovProcess, Transition, State>(source, target)

class State(
  id: String,
  override val edgeMap: (State) -> Set<Transition>
): Vertex<MarkovProcess, Transition, State>(id)