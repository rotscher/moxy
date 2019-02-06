package ch.postfinance.moxy.moxy

import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.micrometer.backends.BackendRegistries

class HttpServerVerticle : AbstractVerticle() {

  override fun start(startFuture: Future<Void>) {

    val server = vertx
      .createHttpServer()

    val router = Router.router(vertx)

    val handler = NodeHandler()
    val metricsHandler = MetricsHandler(vertx)

    router.route().handler(BodyHandler.create())
    router.route(HttpMethod.POST, "/nodes/:nodename").handler(handler::addNode)
    router.route(HttpMethod.GET, "/nodes/").handler(handler::getAll)
    router.route(HttpMethod.GET, "/nodes/:nodename").handler(handler::getNode)
    router.route(HttpMethod.DELETE, "/nodes/:nodename").handler(handler::removeNode)
    router.route(HttpMethod.GET, "/nodes/:nodename/metrics").handler(metricsHandler::getMetrics)

    val registry = BackendRegistries.getDefaultNow() as PrometheusMeterRegistry
    // Setup a route for metrics
    router.route("/metrics").handler { ctx ->
      val response = registry.scrape()
      ctx.response().end(response)
    }

    server.requestHandler(router)
      .listen(8080) { http ->
        if (http.succeeded()) {
          startFuture.complete()
          println("HTTP server started on port 8080")
          //vertx.deployVerticle(PrometheusEndpointRegisterVerticle(), DeploymentOptions(JsonObject(mapOf("worker" to true))))
        } else {
          startFuture.fail(http.cause());
        }
      }
  }

  override fun stop() {
    System.out.println("stop http server")
  }
}
