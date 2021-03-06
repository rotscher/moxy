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
      collector = createCollector(reply.body(), "jmxUrl")
      vertx.eventBus().send("persistence.writer.updatenode", JsonObject(mapOf("nodeName" to nodeName, "jmxUrl" to reply.body())))
    }.exceptionHandler {
      incrementErrorCount("jmxUrlConsumer", nodeName)
    }

    vertx.eventBus().consumer<String>("hostPort.$nodeName") { reply ->
      collector = createCollector(reply.body(), "hostPort")
    }.exceptionHandler {
      incrementErrorCount("hostPortConsumer", nodeName)
    }

    vertx.eventBus().consumer<JsonObject>("new-configuration") { reply ->
      val myCollector = collector as MyJmxCollector
      collector = MyJmxCollector(myCollector.yaml, reply.body())
    }

    val initBuff = writeMetricCollection(createInitSample(0.0))
    vertx.eventBus().publish("metrics-init", JsonObject(mapOf("nodeName" to nodeName, "data" to initBuff.bytes)))


    vertx.setPeriodic(config().getLong("scrapeDelay")) {
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

  private fun createCollector(jmxScrapeUrl: String, scrapeType: String): JmxCollector {
    val yaml = Yaml().loadAs(FileReader(configFile), mutableMapOf<Any?, Any?>().javaClass)
    yaml[scrapeType] = jmxScrapeUrl
    vertx.eventBus().send("nodehandler.update", JsonObject(mapOf("nodeName" to nodeName, "url" to jmxScrapeUrl)))
    LOG.fine("got jvm url, node=$nodeName, url=$jmxScrapeUrl")
    return MyJmxCollector(Yaml().dumpAsMap(yaml), config())
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

  inner class MyJmxCollector(val yaml: String, val config: JsonObject) : JmxCollector(yaml) {

    override fun collect() : List<Collector.MetricFamilySamples> {

      val mfsList = super.collect()

      //if enabled, add some fake samples
      val performanceConf = config.getJsonObject("debug").getJsonObject("performance")
      if (performanceConf.getBoolean("enabled")) {
        IntStream.range(mfsList.size, performanceConf.getInteger("fakeMetrics")).forEach { addFakeSample("$it", it.toDouble(), mfsList) }
      }

      val metricsLimit = config().getInteger("metricsLimit")
      if (metricsLimit > -1 && mfsList.size > metricsLimit) {
        //TODO: we only limit the number of metrics, a metric consists of multiple samples. Maybe the samples should be limited
        mfsList.subList(0, metricsLimit)
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
