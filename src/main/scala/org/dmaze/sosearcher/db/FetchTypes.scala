package org.dmaze.sosearcher.db

import slick.jdbc.SQLiteProfile.api._

/**
  * Database layout for the `fetch_types` table, holding [[FetchType]] objects.
  * 
  * These will generally be a fixed list inserted only via database
  * migrations.  This will also be a fairly small list so it's
  * reasonable to fetch the entire list at startup time.
  */
class FetchTypes(tag: Tag) extends Table[FetchType](tag, "fetch_types") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def fetchType = column[String]("fetch_type", O.Unique)
  def * = (id.?, fetchType) <> ((FetchType.apply _).tupled, (FetchType.unapply _))
}

object FetchTypes {
  val query = TableQuery[FetchTypes]
}
