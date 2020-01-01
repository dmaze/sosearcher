package org.dmaze.sosearcher.seapi

import org.dmaze.sosearcher.attributes.SiteAttributes
import org.dmaze.sosearcher.models.{Site => ModelSite}
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
  */
case class Site(
    attributes: SiteAttributes
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
      (__ \ "logo_url").read[String])(SiteAttributes.apply _).map {
      Site.apply _
    }
}
