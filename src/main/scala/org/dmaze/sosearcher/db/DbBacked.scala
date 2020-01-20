package org.dmaze.sosearcher.db

/** Common interface for objects with a database identifier. */
trait DbBacked {

  /** Database identifier for this record, or `None` if it is not stored
    * in the database. */
  def databaseId: Option[Int]
}
