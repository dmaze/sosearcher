package org.dmaze.sosearcher

import com.google.inject.{AbstractModule, Provider, Provides}
import io.kontainers.micrometer.akka.AkkaMetricRegistry
import io.micrometer.core.instrument.{Clock, MeterRegistry}
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.{ClassLoaderMetrics, JvmGcMetrics, JvmMemoryMetrics, JvmThreadMetrics}
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.jmx.{JmxConfig, JmxMeterRegistry}
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import io.prometheus.client.CollectorRegistry
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}

/**
  * Core Guice module for system-level setup.
  * 
  * This in particular provides a Micrometer metrics registry.
  */
class Module extends AbstractModule with ScalaModule {
  override def configure() = {
    bind[MeterRegistry].toProvider[Module.MeterRegistryProvider].asEagerSingleton

    val registries = ScalaMultibinder.newSetBinder[MeterRegistry](binder)
    registries.addBinding.to[JmxMeterRegistry]
    registries.addBinding.to[PrometheusMeterRegistry]

    val binders = ScalaMultibinder.newSetBinder[MeterBinder](binder)
    binders.addBinding.to[ClassLoaderMetrics]
    binders.addBinding.to[JvmGcMetrics]
    binders.addBinding.to[JvmMemoryMetrics]
    binders.addBinding.to[JvmThreadMetrics]
  }

  // We can't tag a Guice "provides" method as an eager singleton.
  // But we can create a provider object that we can explicitly bind() to.
  // Strange world.

  @Provides @Singleton def provideJmxMeterRegistry: JmxMeterRegistry =
    new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM)

  @Provides @Singleton def providePrometheusMeterRegistry: PrometheusMeterRegistry =
    new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}

object Module {
  @Singleton class MeterRegistryProvider @Inject() (registries: Set[MeterRegistry], binders: Set[MeterBinder]) extends Provider[MeterRegistry] {
    override def get: MeterRegistry = {
      val registry = new CompositeMeterRegistry
      registries.foreach { registry.add(_) }
      binders.foreach { _.bindTo(registry) }
      AkkaMetricRegistry.setRegistry(registry)
      System.out.println(s"MeterRegistryProvider: ${registries.size} registries, ${binders.size} binders")
      registry
    }
  }

}
