package ch.postfinance.moxy.moxy

import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.micrometer.backends.BackendRegistries

val metricsRegistry = BackendRegistries.getDefaultNow() as PrometheusMeterRegistry

fun incrementErrorCount(name: String, nodeName: String) {
    metricsRegistry.counter("moxy_error_count", "name", "$name", "nodeName", "$nodeName").increment()
}