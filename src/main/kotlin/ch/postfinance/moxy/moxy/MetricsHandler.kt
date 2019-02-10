package ch.postfinance.moxy.moxy

import ch.postfinance.moxy.moxy.metrics.MetricsDataSizeGauge
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.micrometer.backends.BackendRegistries

class MetricsHandler(vertx: Vertx) {

  private val dataMap = mutableMapOf<String, Buffer>()
  private val gaugeMap = mutableMapOf<String, MetricsDataSizeGauge>()

  init {
    val eb = vertx.eventBus()

    eb.consumer<Any>("metrics-init") { message ->
      val messageBody = message.body() as JsonObject
      val nodeName = messageBody.getString("nodeName")
      dataMap[nodeName] = Buffer.buffer()
      gaugeMap[nodeName] = MetricsDataSizeGauge(nodeName)
    }

    eb.consumer<Any>("metrics-remove") { message ->
      val messageBody = message.body() as JsonObject
      val nodeName = messageBody.getString("nodeName")
      dataMap.remove(nodeName)
      gaugeMap.remove(nodeName)
    }

    eb.consumer<Any>("metrics") { message ->
      val messageBody = message.body() as JsonObject
      val nodeName = messageBody.getString("nodeName")
      val buf = Buffer.buffer(messageBody.getBinary("data"))
      dataMap[nodeName] = buf
      gaugeMap[nodeName]?.setValue(buf.bytes.size.toDouble())
    }
  }

  fun getMetrics(routingContext: RoutingContext) {

    val nodeName = routingContext.request().getParam("nodeName")

    val response = routingContext.response()
    response.isChunked = true

    val metricData = dataMap[nodeName]
    if (metricData == null) {
      response.statusCode = 404
      response.end()
      return
    }

    if (metricData.length() == 0) {
      //send no content back
      response.statusCode = 204
    }
    response.end(metricData)
  }
}
