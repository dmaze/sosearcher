package org.dmaze.sosearcher.controllers

import akka.actor.typed.{ActorRef, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import javax.inject._
import org.dmaze.sosearcher.actors.{Sites => SitesActor}
import org.dmaze.sosearcher.models.{Site, SiteList}
import play.api._
import play.api.data._
import play.api.data.format.Formatter
import play.api.data.format.Formats._
import play.api.data.Forms._
import play.api.i18n._
import play.api.mvc._
import scala.concurrent.duration._
import scala.concurrent._

/**
  * Controller to present SO questions.
  */
@Singleton
class QuestionsController @Inject() (
    val controllerComponents: ControllerComponents,
    val sitesActor: ActorRef[SitesActor.Command],
    implicit val scheduler: Scheduler,
    implicit val ec: ExecutionContext
) extends BaseController
    with I18nSupport {
  implicit val timeout = Timeout(5.seconds)

  def urlForm(implicit binder: Formatter[QuestionUrl]) = Form(
    mapping(
      "url" -> of[QuestionUrl]
    )(QuestionUrlData.apply)(QuestionUrlData.unapply)
  )

  def index() = Action { implicit request =>
    implicit val formatter = new QuestionUrlFormatter(
      new SiteList(Seq())
    )
    Ok(views.html.questionForm(urlForm))
  }

  def submit() = Action.async { implicit request =>
    sitesActor
      .ask(SitesActor.GetSites)
      .map { case SitesActor.Reply(sites) => sites }
      .flatMap { Future.fromTry(_) }
      .map { sites =>
        {
          implicit val formatter = new QuestionUrlFormatter(sites)
          urlForm.bindFromRequest.fold(
            formWithErrors => {
              BadRequest(views.html.questionForm(formWithErrors))
            },
            urlData => {
              // We shouldn't get here right now.
              Redirect(routes.QuestionsController.index)
            }
          )
        }
      }
  }
}

/** Form data for the "submit a URL" form. */
case class QuestionUrlData(url: QuestionUrl)

/** A parsed question URL. */
case class QuestionUrl(site: Site, number: Int)

class QuestionUrlFormatter(sites: SiteList) extends Formatter[QuestionUrl] {
  // override val format = ???
  override def bind(key: String, data: Map[String, String]) =
    sites
      .questionUrl(data.get(key).getOrElse(""))
      .map((QuestionUrl.apply _).tupled)
      .toRight(Seq(FormError(key, "form.question.url.invalid")))
      .flatMap(
        q =>
          Left(
            Seq(
              FormError(
                key,
                "form.question.url.valid",
                Seq(q.site.name, q.number)
              )
            )
          )
      ) // this last line is temporary
  override def unbind(key: String, value: QuestionUrl) =
    Map(key -> s"${value.site.siteUrl}/questions/${value.number}")
}
