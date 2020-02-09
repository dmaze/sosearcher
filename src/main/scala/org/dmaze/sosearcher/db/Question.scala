package org.dmaze.sosearcher.db

import org.dmaze.sosearcher.attributes.QuestionAttributes

/**
  * In-database representation of a single question.
  * 
  * This is stored in the [[Questions]] table.
  */
case class Question(
  databaseId: Option[Int],
  siteId: Int,
  attributes: SiteAttributes
)
