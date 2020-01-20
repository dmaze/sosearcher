package org.dmaze.sosearcher.db

import java.time.LocalDateTime
import slick.jdbc.SQLiteProfile.api._

/**
  * Database layout for the `fetches` table, holding [[Fetch]] objects.
  */
class Fetches(tag: Tag) extends Table[Fetch](tag, "fetches") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def postTypeId = column[Int]("post_type_id")
  def siteId = column[Int]("site_id")
  def postNumber = column[Long]("post_number")
  def fetchTypeId = column[Int]("fetch_type_id")
  def timestamp = column[Option[LocalDateTime]]("timestamp")
  def result = column[Option[String]]("result")
  def * =
    (id.?, postTypeId, siteId, postNumber, fetchTypeId, timestamp, result) <> ((Fetch.apply _).tupled, (Fetch.unapply _))

  def postType =
    foreignKey("post_type_fk", postTypeId, PostTypes.query)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )
  def fetchType =
    foreignKey("fetch_type_fk", fetchTypeId, FetchTypes.query)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )
  def site =
    foreignKey("site_fk", siteId, Sites.query)(
      _.id,
      onUpdate = ForeignKeyAction.Restrict,
      onDelete = ForeignKeyAction.Cascade
    )
}

object Fetches {
  val query = TableQuery[Fetches]
}
