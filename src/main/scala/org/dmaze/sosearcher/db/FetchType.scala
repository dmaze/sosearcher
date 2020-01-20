package org.dmaze.sosearcher.db

import scala.util.{Failure, Success, Try}

/**
  * A single fetch type.
  */
case class FetchType(
  databaseId: Option[Int],
  fetchType: String
) extends DbBacked

object FetchType {
  /** Fetch type value for question metadata. */
  val metadata = "metadata"

  /** Fetch type value for the actual question body. */
  val body = "body"
}
