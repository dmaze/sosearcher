package org.dmaze.sosearcher.db

import slick.jdbc.SQLiteProfile.api._

/**
  * Database layout for the `sites` table, holding [[Site]] objects.
  */
class Sites(tag: Tag) extends Table[Site](tag, "sites") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def apiSiteParameter = column[String]("api_site_parameter")
  def audience = column[String]("audience")
  def iconUrl = column[String]("icon_url")
  def logoUrl = column[String]("logo_url")
  def name = column[String]("name")
  def siteUrl = column[String]("site_url")
  def active = column[Boolean]("active", O.Default(true))
  def * =
    (id.?, name, siteUrl, apiSiteParameter, audience, iconUrl, logoUrl, active) <> ((Site.fromRow _).tupled, Site.toRow)
}

object Sites {
  val query = TableQuery[Sites]
}
