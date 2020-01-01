package org.dmaze.sosearcher.controllers

import akka.actor.typed.{ActorRef, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import javax.inject._
import org.dmaze.sosearcher.actors.{Sites => SitesActor}
import play.api._
import play.api.mvc._
import scala.concurrent.duration._
import scala.concurrent._

/**
  * Controller to produce the "list of sites" page.
  */
@Singleton
class SitesController @Inject() (
    val controllerComponents: ControllerComponents,
    val sitesActor: ActorRef[SitesActor.Command],
    implicit val scheduler: Scheduler,
    implicit val ec: ExecutionContext
) extends BaseController {
  implicit val timeout = Timeout(5.seconds)
  def index() = Action.async { implicit request: Request[AnyContent] =>
    sitesActor
      .ask(SitesActor.GetSites)
      .map { case SitesActor.Reply(siteSeq) => siteSeq }
      .flatMap { Future.fromTry(_) }
      .map { sites =>
        Ok(views.html.sites(sites))
      }
  }
}
