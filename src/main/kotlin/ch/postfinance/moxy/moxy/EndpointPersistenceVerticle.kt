package ch.postfinance.moxy.moxy

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject

object EndpointPersistence {

  private val dataMap = JsonObject()

  fun init(vertx: Vertx) {
    vertx.fileSystem().readFile("endpoints.json") { result ->
      if (result.succeeded()) {
        JsonObject(result.result()).forEach {
          dataMap.put(it.key, it.value)
          vertx.eventBus().send("deployer.endpoint.jmx", it.value)
        }
        println(result.result())
      } else {
        System.err.println("Oh oh ...${result.cause()}")
      }
    }
  }

  fun addEndpoint(endpoint: JsonObject, vertx: Vertx): Future<Void>? {
    dataMap.put(endpoint.getString("nodename"), endpoint)
    return persist(vertx)
  }

  fun removeEndpoint(nodeName: String, vertx: Vertx, handler: Future<Void>) {
    dataMap.remove(nodeName)
    persist(vertx)
  }

  fun removeEndpoint(nodeName: String, jmxUrl: String, vertx: Vertx, handler: Future<Void>) {
    dataMap.getJsonObject(nodeName).put("jmxUrl", jmxUrl)
    persist(vertx)
  }

  private fun persist(vertx: Vertx): Future<Void>? {
    val future = Future.future<Void>()
    vertx.fileSystem().writeFile("endpoints.json", dataMap.toBuffer(), future.completer())
    return future
  }
}
