package org.dmaze.sosearcher.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer}
import akka.util.Timeout
import com.google.inject.Provides
import java.util.concurrent.TimeUnit.SECONDS
import org.dmaze.sosearcher.db.{
  DbBacked,
  Fetch,
  Fetches,
  FetchType,
  FetchTypes,
  PostType,
  PostTypes
}
import org.dmaze.sosearcher.models.Site
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.ActorModule
import scala.util.{Failure, Success, Try}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

/**
  * Actor to record fetched questions.
  */
object FetchDB extends ActorModule {

  /** A command that can be sent to this actor. */
  sealed trait Command
  type Message = Command

  /**
    * Command to initiate fetching.
    *
    * If the requested item has already been fetched, responds with
    * [[AlreadyFetched]] and a completed fetch record.  Otherwise
    * creates a fetch record and returns it in a [[PleaseFetch]]
    * message.
    *
    * The `postType` and `fetchType` parameters only need their
    * string values filled in, actual database IDs will be populated
    * from a local cache.
    */
  final case class Request(
      site: Site,
      number: Long,
      postType: PostType,
      fetchType: FetchType,
      replyTo: ActorRef[Response]
  ) extends Command

  /**
    * Command to replay partially fetched items out of the database.
    *
    * This scans the database for items that do not have a fetched
    * timestamp, and for each, generates a [[PleaseFetch]] request.
    * It may signal [[CannotFetch]] on error but will not signal
    * [[AlreadyFetched]].  There is no signal once all items have
    * been replayed.
    */
  final case class Replay(replyTo: ActorRef[Response]) extends Command

  /** Response to a [[Request]] or [[Replay]]. */
  sealed trait Response

  /**
    * Response indicating fetching of the requested item has not yet
    * been started.  Fetch the item, fill in the [[Fetch.timestamp]]
    * and [[Fetch.result]] fields of the returned object, and send a
    * [[Fetched]] message to record the final status.
    */
  final case class PleaseFetch(fetch: Fetch) extends Response

  /**
    * Response indicating the requested item has already been fetched
    * or that fetching is in progress.
    */
  final case class AlreadyFetched(fetch: Fetch) extends Response

  /** Response indicating an error looking up fetch status. */
  final case class CannotFetch(t: Throwable) extends Response

  /**
    * Command to commit a completed fetch activity.  Generally the
    * `fetch` parameter will come from a [[PleaseFetch]] response to a
    * [[Request]]; the included database IDs must be filled in already.
    */
  final case class Record(fetch: Fetch, replyTo: Option[ActorRef[Try[Unit]]])
      extends Command

  /** Internal command used at startup time when the list of post
    * types has been retrieved. */
  private final case class HavePostTypes(postTypes: Try[Seq[PostType]])
      extends Command

  /** Internal command used at startup time when the list of fetch
    * types has been retrieved. */
  private final case class HaveFetchTypes(fetchTypes: Try[Seq[FetchType]])
      extends Command

  /** Internal command triggered after we've looked up whether or not a
    * [[Request]] already exists. */
  private final case class Lookup(
      request: Request,
      fetch: Fetch,
      result: Try[Seq[Fetch]]
  ) extends Command

  /** Internal command triggered after a new fetch has been inserted. */
  private final case class NewFetch(
      request: Request,
      fetch: Fetch,
      result: Try[Int]
  ) extends Command

  /** Internal command triggered on the response to the [[Replay]] query. */
  private final case class Replayed(
      replyTo: ActorRef[Response],
      result: Try[Seq[Fetch]]
  ) extends Command

  /** Internal command triggered after [[Record]] has inserted a record. */
  private final case class Recorded(
      replyTo: Option[ActorRef[Try[Unit]]],
      result: Try[Int]
  ) extends Command

  /**
    * Create a fetch-database actor.
    */
  @Provides
  def apply(dbConfigProvider: DatabaseConfigProvider): Behavior[Command] =
    Behaviors.withStash(1000) { buffer =>
      startup(buffer, dbConfigProvider)
    }

  /**
    * State on initial startup.  This fetches the lists of post and
    * fetch types, and then transitions to [[steadyState]].  Any
    * messages not directly related to this startup sequence are
    * buffered.
    */
  private def startup(
      buffer: StashBuffer[Command],
      dbConfigProvider: DatabaseConfigProvider
  ) = {
    val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get
    val db = dbConfig.db
    import dbConfig.profile.api._

    Behaviors.setup[Command] { context =>
      {
        // Launch the two fetches up front
        context.pipeToSelf(db.run(PostTypes.query.result)) {
          HavePostTypes(_)
        }
        context.pipeToSelf(db.run(FetchTypes.query.result)) {
          HaveFetchTypes(_)
        }

        initialFetch(buffer, dbConfigProvider, None, None)
      }
    }
  }

  /**
    * State while performing the initial fetch.  We are waiting for
    * one or both of the post and fetch type lists to come back.
    * Any messages unrelated to this activity is buffered.  Stay
    * in this state until we have the startup-time data, then transition
    * to [[steadyState]].  If we can't retrieve or unmarshal the data,
    * signal failure and stop the actor.
    */
  private def initialFetch(
      buffer: StashBuffer[Command],
      dbConfigProvider: DatabaseConfigProvider,
      postTypes: Option[Map[String, Int]],
      fetchTypes: Option[Map[String, Int]]
  ): Behavior[Command] = {
    // Determine the next state: advance to [[steadyState]] if we have
    // both parts or stay here if not.
    def next(
        pt: Option[Map[String, Int]],
        ft: Option[Map[String, Int]]
    ): Behavior[Command] = {
      (pt, ft) match {
        case (Some(pts), Some(fts)) =>
          buffer.unstashAll(steadyState(dbConfigProvider, pts, fts))
        case _ => initialFetch(buffer, dbConfigProvider, pt, ft)
      }
    }
    // Stop the world if we need to.  Advance to [[failing]], send all
    // of the stashed messages there, and stop ourselves.
    def abort(t: Throwable): Behavior[Command] = {
      buffer.unstashAll(failing(t))
      Behaviors.stopped
    }
    Behaviors.receive { (context, command) =>
      {
        command match {
          case HavePostTypes(Success(pt)) => {
            postTypesToMap(pt)
              .fold(
                t => {
                  context.self ! HavePostTypes(Failure(t))
                  Behaviors.same
                },
                map => next(Some(map), fetchTypes)
              )
          }
          case HavePostTypes(Failure(t)) => abort(t)
          case HaveFetchTypes(Success(ft)) => {
            fetchTypesToMap(ft)
              .fold(
                t => {
                  context.self ! HaveFetchTypes(Failure(t))
                  Behaviors.same
                },
                map => next(postTypes, Some(map))
              )
          }
          case HaveFetchTypes(Failure(t)) => abort(t)
          case message => {
            buffer.stash(message)
            Behaviors.same
          }
        }
      }
    }
  }

  /**
    * State when some part of the initial startup has failed.
    * This reacts to messages with callbacks by sending the error to
    * the callback, and ignores everything else.
    */
  private def failing(t: Throwable): Behavior[Command] =
    Behaviors.receiveMessage {
      case Request(_, _, _, _, replyTo) => {
        replyTo ! CannotFetch(t)
        Behaviors.same
      }
      case _ => Behaviors.same
    }

  /**
    * State where we've gotten all of the startup-time data and are
    * ready to keep processing requests.
    */
  private def steadyState(
      dbConfigProvider: DatabaseConfigProvider,
      postTypes: Map[String, Int],
      fetchTypes: Map[String, Int]
  ): Behavior[Command] = {
    val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get
    val db = dbConfig.db
    import dbConfig.profile.api._

    Behaviors.receive { (context, command) =>
      command match {
        case request @ Request(site, number, postType, fetchType, replyTo) => {
          // The first check is whether a fetch record exists at all.
          // (If it does, we're done here.)
          val fq = for {
            postTypeId <- postTypes
              .get(postType.postType)
              .toRight({
                new DatabaseException(
                  s"no such post type ${postType.postType}"
                )
              })
              .toTry
            fetchTypeId <- fetchTypes
              .get(fetchType.fetchType)
              .toRight({
                new DatabaseException(
                  s"no such fetch type ${fetchType.fetchType}"
                )
              })
              .toTry
            siteId <- site.databaseId
              .toRight({
                new DatabaseException(s"site ${site.name} without an ID")
              })
              .toTry
            fetch = Fetch(
              None,
              postTypeId,
              siteId,
              request.number,
              fetchTypeId,
              None,
              None
            )
            query = Fetches.query
              .filter(_.postTypeId === postTypeId)
              .filter(_.siteId === siteId)
              .filter(_.postNumber === request.number)
              .filter(_.fetchTypeId === fetchTypeId)
              .take(1)
              .result
          } yield (fetch, query)
          fq.fold(
            { replyTo ! CannotFetch(_) }, {
              case (fetch, query) =>
                context.pipeToSelf(db.run(query)) { Lookup(request, fetch, _) }
            }
          )
          Behaviors.same
        }
        case Lookup(request, _, Failure(t)) => {
          // If we can't look up whether or not we need to fetch the
          // document, forward the error back.
          request.replyTo ! CannotFetch(t)
          Behaviors.same
        }
        case Lookup(request, fetch, Success(Seq())) => {
          // We need to record that a fetch will happen.
          val q = (Fetches.query returning Fetches.query.map(_.id)) += fetch
          context.pipeToSelf(db.run(q)) {
            NewFetch(request, fetch, _)
          }
          Behaviors.same
        }
        case Lookup(request, _, Success(fetches)) => {
          // We've already recorded a fetch result so we don't need
          // to do anything here.
          request.replyTo ! AlreadyFetched(fetches.head)
          Behaviors.same
        }
        case NewFetch(request, fetch, result) => {
          // Send the success or failure of this INSERT back to the
          // caller; if it was successful we do need to continue with
          // the actual fetching.
          val response = result.fold(
            CannotFetch(_),
            id => PleaseFetch(fetch.copy(databaseId = Some(id)))
          )
          request.replyTo ! response
          Behaviors.same
        }
        case Replay(replyTo) => {
          val query = Fetches.query.filter(_.timestamp.isEmpty).result
          context.pipeToSelf(db.run(query)) { Replayed(replyTo, _) }
          Behaviors.same
        }
        case Replayed(replyTo, tFetches) => {
          tFetches.fold({
            replyTo ! CannotFetch(_)
          }, {
            _.foreach { replyTo ! PleaseFetch(_) }
          })
          Behaviors.same
        }
        case Record(fetch, replyTo) => {
          fetch.databaseId.fold({
            val response =
              Failure(new DatabaseException("recording uncommitted fetch"))
            replyTo.foreach { _ ! response }
          })({ dbid =>
            val query = Fetches.query
              .filter(_.id === dbid)
              .map(f => (f.timestamp, f.result))
              .update((fetch.timestamp, fetch.result))
            context.pipeToSelf(db.run(query)) { Recorded(replyTo, _) }
          })
          Behaviors.same
        }
        case Recorded(replyTo, result) => {
          val response = result map Function.const(())
          replyTo.foreach { _ ! response }
          Behaviors.same
        }
        case HavePostTypes(Success(pt)) =>
          // This is theoretically a startup-time message, but we
          // could get duplicates.  We already have a valid map
          // of post types.  Replace the one we have if we can
          // unmarshal it, and ignore it if not.
          postTypesToMap(pt)
            .fold(
              Function.const(Behaviors.same), // ignore errors
              steadyState(dbConfigProvider, _, fetchTypes)
            )
        case HavePostTypes(Failure(_)) => Behaviors.same
        case HaveFetchTypes(Success(ft)) =>
          fetchTypesToMap(ft)
            .fold(
              Function.const(Behaviors.same), // ignore errors
              steadyState(dbConfigProvider, postTypes, _)
            )
        case HaveFetchTypes(Failure(_)) => Behaviors.same
      }
    }
  }

  /**
    * Convert a sequence of *-type records to a map from type to
    * database ID.  Checks that all expected types are present.
    */
  private def seqToMap[T <: DbBacked](
      types: Seq[T],
      name: (T => String),
      what: String,
      required: Seq[String]
  ): Try[Map[String, Int]] = {
    val noDatabaseId: Seq[String] = types
      .filter { _.databaseId.isEmpty }
      .map { name(_) }
    if (noDatabaseId.nonEmpty) {
      val names = noDatabaseId.mkString("; ")
      Failure(
        new DatabaseException(
          s"${what}s without assign database IDs: ${names}"
        )
      )
    } else {
      val pairs = types.map { pt =>
        (name(pt), pt.databaseId.get)
      }
      val m = Map.from(pairs)

      val missing = required.filter { !m.contains(_) }
      if (missing.nonEmpty) {
        val names = missing.mkString("; ")
        Failure(
          new DatabaseException(s"missing required ${what}s: ${names}")
        )
      } else {
        Success(m)
      }
    }
  }

  private def postTypesToMap(types: Seq[PostType]): Try[Map[String, Int]] =
    seqToMap(
      types, { pt: PostType =>
        pt.postType
      },
      "post type",
      Seq(PostType.question)
    )

  private def fetchTypesToMap(types: Seq[FetchType]): Try[Map[String, Int]] =
    seqToMap(
      types, { ft: FetchType =>
        ft.fetchType
      },
      "fetch type",
      Seq(FetchType.metadata, FetchType.body)
    )

  /** Exception when the database format isn't what was expected. */
  class DatabaseException(msg: String) extends Exception(msg)
}
