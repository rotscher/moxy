package ch.postfinance.moxy.moxy.persistence

import ch.postfinance.moxy.moxy.incrementErrorCount
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import java.util.logging.Logger


class WriteVerticle : AbstractVerticle() {

    private val LOG = Logger.getLogger("moxy.general")
    private var dataMap = JsonObject()

    override fun start() {
        vertx.eventBus().consumer<JsonObject>("persistence.writer.data") {
            dataMap = it.body()
        }

        vertx.eventBus().consumer<JsonObject>("persistence.writer.addnode") { message ->
            val nodeName = message.body().getString("nodeName")
            dataMap.put(nodeName, message.body())
            persist().setHandler {result ->
                if (!result.succeeded()) {
                    incrementErrorCount("persist_addnode", nodeName)
                }
            }
        }

        vertx.eventBus().consumer<JsonObject>("persistence.writer.updatenode") { message ->
            val nodeName = message.body().getString("nodeName")
            val jmxUrl = message.body().getString("jmxUrl")
            dataMap.getJsonObject(nodeName).put("jmxUrl", jmxUrl)
            persist().setHandler {result ->
                if (!result.succeeded()) {
                    incrementErrorCount("persist_addnode", nodeName)
                }
            }
        }

        vertx.eventBus().consumer<String>("persistence.writer.removenode") { message ->
            val nodeName = message.body()
            dataMap.remove(nodeName)
            persist().setHandler {result ->
                if (!result.succeeded()) {
                    incrementErrorCount("persist_addnode", nodeName)
                }
            }
        }
    }

    private fun persist(): Future<Void> {
        val future = Future.future<Void>()
        vertx.fileSystem().writeFile(config().getString("dataFile"), dataMap.toBuffer(), future.completer())
        return future
    }
}