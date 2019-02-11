package ch.postfinance.moxy.moxy

import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.micrometer.backends.BackendRegistries
import java.util.stream.IntStream

class JmxUrlRetrieverVerticle(private val nodeName: String, private val jmxUrl: String, private val user: String, private val group: String) : AbstractVerticle() {

  override fun start() {
    val eb = vertx.eventBus()

    if (jmxUrl != "") {
      eb.send(nodeName, jmxUrl)
    } else {
      val registry = BackendRegistries.getDefaultNow() as PrometheusMeterRegistry
      val timer = Timer
              .builder("moxy_jmxretrieval")
              .description("measures the time for retrieving the jmx url from a remote jvm") // optional
              .tags("nodeName", nodeName)
              .register(registry)

      timer.record {
        if (MoxyConfiguration.configuration.debug.jmxRetrieval.delay.enabled) {
          IntStream.range(0, MoxyConfiguration.configuration.debug.jmxRetrieval.delay.count).forEach { Thread.sleep(1000) }
        }

        if (MoxyConfiguration.configuration.debug.jmxRetrieval.static) {
          when {
            MoxyConfiguration.configuration.debug.jmxRetrieval.jmxUrl != null -> eb.send("jmxUrl.$nodeName", MoxyConfiguration.configuration.debug.jmxRetrieval.jmxUrl)
            MoxyConfiguration.configuration.debug.jmxRetrieval.hostPort != null -> eb.send("hostPort.$nodeName", MoxyConfiguration.configuration.debug.jmxRetrieval.hostPort)
            else -> throw Exception("either jmxUrl or hostPort must be defined ")
          }
        } else {
          //TODO: get jmx url
        }
      }
    }

  }
}
