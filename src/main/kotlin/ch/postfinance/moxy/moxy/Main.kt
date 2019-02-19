package ch.postfinance.moxy.moxy

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.VertxPrometheusOptions

fun main() {

  val vertx = Vertx.vertx(
    VertxOptions().setMetricsOptions(
      MicrometerMetricsOptions()
        .setPrometheusOptions(
          VertxPrometheusOptions().setEnabled(true))
        .setEnabled(true)))

  metricsRegistry.config().commonTags("app", "moxy")

  ClassLoaderMetrics().bindTo(metricsRegistry)
  JvmMemoryMetrics().bindTo(metricsRegistry)
  JvmGcMetrics().bindTo(metricsRegistry)
  ProcessorMetrics().bindTo(metricsRegistry)
  JvmThreadMetrics().bindTo(metricsRegistry)
  UptimeMetrics().bindTo(metricsRegistry)

  val retriever = ConfigRetriever.create(vertx, ConfigRetrieverOptions().setIncludeDefaultStores(true).setScanPeriod(60000))
  retriever.getConfig { json ->
    val result = json.result()
    vertx.deployVerticle(EndpointDeployerVerticle(), DeploymentOptions(JsonObject(mapOf("worker" to true))).setConfig(result))
    vertx.deployVerticle(HttpServerVerticle(), DeploymentOptions(JsonObject(mapOf("worker" to true))).setConfig(result)) {
      if (it.succeeded()) {
        EndpointPersistence.init(vertx, result)
      }
    }
  }

  retriever.listen { change ->
    val json = change.newConfiguration
    vertx.eventBus().publish("new-configuration", json)
  }


}
