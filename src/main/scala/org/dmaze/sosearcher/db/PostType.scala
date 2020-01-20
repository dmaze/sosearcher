package org.dmaze.sosearcher.db

/**
  * A single post type.
  */
case class PostType(
    databaseId: Option[Int],
    postType: String
) extends DbBacked

object PostType {

  /** Post type value for questions. */
  val question = "question"
}
