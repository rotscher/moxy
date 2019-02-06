package ch.postfinance.moxy.moxy

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

class MetricsHandler(vertx: Vertx) {

  private val dataMap = mutableMapOf<String, Buffer>()

  init {
    val eb = vertx.eventBus()

    eb.consumer<Any>("metrics-init") { message ->
      val messageBody = message.body() as JsonObject
      dataMap[messageBody.getString("nodename")] = Buffer.buffer()
    }

    eb.consumer<Any>("metrics-remove") { message ->
      val messageBody = message.body() as JsonObject
      dataMap.remove(messageBody.getString("nodename"))
    }

    eb.consumer<Any>("metrics") { message ->
      val messageBody = message.body() as JsonObject
      dataMap[messageBody.getString("nodename")] = Buffer.buffer(messageBody.getBinary("data"))
      //TODO: add metric with buffer sizes
    }
  }

  fun getMetrics(routingContext: RoutingContext) {

    val nodename = routingContext.request().getParam("nodename")

    val response = routingContext.response()
    response.isChunked = true

    val metricData = dataMap[nodename]
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
