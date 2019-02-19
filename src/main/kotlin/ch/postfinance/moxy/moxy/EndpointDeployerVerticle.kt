package ch.postfinance.moxy.moxy

import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class EndpointDeployerVerticle: AbstractVerticle() {

  private val LOG = Logger.getLogger("moxy.general")

  override fun start() {
    val eb = vertx.eventBus()

    eb.consumer<JsonObject>("deployer.endpoint.jmx") {

      val config = config()
      val node = NodeModel(it.body())

      val deploymentOptions = DeploymentOptions()
        .setWorker(true)
        .setWorkerPoolName("bootstrap")
        .setMaxWorkerExecuteTime(config.getLong("bootstrapMaxWait", 15))
        .setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES).setConfig(config)

      vertx.deployVerticle(
        JmxEndpointVerticle(node.nodeName, node.configFile),
        DeploymentOptions(JsonObject(mapOf("worker" to true))).setConfig(config)) { jmxMetricsResult ->
        if (jmxMetricsResult.succeeded()) {
          val json = node.asJsonObject()
          json.put("deploymentId", jmxMetricsResult.result())
          eb.send("nodehandler.register", json)
          LOG.info("deployed verticle, node=${node.nodeName}")

          vertx.deployVerticle(
                  JmxUrlRetrieverVerticle(node.nodeName, node.jmxUrl, node.pid, node.user, node.group),
                  deploymentOptions) { jmxUrlRetrieverResult ->
            if (!jmxUrlRetrieverResult.succeeded()) {
              LOG.severe("jmx url retriever, node=${node.nodeName}, cause=${jmxUrlRetrieverResult.cause()}")
              incrementErrorCount("deploy_JmxUrlRetrieverVerticle", node.nodeName)
            }
            vertx.undeploy(jmxUrlRetrieverResult.result())
          }
        } else   {
          incrementErrorCount("deploy_JmxEndpointVerticle", node.nodeName)
          LOG.severe("could not deploy verticle, node=${node.nodeName}, cause=${jmxMetricsResult.cause()}")
        }
      }
      it.reply("success")
    }
  }
}
