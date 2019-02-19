package ch.postfinance.moxy.moxy

import ch.postfinance.moxy.moxy.jmxextractor.ForeignUserExtractor
import ch.postfinance.moxy.moxy.jmxextractor.JmxExtractor
import ch.postfinance.moxy.moxy.jmxextractor.NativeUserExtractor
import io.micrometer.core.instrument.Timer
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
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

        val jmxRetrievalConf = config().getJsonObject("debug").getJsonObject("jmxRetrieval")
        val delayConf = jmxRetrievalConf.getJsonObject("delay")
        if (delayConf.getBoolean("enabled")) {
          IntStream.range(0, delayConf.getInteger("count")).forEach { Thread.sleep(1000) }
        }

        jmxRetrievalConf.getBoolean("static")
        if (jmxRetrievalConf.getBoolean("static")) {
          when {
            jmxRetrievalConf.getString("jmxUrl") != null -> eb.send("jmxUrl.$nodeName", jmxRetrievalConf.getString("jmxUrl"))
              jmxRetrievalConf.getString("hostPort") != null -> eb.send("hostPort.$nodeName", jmxRetrievalConf.getString("hostPort"))
            else -> throw Exception("either jmxUrl or hostPort must be defined ")
          }
        } else {
          if (pid <= 1) {
            incrementErrorCount("jmxretrieval_pid", nodeName)
            LOG.warning("pid not valid, pid=$pid, node=$nodeName")
            throw Exception("pid not valid, pid=$pid, node=$nodeName")
          } else {
            val jmxExtractor = jmxExtractorFactory(user, group, config())
            val jmxUrl = jmxExtractor.extractJmxUrl(pid)
            LOG.fine("retrieved jmx url, url=$jmxUrl")
            eb.send("jmxUrl.$nodeName", jmxUrl)
          }
        }
      }
    }
  }

  private fun jmxExtractorFactory(user: String, group: String, config: JsonObject): JmxExtractor {
    if (user.isBlank() && group.isBlank()) {
      return NativeUserExtractor()
    }

    return ForeignUserExtractor(user, group, config)
  }
}
