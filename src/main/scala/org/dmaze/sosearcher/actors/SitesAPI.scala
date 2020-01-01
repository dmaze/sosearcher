package org.dmaze.sosearcher.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.google.inject.Provides
import java.util.concurrent.TimeUnit.SECONDS
import org.dmaze.sosearcher.seapi.{ApiWrapper, Site}
import play.api.libs.concurrent.ActorModule
import play.api.libs.ws.{WSClient, WSResponse}
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
  * Actor to interact with the SE "sites" API.
  *
  * This can request the list of sites and return it.  This changes
  * very infrequently (at most once per day) and the API documentation
  * notes that it should be called sparingly.  You generally would
  * want to use a higher-level actor like [[Sites]] that can cache
  * the site response.
  */
object SitesAPI extends ActorModule {

  /** A command that can be sent to this actor. */
  sealed trait Command
  type Message = Command

  /** Command to request the list of sites. */
  final case class Request(replyTo: ActorRef[Reply]) extends Command

  /** Trigger handling when the SE API returns a list of sites. */
  private final case class Respond(response: Try[WSResponse]) extends Command

  /** Trigger forward processing when a back-off has completed. */
  private final case class BackedOff() extends Command

  /** A reply from a [[Request]] action. */
  final case class Reply(sites: Try[Seq[Site]])

  // Get implicits for JSON decoding.
  import Site.readsSite
  import ApiWrapper.readsApiWrapper

  // Helper for tracking the timer
  private case object TimerKey

  /** Construct an actor from an HTTP client. */
  @Provides
  def apply(ws: WSClient): Behavior[Command] =
    Behaviors.withTimers(timers => idle(ws, timers))

  // There are three states this actor can be in:
  //
  // (1) We are idle.  This is the initial state.
  // (2) We have launched a request; it is inFlight.  We pass along
  //     the list of active requests and queue them up until we get
  //     some single response.
  // (3) The API has asked us to back off so we do.  We batch up any
  //     requests that arrive.
  //
  //                 Request                       Request
  //       Request    v    ^   Response (backoff)   v   ^
  // idle ---------> inFlight -------------------> backOff
  //  ^                 |  ^                        v   |
  //  +-----------------+  +------------------------+   |
  //  |    Response           Waited (w/ requests)      |
  //  +-------------------------------------------------+
  //                          Waited (no requests)

  /**
    * State behavior for the initial (idle) state.
    *
    * Transition to [[inFlight]] when a [[Request]] is received.
    */
  private def idle(
      ws: WSClient,
      timers: TimerScheduler[Command]
  ): Behavior[Command] = {
    Behaviors.receive { (context, command) =>
      command match {
        case Request(replyTo) =>
          makeRequest(context, ws, timers, Seq(replyTo))
        case Respond(response) =>
          Behaviors.unhandled
        case BackedOff() =>
          Behaviors.unhandled
      }
    }
  }

  /** Helper to transition to the {inFlight} state. */
  private def makeRequest(
      context: ActorContext[Command],
      ws: WSClient,
      timers: TimerScheduler[Command],
      replies: Seq[ActorRef[Reply]]
  ): Behavior[Command] = {
    val future = ws
      .url(Site.url)
      .addHttpHeaders("Accept" -> "application/json")
      .addQueryStringParameters("filter" -> Site.filter)
      .get()
    context.pipeToSelf(future) { case t => Respond(t) }
    inFlight(ws, timers, replies)
  }

  /**
    * State behavior for the in-flight state.
    *
    * Transition to this state from {idle} when at least one
    * {Request} has been received.  Stay in this state until a
    * response arrives, then transition to either {idle} or {backOff}
    * depending on whether a backoff was requested.
    */
  private def inFlight(
      ws: WSClient,
      timers: TimerScheduler[Command],
      replies: Seq[ActorRef[Reply]]
  ): Behavior[Command] = {
    Behaviors.receive { (context, command) =>
      command match {
        case Request(replyTo) =>
          inFlight(ws, timers, replies :+ replyTo)
        case Respond(tResponse) => {
          val tWrapper = tResponse.map { _.json.as[ApiWrapper[Site]] }
          val backoff = tWrapper.toOption.flatMap { _.backoff }.getOrElse(0)
          val tSites = tWrapper.flatMap { _.getItems }
          val response = Reply(tSites)
          replies.foreach { _ ! response }
          if (backoff == 0) {
            idle(ws, timers)
          } else {
            timers.startSingleTimer(
              TimerKey,
              BackedOff(),
              FiniteDuration(backoff, SECONDS)
            )
            backOff(ws, timers, Seq())
          }
        }
        case BackedOff() => {
          Behaviors.unhandled
        }
      }
    }
  }

  /**
    * State behavior for the backoff state.
    *
    * Transition to this state from [[inFlight]] when the API
    * response included a backoff time.  That handler is also
    * responsible for setting a timeout.  If we receive any Request
    * commands in this state, them up and do not launch a request.
    *
    * When we receive a BackedOff command, the timer has gone
    * off.  If there are no outstanding requests, return to [[idle]]
    * state; otherwise launch a single network request and go to
    * [[inFlight]].
    */
  private def backOff(
      ws: WSClient,
      timers: TimerScheduler[Command],
      replies: Seq[ActorRef[Reply]]
  ): Behavior[Command] = {
    Behaviors.receive { (context, command) =>
      command match {
        case Request(replyTo) =>
          backOff(ws, timers, replies :+ replyTo)
        case Respond(_) =>
          Behaviors.unhandled
        case BackedOff() => {
          if (replies.isEmpty) {
            idle(ws, timers)
          } else {
            makeRequest(context, ws, timers, replies)
          }
        }
      }
    }
  }
}
