package ch.postfinance.moxy.moxy.metrics

import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.micrometer.backends.BackendRegistries
import java.util.concurrent.atomic.AtomicLong

class MetricsDataSizeGauge(val nodeName: String) {

    private val registry = BackendRegistries.getDefaultNow() as PrometheusMeterRegistry
    private val atomicLong = AtomicLong()

    init {
        registry.gauge("metrics_data_size_bytes", mutableListOf(ImmutableTag("nodeName", nodeName)), atomicLong)
    }

    fun setValue(curVal: Double) {
        atomicLong.set(curVal.toLong())
    }
}