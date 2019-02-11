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
import java.util.stream.IntStream


class JmxEndpointVerticle(private val nodeName: String, private val configFile: String) : AbstractVerticle() {

  override fun start() {

    var collector: JmxCollector? = null

    vertx.eventBus().consumer<String>("jmxUrl.${nodeName}") { reply ->
      collector = handleUrlRetrieval(reply.body(), "jmxUrl")
    }

    vertx.eventBus().consumer<String>("hostPort.${nodeName}") { reply ->
      collector = handleUrlRetrieval(reply.body(), "hostPort")
    }

    vertx.eventBus().publish("metrics-init", JsonObject(mapOf("nodeName" to nodeName)))
    vertx.setPeriodic(15000) {
      if (collector != null) {
        val myCollector = collector
        val mfsList = myCollector?.collect()
        val vector = Vector(mfsList).elements()

        val bufWriter = StringWriter()
        TextFormat.write004(bufWriter, vector)

        val buff = Buffer.buffer()
        buff.appendBytes(bufWriter.toString().toByteArray())

        val message = JsonObject(mapOf("nodeName" to nodeName, "data" to buff.bytes))
        vertx.eventBus().publish("metrics", message)
      }
    }
  }

  private fun handleUrlRetrieval(jmxScrapeUrl: String, scapeType: String): JmxCollector {
    val yaml = Yaml().loadAs(FileReader(configFile), mutableMapOf<Any?, Any?>().javaClass)
    yaml[scapeType] = jmxScrapeUrl
    vertx.eventBus().send("nodehandler.update", JsonObject(mapOf("nodeName" to nodeName, "url" to jmxScrapeUrl)))
    return MyJmxCollector(Yaml().dumpAsMap(yaml))
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
