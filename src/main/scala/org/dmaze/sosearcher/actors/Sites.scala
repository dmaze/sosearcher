package org.dmaze.sosearcher.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import com.google.inject.Provides
import java.util.concurrent.TimeUnit.SECONDS
import org.dmaze.sosearcher.models.Site
import org.dmaze.sosearcher.seapi.{Site => APISite}
import play.api.Logging
import play.api.libs.concurrent.ActorModule
import scala.concurrent.duration.FiniteDuration
import scala.util.{Try, Success, Failure}

/**
  * Actor to maintain a list of known sites.
  *
  * This relies on an in-process cache, then an in-database cache, and
  * finally falls back to the Stack Exchange API to retrieve the list
  * of sites.
  *
  * In practice the list of sites is updated extremely rarely (not
  * more than once per day) and a caller can assume that a response
  * from the [[GetSites]] command will be valid for reasonable (but
  * not indefinite) periods.
  *
  * In typical use you can ask this [[GetSites]]:
  *
  * {{{
  * val actor: ActorRef[Sites.Command] = ... // injected by Guice
  * actor.ask(Sites.GetSites)
  *   .flatMap { case Sites.Reply(t) => Future.fromTry(t) }
  *   .map { sites => ... }
  * }}}
  */
object Sites extends ActorModule with Logging {

  /** A command that can be sent to this actor. */
  sealed trait Command
  type Message = Command

  /** Command to request the current list of sites. */
  final case class GetSites(replyTo: ActorRef[Reply]) extends Command

  // Helpers along the path to retrieving sites:
  private final case class DbRetrieved(sites: Try[Seq[Site]]) extends Command
  private final case class DbReplaced(sites: Try[Seq[Site]]) extends Command
  private final case class ApiRetrieved(sites: Try[Seq[APISite]])
      extends Command

  /** Reply from [[GetSites]] with the list of sites. */
  final case class Reply(sites: Try[Seq[Site]])

  implicit val timeout = new Timeout(5, SECONDS)

  // We will go through the request sequence on first use.  There are
  // three states, one where we don't yet have the site list yet; one
  // where we've started fetching it; and one where we have the whole
  // thing.

  /** Construct an actor. */
  @Provides
  def apply(
      sitesAPI: ActorRef[SitesAPI.Command],
      sitesDB: ActorRef[SitesDB.Command]
  ): Behavior[Command] = startup(sitesAPI, sitesDB)

  /**
    * State handler when we do not have the sites list.
    *
    * Transitions to [[fetching]] on a [[GetSites]] request.  Can
    * transition back here if part of that fetch failed.
    */
  private def startup(
      sitesAPI: ActorRef[SitesAPI.Command],
      sitesDB: ActorRef[SitesDB.Command]
  ): Behavior[Command] = {
    Behaviors.receive { (context, command) =>
      command match {
        case GetSites(replyTo) => {
          logger.info("Requesting sites from the database")
          context.ask(sitesDB, SitesDB.Retrieve) {
            case Success(SitesDB.Reply(sites)) => DbRetrieved(sites)
            case Failure(t)                    => DbRetrieved(Failure(t))
          }
          fetching(sitesAPI, sitesDB, Seq(replyTo))
        }
        case _ => Behaviors.unhandled
      }
    }
  }

  private def fetching(
      sitesAPI: ActorRef[SitesAPI.Command],
      sitesDB: ActorRef[SitesDB.Command],
      replies: Seq[ActorRef[Reply]]
  ): Behavior[Command] = {
    Behaviors.receive { (context, command) =>
      {
        // If any of the async actions fail, signal all waiting requests
        // with that failure, and return to the initial state.
        def fail(what: String, t: Throwable): Behavior[Command] = {
          logger.error(s"${what} failed: ${t.getMessage}")
          val reply = Reply(Failure(t))
          replies.foreach { _ ! reply }
          startup(sitesAPI, sitesDB)
        }

        def finish(sites: Seq[Site]): Behavior[Command] = {
          logger.info(s"Have ${sites.length} sites")
          val reply = Reply(Success(sites))
          replies.foreach { _ ! reply }
          steadyState(sitesAPI, sitesDB, sites)
        }

        command match {
          case GetSites(replyTo) => {
            // add it to the queue
            fetching(sitesAPI, sitesDB, replies :+ replyTo)
          }
          case DbRetrieved(Failure(t)) =>
            fail("Retrieving sites from the database", t)
          case DbRetrieved(Success(sites)) => {
            if (sites.isEmpty) {
              logger.info("No sites in the database, asking the SE API")
              // If there's nothing in the DB then we need to get
              // them from the API.
              context.ask(sitesAPI, SitesAPI.Request) {
                case Success(SitesAPI.Reply(sites)) => ApiRetrieved(sites)
                case Failure(t)                     => ApiRetrieved(Failure(t))
              }
              Behaviors.same
            } else {
              // If the DB produced something then we're all set.
              finish(sites)
            }
          }
          case ApiRetrieved(Failure(t)) =>
            fail("Retrieving sites from the SE API", t)
          case ApiRetrieved(Success(apiSites)) => {
            logger.info(s"SE API returned ${apiSites.length} sites, saving")
            // Write these into the database
            context.ask(sitesDB, SitesDB.Replace(apiSites, _)) {
              case Success(SitesDB.Reply(sites)) => DbReplaced(sites)
              case Failure(t)                    => DbReplaced(Failure(t))
            }
            Behaviors.same
          }
          case DbReplaced(Failure(t)) =>
            fail("Recording sites in the database", t)
          case DbReplaced(Success(sites)) => finish(sites)
        }
      }
    }
  }

  private def steadyState(
      sitesAPI: ActorRef[SitesAPI.Command],
      sitesDB: ActorRef[SitesDB.Command],
      sites: Seq[Site]
  ): Behavior[Command] = {
    Behaviors.receive { (context, command) =>
      command match {
        case GetSites(replyTo) => {
          replyTo ! Reply(Success(sites))
          Behaviors.same
        }
        case _ => Behaviors.unhandled
      }
    }
  }
}
