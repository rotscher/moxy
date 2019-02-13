package ch.postfinance.moxy.moxy

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import java.util.logging.Logger

object EndpointPersistence {

  private val LOG = Logger.getLogger("moxy.general")
  private val dataMap = JsonObject()

  fun init(vertx: Vertx) {
    vertx.fileSystem().readFile(MoxyConfiguration.configuration.dataFile) { result ->
      if (result.succeeded()) {
        JsonObject(result.result()).forEach {
          dataMap.put(it.key, it.value)
          vertx.eventBus().send("deployer.endpoint.jmx", it.value)
        }
      } else {
        incrementErrorCount("init_persistence", "n/A")
        LOG.severe("error while loading verticles, cause=${result.cause()}")
      }
    }
  }

  fun addEndpoint(endpoint: JsonObject, vertx: Vertx): Future<Void>? {
    dataMap.put(endpoint.getString("nodeName"), endpoint)
    return persist(vertx)
  }

  fun removeEndpoint(nodeName: String, vertx: Vertx): Future<Void>? {
    dataMap.remove(nodeName)
    return persist(vertx)
  }

  fun updateJmxUrl(nodeName: String, jmxUrl: String, vertx: Vertx): Future<Void>? {
    dataMap.getJsonObject(nodeName).put("jmxUrl", jmxUrl)
    return persist(vertx)
  }

  private fun persist(vertx: Vertx): Future<Void>? {
    val future = Future.future<Void>()
    vertx.fileSystem().writeFile(MoxyConfiguration.configuration.dataFile, dataMap.toBuffer(), future.completer())
    return future
  }
}
