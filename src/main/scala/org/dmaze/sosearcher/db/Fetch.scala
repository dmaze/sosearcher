package org.dmaze.sosearcher.db

import java.time.LocalDateTime

/**
  * Database record of a single question fetch.
  * 
  * This is generally only used within the fetch machinery.  The
  * `timestamp` and `result` fields record the result of some
  * completed fetch; if both are `None` then the fetch is still
  * outstanding.
  */ 
case class Fetch(
  databaseId: Option[Int],
  postTypeId: Int,
  siteId: Int,
  postNumber: Long,
  fetchTypeId: Int,
  timestamp: Option[LocalDateTime],
  result: Option[String]
)
