package org.dmaze.sosearcher.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.google.inject.Provides
import org.dmaze.sosearcher.seapi.{Site => APISite}
import org.dmaze.sosearcher.db.{Site => DBSite, Sites}
import org.dmaze.sosearcher.models.Site
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.ActorModule
import scala.concurrent.duration.FiniteDuration
import scala.util.{Try, Success, Failure}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

/**
  * Actor to persist the list of sites in a database.
  *
  * It is assumed the list of sites will be relatively short, so
  * the messages here deal only in the entire list of sites.  Since
  * the [[org.dmaze.sosearcher.models.Site]] object includes the
  * database ID, this is also the point that fills those in.
  *
  * This generally does not need to be used by application code
  * directly; it is invoked via [[Sites]] for a local backing store.
  */
object SitesDB extends ActorModule {

  /** A command that can be sent to this actor. */
  sealed trait Command
  type Message = Command

  /** Command to request the current list of sites (if any). */
  final case class Retrieve(replyTo: ActorRef[Reply]) extends Command

  /** Command to replace the current list of sites. */
  final case class Replace(sites: Seq[APISite], replyTo: ActorRef[Reply])
      extends Command

  /** Command indicating the query part of [[Retrieve]] has completed. */
  private final case class Retrieved(
      sites: Try[Seq[DBSite]],
      replyTo: ActorRef[Reply]
  ) extends Command

  /** Command indicating the query part of [[Replace]] has completed. */
  private final case class ReplaceMerge(
      ids: Seq[(String, Int)],
      sites: Seq[APISite],
      replyTo: ActorRef[Reply]
  ) extends Command

  /** The response from either [[Command]], embedding the list of sites. */
  final case class Reply(sites: Try[Seq[Site]])

  /** Construct an actor. */
  @Provides
  def apply(dbConfigProvider: DatabaseConfigProvider): Behavior[Command] = {
    val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get
    val db = dbConfig.db
    import dbConfig.profile.api._

    Behaviors.receive { (context, command) =>
      command match {
        case Retrieve(replyTo) => {
          val action = TableQuery[Sites].filter(_.active).result
          val future = db.run(action)
          context.pipeToSelf(future) { case t => Retrieved(t, replyTo) }
          Behaviors.same
        }
        case Retrieved(tSites, replyTo) => {
          replyTo ! Reply(tSites.map { _.map { _.toModel } })
          Behaviors.same
        }
        case Replace(sites, replyTo) => {
          // We ultimately need to be in a state where
          // (a) every site in `sites` is in the db active=true
          // (b) every site in the db not in `sites` is active=false
          //
          // The "most SQLish" way to do this is to create a temporary
          // table, dump the new sites into it, and then do the requisite
          // operations database-side.
          //
          // Doing this client-side is probably less annoying, and
          // since our database (for now) is SQLite the performance
          // won't be awful.
          //
          // Step one is to get a dump of the current site IDs.
          val action = TableQuery[Sites].map { r =>
            (r.apiSiteParameter, r.id)
          }.result
          val future = db.run(action)
          context.pipeToSelf(future) {
            // If this fails, jump straight to the response
            case Success(newSites) => ReplaceMerge(newSites, sites, replyTo)
            case Failure(t)        => Retrieved(Failure(t), replyTo)
          }
          Behaviors.same
        }
        case ReplaceMerge(ids, sites, replyTo) => {
          val idMap = Map.from(ids)
          // Update the "sites" list to have the matching IDs
          val newSites = sites.map { s =>
            DBSite(
              databaseId = idMap.get(s.attributes.apiSiteParameter),
              attributes = s.attributes,
              active = true
            )
          }
          // In the database, mark everything as inactive, then
          // upsert the new list of sites
          val deactivate = TableQuery[Sites].map { _.active }.update(false)
          val upserts = newSites.map { TableQuery[Sites].insertOrUpdate(_) }
          val action = DBIO.seq(deactivate +: upserts: _*)
          val future = db.run(action)
          context.pipeToSelf(future) {
            // If we get back success, replace it with the updated
            // site list; either way go to the "Retrieved" state
            // which sends back the result
            case t =>
              Retrieved(t.map { _ =>
                newSites
              }, replyTo)
          }
          Behaviors.unhandled
        }
      }
    }
  }
}
