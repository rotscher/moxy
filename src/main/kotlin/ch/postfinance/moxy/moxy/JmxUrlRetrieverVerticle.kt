package ch.postfinance.moxy.moxy

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.WorkerExecutor
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream

class JmxUrlRetrieverVerticle(private val nodeName: String, private val jmxUrl: String, private val user: String, private val group: String) : AbstractVerticle() {

  override fun start() {
    val eb = vertx.eventBus()

    if (jmxUrl != "") {
      eb.send(nodeName, "$jmxUrl")
    } else {
      //TODO: add metric on how long it took to get the jvm url
      IntStream.range(0, 70).forEach { Thread.sleep(1000) }
      val jmxUrl = "jmx url for ${nodeName}"
      eb.send(nodeName, jmxUrl)
      eb.publish("update-jmx", JsonObject(mapOf("nodeName" to nodeName, "jmxUrl" to jmxUrl)))
    }

  }
}
