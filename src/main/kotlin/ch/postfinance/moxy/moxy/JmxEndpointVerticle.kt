package ch.postfinance.moxy.moxy

import io.prometheus.client.Collector
import io.prometheus.client.CollectorRegistry
import io.prometheus.jmx.JmxCollector
import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import org.yaml.snakeyaml.Yaml
import java.io.FileReader


class JmxEndpointVerticle(private val nodeName: String, private val configFile: String) : AbstractVerticle() {

  override fun start() {

    //var jmxUrl = ""
    var collector: JmxCollector? = null

    vertx.eventBus().consumer<String>(nodeName) { reply ->
      val jmxUrl = reply.body()
      val yaml = Yaml().load(FileReader(configFile))
      val yaml2 = (yaml as (Map<*, *>)).toMutableMap()
      yaml2.put("jmxUrl", jmxUrl)
      collector = MyJmxCollector(Yaml().dumpAsMap(yaml2))

    }

    vertx.eventBus().publish("metrics-init", JsonObject(mapOf("nodeName" to nodeName)))
    vertx.setPeriodic(15000) { id ->
      if (collector != null) {
        val myCollector = collector
        val buff = Buffer.buffer()
        myCollector?.collect()
          ?.forEach { metricFamily ->
            System.out.println(metricFamily)
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

  class MyJmxCollector(yaml: String) : JmxCollector(yaml) {
    var count = 0.0
    override fun collect() : List<Collector.MetricFamilySamples> {

      val start = System.nanoTime()
      val mfsList = super.collect()
      //val mfsList = ArrayList<MetricFamilySamples>()

      addSample("jmx_scrape_duration_seconds",  (System.nanoTime() - start) / 1.0E9, mfsList)

      //IntStream.range(0,100000).forEach { addSample("$it", it.toDouble(), mfsList) }

      return mfsList
    }

    private fun addSample(sampleName: String, value: Double, mfsList: MutableList<MetricFamilySamples>) {
      val scrapeCount = MetricFamilySamples.Sample(sampleName, listOf<String>(), listOf<String>(), value)
      mfsList.add(MetricFamilySamples(sampleName, Type.GAUGE, "Time this JMX scrape took, in seconds.", listOf(scrapeCount)))
    }

      /*
      Callable<List<Collector.MetricFamilySamples>> callable = () -> NodeVerticleSender.this.collector.collect(jmxUrl);
        Future<List<Collector.MetricFamilySamples>> future = executorService.submit(callable);
        try {
            List<Collector.MetricFamilySamples> metricFamilySamples = future.get(this.getSampleInterval(), TimeUnit.SECONDS);
            LOG.fine("finished collect on thread, nodeKey=" + this.getNodeKey() + ", name=" + Thread.currentThread().getName() + ", id=" + Thread.currentThread().getId() + ", jmx-url=" + jmxUrl);
            return metricFamilySamples;
        } catch (TimeoutException e) {
            LOG.warning("timeout while scraping metrics, nodeKey=" + this.getNodeKey());
            future.cancel(true);
        } catch (InterruptedException | ExecutionException e) {
            LOG.severe("error while scraping metrics, " + e.getMessage());
        }

       */
  }
}
