package ch.postfinance.moxy.moxy.persistence

import ch.postfinance.moxy.moxy.incrementErrorCount
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import java.util.logging.Logger

class LoadVerticle : AbstractVerticle() {

    private val LOG = Logger.getLogger("moxy.general")

    override fun start() {
        val configFileName = config().getString("dataFile")
        //vertx.fileSystem().exists(configFileName) {
          //  if (it.result()) {
                vertx.fileSystem().readFile(configFileName) { result ->
                    if (result.succeeded()) {
                        vertx.eventBus().send("persistence.writer.data", result.result())
                        JsonObject(result.result()).forEach {
                            vertx.eventBus().send("deployer.endpoint.jmx", it.value)
                        }
                    } else {
                        incrementErrorCount("init_persistence", "n/A")
                        LOG.severe("error while loading verticles, cause=${result.cause()}")
                    }
                }
           // }
        //}
    }
}