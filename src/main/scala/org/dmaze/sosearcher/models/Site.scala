package org.dmaze.sosearcher.models

import org.dmaze.sosearcher.attributes.SiteAttributes

/**
  * A single Stack Exchange site.
  *
  * @param attributes Static attributes of the site
  * @param databaseId If this is stored in a database, its unique ID.
  */
case class Site(
    attributes: SiteAttributes,
    databaseId: Option[Int] = None
)

object Site {
  import scala.language.implicitConversions
  implicit def toAttributes(s: Site): SiteAttributes = s.attributes
}
