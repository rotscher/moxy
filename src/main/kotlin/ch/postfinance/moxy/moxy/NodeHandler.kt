package ch.postfinance.moxy.moxy

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

class NodeHandler {

  private val deploymentMap = mutableMapOf<String, String>()

  fun getAll(routingContext: RoutingContext) {
    val response = routingContext.response()
    response.putHeader("content-type", "application/json")
    response.isChunked = true
    response.write(JsonObject(deploymentMap.toMap()).encodePrettily())
    response.end()
  }

  fun getNode(routingContext: RoutingContext) {
    val nodeName = routingContext.request().getParam("nodeName")
  }

  fun addNode(routingContext: RoutingContext) {

    //val jmxUrl = "service:jmx:rmi://127.0.0.1/stub/rO0ABXN9AAAAAQAlamF2YXgubWFuYWdlbWVudC5yZW1vdGUucm1pLlJNSVNlcnZlcnhyABdqYXZhLmxhbmcucmVmbGVjdC5Qcm94eeEn2iDMEEPLAgABTAABaHQAJUxqYXZhL2xhbmcvcmVmbGVjdC9JbnZvY2F0aW9uSGFuZGxlcjt4cHNyAC1qYXZhLnJtaS5zZXJ2ZXIuUmVtb3RlT2JqZWN0SW52b2NhdGlvbkhhbmRsZXIAAAAAAAAAAgIAAHhyABxqYXZhLnJtaS5zZXJ2ZXIuUmVtb3RlT2JqZWN002G0kQxhMx4DAAB4cHcyAApVbmljYXN0UmVmAAkxMjcuMC4wLjEAAKFj3grabr/F9WJHz8xTAAABaKuY7daAAgB4"

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
    val result = deployEndpoint.compose{reply ->
      persistEndpoint(data, routingContext.vertx())
    }
    result.setHandler { asyncResult ->
        if (asyncResult.succeeded()) {
          System.out.println("persistence ok")
        }
        else {
          // oh ! we have a problem...
          System.out.println("persistence not ok")
        }
      }

    val response = routingContext.response()
    response.statusCode = 201
    response.end()

    //persist the nodes

  }

  private fun persistEndpoint(data: JsonObject, vertx: Vertx): Future<Void>? {
    return EndpointPersistence.addEndpoint(data, vertx)
  }

  fun removeNode(routingContext: RoutingContext) {
    val nodeName = routingContext.request().getParam("nodeName")
    val response = routingContext.response()

    if (deploymentMap.containsKey(nodeName)) {
      routingContext.vertx().undeploy(deploymentMap.get(nodeName)) {
        if (it.succeeded()) {
          deploymentMap.remove(nodeName)

          routingContext.vertx().eventBus().publish("remove-endpoint", nodeName)
        } else   {
          //increment a counter
        }
      }

      response.statusCode = 200
    } else {
      response.statusCode = 400
    }

    response.end()
    //persist the nodes
  }

}
