package ch.postfinance.moxy.moxy

import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import java.util.concurrent.TimeUnit

class EndpointDeployerVerticle: AbstractVerticle() {

  override fun start() {
    val eb = vertx.eventBus()

    eb.consumer<JsonObject>("deployer.endpoint.jmx") {

      val node = NodeModel(it.body())

      val deploymentOptions = DeploymentOptions()
        .setWorker(true)
        .setWorkerPoolName("bootstrap")
        .setMaxWorkerExecuteTime(2)  //TODO: make this configurable
        .setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES)

      vertx.deployVerticle(
        JmxEndpointVerticle(node.nodeName, node.configFile),
        DeploymentOptions(JsonObject(mapOf("worker" to true)))) { jmxMetricsResult ->
        if (jmxMetricsResult.succeeded()) {
          val json = node.asJsonObject()
          json.put("deploymentId", jmxMetricsResult.result())
          eb.send("nodehandler.register", json)

          vertx.deployVerticle(
                  JmxUrlRetrieverVerticle(node.nodeName, node.jmxUrl, node.user, node.group),
                  deploymentOptions) { jmxUrlRetrieverResult ->
            if (jmxUrlRetrieverResult.succeeded()) {
              vertx.undeploy(jmxUrlRetrieverResult.result())
            } else   {
              //increment a counter
            }
          }


        } else   {
          //increment a counter
        }
      }
      it.reply("success")
    }
  }
}
