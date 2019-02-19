package ch.postfinance.moxy.moxy

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

class HttpServerVerticle : AbstractVerticle() {

  override fun start(startFuture: Future<Void>) {
    val router = Router.router(vertx)

    val handler = NodeHandler(vertx, config())
    val metricsHandler = MetricsHandler(vertx)

    router.route().handler(BodyHandler.create())
    router.route(HttpMethod.POST, "/nodes/:nodeName").handler(handler::addNode)
    router.route(HttpMethod.GET, "/nodes/").handler(handler::getAll)
    router.route(HttpMethod.GET, "/nodes/:nodeName").handler(handler::getNode)
    router.route(HttpMethod.DELETE, "/nodes/:nodeName").handler(handler::removeNode)
    router.route(HttpMethod.GET, "/nodes/:nodeName/metrics").handler(metricsHandler::getMetrics)

    // Setup a route for metrics
    router.route("/metrics/").handler { ctx ->
      val response = metricsRegistry.scrape()
      ctx.response().end(response)
    }

    val port = config().getInteger("httpServerPort", 8080)

    vertx.createHttpServer().requestHandler(router)
      .listen(port) { http ->
        if (http.succeeded()) {
          startFuture.complete()
          println("HTTP server started on port $port")
        } else {
          startFuture.fail(http.cause())
        }
      }
  }

  override fun stop() {
    System.out.println("stop http server")
  }
}
