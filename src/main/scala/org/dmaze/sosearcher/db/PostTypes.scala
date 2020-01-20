package org.dmaze.sosearcher.db

import slick.jdbc.SQLiteProfile.api._

/**
  * Database layout for the `post_types` table, holding [[PostType]] objects.
  * 
  * These will generally be a fixed list inserted only via database
  * migrations.  This will also be a fairly small list so it's
  * reasonable to fetch the entire list at startup time.
  */
class PostTypes(tag: Tag) extends Table[PostType](tag, "post_types") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def postType = column[String]("post_type", O.Unique)
  def * = (id.?, postType) <> ((PostType.apply _).tupled, PostType.unapply)
}

object PostTypes {
  val query = TableQuery[PostTypes]
}

