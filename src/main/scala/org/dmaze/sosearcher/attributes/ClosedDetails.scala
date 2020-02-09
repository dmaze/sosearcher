package org.dmaze.sosearcher.attributes

/**
  * Reason a question was closed.
  * 
  * This is an optional part of [[QuestionAttributes]].  In the Stack
  * Exchange API, this is a separate object that could also appear as
  * part of a flag.
  */
case class ClosedDetails(
  description: String,
  reason: String
)
