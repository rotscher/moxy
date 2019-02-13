package ch.postfinance.moxy.moxy

import io.prometheus.client.Collector
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.jmx.JmxCollector
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import org.yaml.snakeyaml.Yaml
import java.io.FileReader
import java.io.StringWriter
import java.util.*
import java.util.logging.Logger
import java.util.stream.IntStream


class JmxEndpointVerticle(private val nodeName: String, private val configFile: String) : AbstractVerticle() {

  private val LOG = Logger.getLogger("moxy.verticle.endpoint.jmx")

  override fun start() {

    var collector: JmxCollector? = null

    vertx.eventBus().consumer<String>("jmxUrl.$nodeName") { reply ->
      collector = handleUrlRetrieval(reply.body(), "jmxUrl")
      EndpointPersistence.updateJmxUrl(nodeName, reply.body(), vertx)?.setHandler {
        if (!it.succeeded()) {
          incrementErrorCount("jmxUrlConsumer_persistence", nodeName)
        }
      }
    }.exceptionHandler {
      incrementErrorCount("jmxUrlConsumer", nodeName)
    }

    vertx.eventBus().consumer<String>("hostPort.$nodeName") { reply ->
      collector = handleUrlRetrieval(reply.body(), "hostPort")
    }.exceptionHandler {
      incrementErrorCount("hostPortConsumer", nodeName)
    }

    val initBuff = writeMetricCollection(createInitSample(0.0))
    vertx.eventBus().publish("metrics-init", JsonObject(mapOf("nodeName" to nodeName, "data" to initBuff.bytes)))


    vertx.setPeriodic(MoxyConfiguration.configuration.scrapeDelay) {
      if (collector != null) {
        val myCollector = collector
        val mfsList = myCollector?.collect()

        if (mfsList != null) {
          if (hasScrapeError(mfsList)) {
            incrementErrorCount("scrapeError", nodeName)
          }

          mfsList.addAll(createInitSample(1.0))
          val buff = writeMetricCollection(mfsList)

          val message = JsonObject(mapOf("nodeName" to nodeName, "data" to buff.bytes))
          vertx.eventBus().publish("metrics", message)

        }
      }
    }
  }

  private fun writeMetricCollection(mfsList: List<Collector.MetricFamilySamples>): Buffer {
    val vector = Vector(mfsList).elements()
    val bufWriter = StringWriter()
    TextFormat.write004(bufWriter, vector)
    return Buffer.buffer(bufWriter.toString().toByteArray())
  }

  fun hasScrapeError(metricsList: List<Collector.MetricFamilySamples>): Boolean {

    val samples = metricsList.parallelStream().filter{ it.name == "jmx_scrape_error"}.flatMap { it.samples.stream() }
    return samples.anyMatch{it.name == "jmx_scrape_error" && it.value > 0}
  }

  private fun handleUrlRetrieval(jmxScrapeUrl: String, scrapeType: String): JmxCollector {
    val yaml = Yaml().loadAs(FileReader(configFile), mutableMapOf<Any?, Any?>().javaClass)
    yaml[scrapeType] = jmxScrapeUrl
    vertx.eventBus().send("nodehandler.update", JsonObject(mapOf("nodeName" to nodeName, "url" to jmxScrapeUrl)))
    LOG.fine("got jvm url, node=$nodeName, url=$jmxScrapeUrl")
    return MyJmxCollector(Yaml().dumpAsMap(yaml))
  }

  private fun createInitSample(initStatus: Double): List<Collector.MetricFamilySamples> {
    return listOf(
      Collector.MetricFamilySamples("moxy_node_init", Collector.Type.GAUGE, "gauge ",
            /* samples */ listOf(Collector.MetricFamilySamples.Sample("moxy_node_init", listOf("nodeName"), listOf(nodeName), initStatus)))
    )
  }

  override fun stop() {
    vertx.eventBus().publish("metrics-remove", JsonObject(mapOf("nodeName" to nodeName)))
  }

  class MyJmxCollector(yaml: String) : JmxCollector(yaml) {

    override fun collect() : List<Collector.MetricFamilySamples> {

      val mfsList = super.collect()

      //if enabled, add some fake samples
      if (MoxyConfiguration.configuration.debug.performance.enabled) {
        IntStream.range(mfsList.size, MoxyConfiguration.configuration.debug.performance.fakeMetrics).forEach { addFakeSample("$it", it.toDouble(), mfsList) }
      }

      if (MoxyConfiguration.configuration.metricsLimit > -1) {
        return mfsList.subList(0, MoxyConfiguration.configuration.metricsLimit)
      }

      return mfsList
    }

    private fun addFakeSample(sampleName: String, value: Double, mfsList: MutableList<MetricFamilySamples>) {
      val labels = mutableListOf("context", "host")
      val values = mutableListOf("host-$sampleName", "localhost")
      val metricName = "tomcat_session_sessioncounter_total"
      val scrapeCount = MetricFamilySamples.Sample(metricName, labels, values, value)
      mfsList.add(MetricFamilySamples(sampleName, Type.GAUGE, "fake metric $sampleName", listOf(scrapeCount)))
    }
  }
}
