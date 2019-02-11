package ch.postfinance.moxy.moxy

import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.micrometer.backends.BackendRegistries

class NodeHandler(vertx: Vertx) {

  private val deploymentMap = mutableMapOf<String, JsonObject>()

  init {
    val eb = vertx.eventBus()

    eb.consumer<Any>("nodehandler.register") { message ->
      val messageBody = message.body() as JsonObject
      val nodeName = messageBody.getString("nodeName")
      deploymentMap[nodeName] = messageBody
    }

    eb.consumer<Any>("nodehandler.update") { message ->
      val messageBody = message.body() as JsonObject
      val nodeName = messageBody.getString("nodeName")
      deploymentMap[nodeName]?.put("url", messageBody.getString("url"))
    }

    eb.consumer<Any>("metrics-remove") { message ->
      val messageBody = message.body() as JsonObject
      val nodeName = messageBody.getString("nodeName")
      deploymentMap.remove(nodeName)
    }
  }

  fun getAll(routingContext: RoutingContext) {
    val response = routingContext.response()
    response.putHeader("content-type", "application/json")
    response.isChunked = true
    deploymentMap.forEach{ response.write(it.value.encodePrettily())}
    response.end()
  }

  fun getNode(routingContext: RoutingContext) {
    val nodeName = routingContext.request().getParam("nodeName")
    val response = routingContext.response()

    if (!deploymentMap.containsKey(nodeName)) {
      val response = routingContext.response()
      response.statusCode = 404
      response.end()
      return
    }

    response.putHeader("content-type", "application/json")
    response.isChunked = true
    response.write(deploymentMap[nodeName]?.encodePrettily())
    response.end()
  }

  fun addNode(routingContext: RoutingContext) {

    if (!MoxyConfiguration.configuration.enabled) {
      val response = routingContext.response()
      response.statusCode = 204
      response.end()
      return
    }
    val nodeName = routingContext.request().getParam("nodeName")
    val jmxUrl: String? = routingContext.bodyAsJson.getString("jmxUrl")
    val configFile = routingContext.bodyAsJson.getString("configFile")
    val user: String? = routingContext.bodyAsJson.getString("user")
    val group: String? = routingContext.bodyAsJson.getString("group")

    if (deploymentMap.containsKey(nodeName)) {
      val response = routingContext.response()
      response.statusCode = 304
      response.end()
      return
    }

    val data = NodeModel(nodeName, if (jmxUrl == null) "" else jmxUrl, configFile, if (user == null) "" else user, if (group == null) "" else group).asJsonObject()

    val deployEndpoint = Future.future<Message<Any>>()

    routingContext.vertx().eventBus().send("deployer.endpoint.jmx", data, deployEndpoint.completer())
    //TODO: register endpoint as prometheus service, get the code from midwadm

    val result = deployEndpoint.compose{
      EndpointPersistence.addEndpoint(data, routingContext.vertx())
    }

    result.setHandler { asyncResult ->
      if (!asyncResult.succeeded()) {
        val registry = BackendRegistries.getDefaultNow() as PrometheusMeterRegistry
        registry.counter("moxy_error_count", "name", "endpoint_persistence", "action", "add").increment()
      }
    }

    val response = routingContext.response()
    response.statusCode = 201
    response.putHeader("Location", "${routingContext.currentRoute().path}/${nodeName}")
    response.end()
  }


  fun removeNode(routingContext: RoutingContext) {
    val nodeName = routingContext.request().getParam("nodeName")
    val response = routingContext.response()

    val undeployEndpoint = Future.future<Void>()

    if (deploymentMap.containsKey(nodeName)) {
      routingContext.vertx().undeploy(deploymentMap.get(nodeName)?.getString("deploymentId"), undeployEndpoint.completer())

      val result = undeployEndpoint.compose{
        EndpointPersistence.removeEndpoint(nodeName, routingContext.vertx())
      }

      result.setHandler { asyncResult ->
        if (!asyncResult.succeeded()) {
          val registry = BackendRegistries.getDefaultNow() as PrometheusMeterRegistry
          registry.counter("moxy_error_count", "name", "endpoint_persistence", "action", "remove").increment()
        }
      }

      response.statusCode = 200
    } else {
      response.statusCode = 400
    }

    response.end()
  }

}
