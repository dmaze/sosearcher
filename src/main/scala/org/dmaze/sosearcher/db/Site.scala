package org.dmaze.sosearcher.db

import org.dmaze.sosearcher.models.{Site => SiteModel}
import slick.jdbc.SQLiteProfile.api._

/**
  * Database layout for the `site` table, holding
  * [[org.dmaze.sosearcher.models.Site]] objects.
  */
class Site(tag: Tag) extends Table[SiteModel](tag, "site") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def apiSiteParameter = column[String]("api_site_parameter")
  def audience = column[String]("audience")
  def iconUrl = column[String]("icon_url")
  def logoUrl = column[String]("logo_url")
  def name = column[String]("name")
  def siteUrl = column[String]("site_url")
  def active = column[Boolean]("active", O.Default(true))
  def * = (name, siteUrl, apiSiteParameter, audience, iconUrl, logoUrl, id.?, active) <> ((SiteModel.apply _).tupled, SiteModel.unapply)
}
