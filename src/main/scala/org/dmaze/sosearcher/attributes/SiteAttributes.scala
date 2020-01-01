package org.dmaze.sosearcher.attributes

/**
  * Static attributes for some single Stack Exchange site.
  * @param name Name of the site, e.g. "Stack Overflow"
  * @param siteUrl Base URL of the site, e.g. "https://stackoverflow.com"
  * @param apiSiteParameter Key that appears in API queries to identify
  *   the site, e.g. "stackoverflow".
  * @param audience Sentence fragment describing who the site is for.
  * @param iconUrl URL to a bare icon for the site.
  * @param logoUrl URL to an expanded icon including the site name.
  */
case class SiteAttributes(
    name: String,
    siteUrl: String,
    apiSiteParameter: String,
    audience: String,
    iconUrl: String,
    logoUrl: String
)
