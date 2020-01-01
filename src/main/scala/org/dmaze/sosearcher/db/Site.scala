package org.dmaze.sosearcher.db

import org.dmaze.sosearcher.attributes.SiteAttributes
import org.dmaze.sosearcher.models.{Site => ModelSite}

/**
  * In-database representation of a single Stack Exchange site.
  *
  * This is stored in the [[Sites]] table.
  *
  * [[active]] is used as a marker to allow sites to be removed from
  * the Stack Exchange API, without purging their data from the
  * database.  If a `/sites` query does not return a site we believe
  * to exist then it is marked inactive; if it reappears then it
  * becomes marked active and all of its saved data is available
  * again.
  */
case class Site(
    databaseId: Option[Int],
    attributes: SiteAttributes,
    active: Boolean
) {
  def toModel: ModelSite = ModelSite(attributes, databaseId)
}

object Site {
  def apply(model: ModelSite): Site =
    Site(model.databaseId, model.attributes, true)
  def fromRow(
      databaseId: Option[Int],
      name: String,
      siteUrl: String,
      apiSiteParameter: String,
      audience: String,
      iconUrl: String,
      logoUrl: String,
      active: Boolean
  ): Site =
    Site(
      databaseId,
      SiteAttributes(
        name,
        siteUrl,
        apiSiteParameter,
        audience,
        iconUrl,
        logoUrl
      ),
      active
    )
  def toRow(s: Site): Option[
    (Option[Int], String, String, String, String, String, String, Boolean)
  ] =
    Some(
      (
        s.databaseId,
        s.attributes.name,
        s.attributes.siteUrl,
        s.attributes.apiSiteParameter,
        s.attributes.audience,
        s.attributes.iconUrl,
        s.attributes.logoUrl,
        s.active
      )
    )
}
