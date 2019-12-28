package org.dmaze.sosearcher.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{__, Reads}

/**
  * Representation of some single Site.
  *
  * This is returned from the `/sites` endpoint.
  *
  * @param name Name of the site, e.g. "Stack Overflow".
  * @param siteUrl Base URL of the site, e.g. "https://stackoverflow.com".
  * @param apiSiteParameter Key that appears in API queries to
  *   identify the site, e.g. "stackoverflow".
  * @param audience Sentence-long description of the site.
  * @param iconUrl URL to a bare icon for the site.
  * @param logoUrl URL to an expanded icon including the site name.
  * @param databaseId If this is stored in a database, its unique ID.
  * @param active If false, this is a record in the database for a
  *   site that the Stack Exchange API is no longer advertising.
  */
case class Site(
    name: String,
    siteUrl: String,
    apiSiteParameter: String,
    audience: String,
    iconUrl: String,
    logoUrl: String,
    databaseId: Option[Int] = None,
    active: Boolean = true
)

object Site extends SeApi {

  /** The API URL for the site listing. */
  val url: String = s"${baseURL}sites"

  /** A filter name including the requested fields. */
  val filter: String = "*Ids4-aVXHOlLe_U(8S("

  /** A JSON reader for a site object. */
  implicit val readsSite: Reads[Site] =
    ((__ \ "name").read[String] and
      (__ \ "site_url").read[String] and
      (__ \ "api_site_parameter").read[String] and
      (__ \ "audience").read[String] and
      (__ \ "icon_url").read[String] and
      (__ \ "logo_url").read[String] and
      Reads.pure(None) and
      Reads.pure(true))(Site.apply _)
}
