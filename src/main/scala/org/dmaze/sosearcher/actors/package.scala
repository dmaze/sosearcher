package org.dmaze.sosearcher

/**
  * Akka Actors supporting the searcher.
  * 
  * These perform various asynchronous and possibly stateful tasks,
  * such as maintaining the list of known Stack Exchange sites.
  * 
  * =Useful Actors=
  * 
  * To request the list of Stack Exchange sites, call [[Sites]]:
  * 
  * {{{
  * val actor: ActorRef[Sites.Command] = ... // injected by Guice
  * actor.ask(Sites.GetSites)
  *   .flatMap { case Sites.Reply(t) => Future.fromTry(t) }
  *   .map { sites => ... }
  * }}}
  */
package object actors {
}
