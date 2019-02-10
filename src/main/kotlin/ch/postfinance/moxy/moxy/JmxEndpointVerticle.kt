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

    vertx.eventBus().consumer<String>(nodeName) { reply ->
      val jmxUrl = reply.body()
      val yaml = Yaml().load(FileReader(configFile))
      val yaml2 = (yaml as (Map<*, *>)).toMutableMap()
      yaml2.put("jmxUrl", jmxUrl)
      collector = MyJmxCollector(Yaml().dumpAsMap(yaml2))

      vertx.eventBus().send("nodehandler.update", JsonObject(mapOf("nodeName" to nodeName, "jmxUrl" to jmxUrl)))
    }

    vertx.eventBus().publish("metrics-init", JsonObject(mapOf("nodeName" to nodeName)))
    vertx.setPeriodic(15000) { id ->
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

  override fun stop() {
    vertx.eventBus().publish("metrics-remove  ", JsonObject(mapOf("nodeName" to nodeName)))
  }

  class MyJmxCollector(yaml: String) : JmxCollector(yaml) {

    override fun collect() : List<Collector.MetricFamilySamples> {

      val start = System.nanoTime()
      val mfsList = super.collect()

      //Todo: make this configurable
      IntStream.range(0,100000).forEach { addSample("$it", it.toDouble(), mfsList) }

      //TODO: limit number of metrics
      return mfsList
    }

    private fun addSample(sampleName: String, value: Double, mfsList: MutableList<MetricFamilySamples>) {
      val labels = mutableListOf("context", "host")
      val values = mutableListOf("host-${sampleName}", "localhost")
      val metricName = "tomcat_session_sessioncounter_total"
      val scrapeCount = MetricFamilySamples.Sample(metricName, labels, values, value)
      mfsList.add(MetricFamilySamples(sampleName, Type.GAUGE, "Time this JMX scrape took, in seconds.", listOf(scrapeCount)))
    }
  }
}
