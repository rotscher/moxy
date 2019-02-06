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

      val nodeName = it.body().getString("nodename")
      val jmxUrl = it.body().getString("jmxUrl")
      val configuration = it.body().getString("configuration")
      val deploymentOptions = DeploymentOptions()
        .setWorker(true)
        .setWorkerPoolName("bootstrap")
        .setMaxWorkerExecuteTime(2)  //make this configurable
        .setMaxWorkerExecuteTimeUnit(TimeUnit.MINUTES)

      vertx.deployVerticle(
        JmxUrlRetrieverVerticle(nodeName, jmxUrl, "user", "group"),
        deploymentOptions) { jmxMetricsResult ->
        if (jmxMetricsResult.succeeded()) {
          vertx.undeploy(jmxMetricsResult.result())
        } else   {
          //increment a counter
        }
      }

      var deploymentId = ""
      vertx.deployVerticle(
        JmxEndpointVerticle(nodeName, configuration),
        DeploymentOptions(JsonObject(mapOf("worker" to true)))) { jmxMetricsResult ->
        if (jmxMetricsResult.succeeded()) {
          deploymentId = jmxMetricsResult.result()
          System.out.println("deployed verticle ${deploymentId} for node ${nodeName}");
        } else   {
          //increment a counter
        }
      }
      it.reply(deploymentId)
    }
  }
}
