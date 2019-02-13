package ch.postfinance.moxy.moxy.metrics

import ch.postfinance.moxy.moxy.metricsRegistry
import io.micrometer.core.instrument.ImmutableTag
import java.util.concurrent.atomic.AtomicLong

class MetricsDataSizeGauge(val nodeName: String) {

    private val atomicLong = AtomicLong()

    init {
        metricsRegistry.gauge("metrics_data_size_bytes", mutableListOf(ImmutableTag("nodeName", nodeName)), atomicLong)
    }

    fun setValue(curVal: Double) {
        atomicLong.set(curVal.toLong())
    }
}