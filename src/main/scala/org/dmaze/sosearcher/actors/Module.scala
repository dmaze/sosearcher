package org.dmaze.sosearcher.actors

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

/**
  * Guice module to supply bindings for actors.
  *
  * This registers all of the actors in this package with Guice, and
  * makes types like `ActorRef[Sites.Command]` available for
  * dependency injection.
  *
  * This needs to be referenced in the Play configuration but does
  * not generally need to be used in application code.
  */
class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bindTypedActor(Sites, "sites-actor")
    bindTypedActor(SitesAPI, "sites-api-actor")
    bindTypedActor(SitesDB, "sites-db-actor")
  }
}
