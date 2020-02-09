package org.dmaze.sosearcher.attributes

import java.time.Instant

/**
  * Details of a question that do not refer to other objects.
  * 
  * These generally follow the names and object layout presented in the
  * Stack Exchange API.  This is a subset of details we hope are useful,
  * and does not include user-private details or vote counts.
  * 
  * In the API there are also links to several other objects, which
  * are not recorded here.  These include duplicate questions and the
  * asking user.  At the database level the list of tags is also
  * stored separately and so those are not recorded here.
  */
case class QuestionAttributes(
  closedDate: Option[Instant],
  closedDetails: Option[ClosedDetails],
  creationDate: Instant,
  lastActivityDate: Instant,
  link: String,
  lockedDate: Option[Instant],
  protectedDate: Option[Instant],
  questionId: Long,
  score: Int,
  title: String
)
