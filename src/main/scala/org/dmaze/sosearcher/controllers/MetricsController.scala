package org.dmaze.sosearcher.controllers

import io.micrometer.prometheus.PrometheusMeterRegistry
import javax.inject._
import play.api._
import play.api.mvc._

/**
  * Play controller to present Prometheus metrics.
  *
  * Set a route on `/metrics` to this controller.
  */
@Singleton
class MetricsController @Inject() (
    val controllerComponents: ControllerComponents,
    val registry: PrometheusMeterRegistry
) extends BaseController {
  def index = Action { Ok(registry.scrape()) }
}
