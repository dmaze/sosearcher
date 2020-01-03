package org.dmaze.sosearcher.models

import java.net.URI

/**
  * A catalog of all known Stack Exchange sites.
  */
class SiteList(val sites: Seq[Site]) {

  /** Index of site API key to site object. */
  val byParam: Map[String, Site] =
    Map.from(sites.map(s => (s.apiSiteParameter, s)))

  /** Index of base site URL to site object. */
  val byUrl: Map[String, Site] = Map.from(sites.map(s => (s.siteUrl, s)))

  /**
    * Attempt to parse a question URL.
    *
    * A typical URL will be of the form
    * `https://stackoverflow.com/questions/59557016/anything-at-all`.
    * In this the prefix should match one of the site URLs,
    * "questions" is a literal, the next path part is the question ID,
    * and the suffix can be literally any string.
    *
    * On success returns a pair of the matching site object and
    * question ID.
    */
  def questionUrl(url: String): Option[(Site, Int)] = {
    // Implicitly assume all of the URLs are of the form
    // https://stackoverflow.com with no URL path component at are.
    // (Currently that seems to be the case.)
    val uri = new URI(url)
    for {
      // Unpack the URL; the java.net.URI API returns null if
      // various parts are missing.
      scheme <- Option(uri.getScheme)
      _ <- Option.when(uri.getUserInfo() == null)(())
      host <- Option(uri.getHost)
      _ <- Option.when(uri.getPort == -1)(())
      path <- Option(uri.getPath)
      // ignore query, fragment parts

      // Get the site based on the base URL
      baseUrl = s"${scheme}://${host}"
      site <- byUrl.get(baseUrl)

      // Get the question ID
      re = raw"/questions/(\d+)(/.*)?".r
      matched <- re.findFirstMatchIn(path)
      number <- matched.group(1).toIntOption
    } yield (site, number)
  }
}
