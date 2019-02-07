package ch.postfinance.moxy.moxy

import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import java.util.concurrent.TimeUnit

class EndpointDeployerVerticle: AbstractVerticle() {

  private val deploymentMap = mutableMapOf<String, String>()

  override fun start() {
    val eb = vertx.eventBus()

    eb.consumer<JsonObject>("deployer.endpoint.jmx") {

      val node = NodeModel(it.body())

      val deploymentOptions = DeploymentOptions()
        .setWorker(true)
        .setWorkerPoolName("bootstrap")
        .setMaxWorkerExecuteTime(2)  //make this configurable
        .setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES)

      vertx.deployVerticle(
        JmxEndpointVerticle(node.nodeName, node.configFile),
        DeploymentOptions(JsonObject(mapOf("worker" to true)))) { jmxMetricsResult ->
        if (jmxMetricsResult.succeeded()) {
          val deploymentId = jmxMetricsResult.result()
          System.out.println("deployed verticle ${deploymentId} for node ${node.nodeName}");

          //
          vertx.deployVerticle(
                  JmxUrlRetrieverVerticle(node.nodeName, node.jmxUrl, node.user, node.group),
                  deploymentOptions) { jmxMetricsResult ->
            if (jmxMetricsResult.succeeded()) {
              vertx.undeploy(jmxMetricsResult.result())
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
