package org.dmaze.sosearcher.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{__, Reads}
import scala.util.{Try, Success, Failure}

/**
  * Generic wrapper around any SE API response.
  * 
  * @param backoff If provided, wait this many seconds before making
  *   another response.
  * @param errorId If provided, an integer error code.
  * @param errorMessage If provided, a human-readable error message.
  * @param errorName If provided, a string error code.
  * @param hasMore If true, there are more items after this batch.
  * @param items The actual items being requested.
  */
case class ApiWrapper[T](
  backoff: Option[Int],
  errorId: Option[Int],
  errorMessage: Option[String],
  errorName: Option[String],
  hasMore: Boolean,
  items: Seq[T]
) {
  /**
    * Get the items out of this wrapper, or an exception.
    * 
    * If any of the error metadata is set, returns a failure with an
    * {ApiError}; otherwise returns a success with the embedded items.
    */
  def getItems: Try[Seq[T]] = {
    if (errorId.nonEmpty || errorMessage.nonEmpty || errorName.nonEmpty) {
      val name = errorName.getOrElse("unknown")
      Failure(new ApiError(
        errorMessage.getOrElse(name),
        errorId.getOrElse(999),
        name
      ))
    } else {
      Success(items)
    }
  }
}

object ApiWrapper {
  /**
    * A filter name that includes only these fields.
    * 
    * It is unlikely to use this directly (it will exclude every
    * value from the "items" sequence) but it is useful as a base
    * for other filters.
    */
  val filter: String = ".s-Mpdf3"

  /**
    * Construct a JSON reader for some particular wrapped type.
    * 
    * This must be done once per type, but these objects may be
    * safely reused once created.
    */
  implicit def readsApiWrapper[T](implicit readT: Reads[T]): Reads[ApiWrapper[T]] =
    ( (__ \ "backoff").readNullable[Int] and
      (__ \ "error_id").readNullable[Int] and
      (__ \ "error_message").readNullable[String] and
      (__ \ "error_name").readNullable[String] and
      (__ \ "has_more").read[Boolean] and
      (__ \ "items").read[Seq[T]]
    )(ApiWrapper.apply[T] _)
}
