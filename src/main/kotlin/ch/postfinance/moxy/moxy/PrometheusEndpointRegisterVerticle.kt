package ch.postfinance.moxy.moxy

import io.vertx.core.AbstractVerticle

class PrometheusEndpointRegisterVerticle : AbstractVerticle() {

  override fun start() {
    println("First this is printed")
  }

  override fun stop() {
    println("stopping the endpoint")
  }
}
