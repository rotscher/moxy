package ch.postfinance.moxy.moxy

import io.prometheus.client.Collector
import io.prometheus.jmx.JmxCollector
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import java.io.File
import java.util.*
import java.util.stream.IntStream


class JmxEndpointVerticle(private val nodeName: String, private val configFile: String) : AbstractVerticle() {

  override fun start() {

    var jmxUrl = ""

    vertx.eventBus().consumer<String>(nodeName) { reply ->
      jmxUrl = reply.body()
    }

    val collector = MyJmxCollector(File(configFile))
    vertx.eventBus().publish("metrics-init", JsonObject(mapOf("nodeName" to nodeName)))
    vertx.setPeriodic(15000) { id ->
      if (jmxUrl != "") {
        val buff = Buffer.buffer()
        collector
          .collect()
          .forEach { metricFamily ->
            metricFamily.samples.forEach { sample ->
              buff.appendBytes("${metricFamily.name} ${sample.value}\n".toByteArray())
            }
          }

        val message = JsonObject(mapOf("nodeName" to nodeName, "data" to buff.bytes))
        vertx.eventBus().publish("metrics", message)
      }
    }
  }

  override fun stop() {
    vertx.eventBus().publish("metrics-remove  ", JsonObject(mapOf("nodeName" to nodeName)))
  }

  class MyJmxCollector(configFile: File) : JmxCollector(configFile) {
    var count = 0.0
    override fun collect() : List<Collector.MetricFamilySamples> {

      val start = System.nanoTime()
      val mfsList = ArrayList<MetricFamilySamples>()

      addSample("jmx_scrape_duration_seconds",  (System.nanoTime() - start) / 1.0E9, mfsList)
      addSample("jmx_scrape_count", count++, mfsList)

      IntStream.range(0,100000).forEach { addSample("$it", it.toDouble(), mfsList) }

      return mfsList
    }

    private fun addSample(sampleName: String, value: Double, mfsList: ArrayList<MetricFamilySamples>) {
      val scrapeCount = MetricFamilySamples.Sample(sampleName, listOf<String>(), listOf<String>(), value)
      mfsList.add(MetricFamilySamples(sampleName, Type.GAUGE, "Time this JMX scrape took, in seconds.", listOf(scrapeCount)))
    }
  }
}
