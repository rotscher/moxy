package ch.postfinance.moxy.moxy

import ch.postfinance.moxy.moxy.jmxextractor.ForeignUserExtractor
import ch.postfinance.moxy.moxy.jmxextractor.JmxExtractor
import ch.postfinance.moxy.moxy.jmxextractor.NativeUserExtractor
import io.micrometer.core.instrument.Timer
import io.vertx.core.AbstractVerticle
import java.util.logging.Logger
import java.util.stream.IntStream

class JmxUrlRetrieverVerticle(private val nodeName: String, private val jmxUrl: String, private val pid: Int, private val user: String, private val group: String) : AbstractVerticle() {

  private val LOG = Logger.getLogger("moxy.verticle.jmxurlretriever")

  override fun start() {
    val eb = vertx.eventBus()

    if (jmxUrl != "") {
      eb.send("jmxUrl.$nodeName", jmxUrl)
    } else {
      val timer = Timer
              .builder("moxy_jmxretrieval")
              .description("measures the time for retrieving the jmx url from a remote jvm") // optional
              .tags("nodeName", nodeName)
              .register(metricsRegistry)

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
          if (pid <= 1) {
            incrementErrorCount("jmxretrieval_pid", nodeName)
            LOG.warning("pid not valid, pid=$pid, node=$nodeName")
            throw Exception("pid not valid, pid=$pid, node=$nodeName")
          } else {
            val jmxExtractor = jmxExtractorFactory(user, group)
            val jmxUrl = jmxExtractor.extractJmxUrl(pid)
            LOG.fine("retrieved jmx url, url=$jmxUrl")
            eb.send("jmxUrl.$nodeName", jmxUrl)
          }
        }
      }
    }
  }

  private fun jmxExtractorFactory(user: String, group: String): JmxExtractor {
    if (user.isBlank() && group.isBlank()) {
      return NativeUserExtractor()
    }

    return ForeignUserExtractor(user, group)
  }
}
