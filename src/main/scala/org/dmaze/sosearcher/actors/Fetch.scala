package org.dmaze.sosearcher.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import com.google.inject.Provides
import java.util.concurrent.TimeUnit.SECONDS
import org.dmaze.sosearcher.db.{FetchType, PostType}
import org.dmaze.sosearcher.models.Site
import play.api.libs.concurrent.ActorModule
import scala.util.{Failure, Success, Try}

/**
  * Actor to fetch questions.
  */
object Fetch extends ActorModule {

  /** A command that can be sent to this actor. */
  sealed trait Command
  type Message = Command

  /**
    * Command to request fetching a question.
    *
    * If a `replyTo` target is given, it is signaled at the point
    * where we have committed to fetching the question; that is, we
    * have recorded that the question will be fetched and triggered
    * the fetching mechanism.  If the system fails and restarts we
    * will still fetch the question.  There is no signal when the
    * fetch has actually been completed.
    */
  final case class Question(
      site: Site,
      number: Long,
      priority: Priority,
      replyTo: Option[ActorRef[QuestionReply]]
  ) extends Command

  /** Command when a child actor has signaled that it has started. */
  private final case class QuestionStarted(
      reply: Try[(Site, Long)],
      replyTo: Option[ActorRef[QuestionReply]]
  ) extends Command

  /**
    * Reply to a [[Question]] command.
    *
    * Signaled at the point where we have committed to fetching the
    * question: we have recorded the fetch request in persistent
    * storage in a way that we can repeat it on a restart.  This can
    * also be signaled with failure if we do not get that far.
    */
  final case class QuestionReply(reply: Try[(Site, Long)])

  implicit val timeout = new Timeout(5, SECONDS)

  @Provides
  def apply(db: ActorRef[FetchDB.Command]): Behavior[Command] =
    Behaviors.receive { (context, command) =>
      command match {
        case Question(site, number, priority, replyTo) => {
          context.ask(
            db,
            FetchDB.Request(
              site,
              number,
              PostType(None, PostType.question),
              FetchType(None, FetchType.metadata),
              _
            )
          ) {
            case Success(FetchDB.PleaseFetch(f)) =>
              QuestionStarted(Success((site, number)), replyTo)
            case Success(FetchDB.AlreadyFetched(f)) =>
              QuestionStarted(Success((site, number)), replyTo)
            case Success(FetchDB.CannotFetch(t)) =>
              QuestionStarted(Failure(t), replyTo)
            case Failure(t) => QuestionStarted(Failure(t), replyTo)
          }
          Behaviors.same
        }

        case QuestionStarted(reply, replyTo) => {
          replyTo.foreach { _ ! QuestionReply(reply) }
          Behaviors.same
        }
      }
    }
}
