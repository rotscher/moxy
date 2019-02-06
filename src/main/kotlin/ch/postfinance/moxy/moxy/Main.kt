package ch.postfinance.moxy.moxy

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.VertxPrometheusOptions
import io.vertx.micrometer.backends.BackendRegistries

fun main() {


  val vertx = Vertx.vertx(
    VertxOptions().setMetricsOptions(
      MicrometerMetricsOptions()
        .setPrometheusOptions(
          VertxPrometheusOptions().setEnabled(true))
        .setEnabled(true)))

  val registry = BackendRegistries.getDefaultNow() as PrometheusMeterRegistry
  ClassLoaderMetrics().bindTo(registry)
  JvmMemoryMetrics().bindTo(registry)
  JvmGcMetrics().bindTo(registry)
  ProcessorMetrics().bindTo(registry)
  JvmThreadMetrics().bindTo(registry)

  val httpServer = HttpServerVerticle()
  vertx.deployVerticle(httpServer, DeploymentOptions(JsonObject(mapOf("worker" to true))))
  vertx.deployVerticle(EndpointDeployerVerticle(), DeploymentOptions(JsonObject(mapOf("worker" to true))))
  EndpointPersistence.init(vertx)
}
