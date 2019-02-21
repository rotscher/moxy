package ch.postfinance.moxy.moxy.metrics

import ch.postfinance.moxy.moxy.metricsRegistry
import io.micrometer.core.instrument.ImmutableTag
import java.util.concurrent.atomic.AtomicLong

class MetricsDataSizeGauge(val nodeName: String) {

    private val metricsByteCount = AtomicLong()

    init {
        metricsRegistry.gauge("moxy_metrics_data_size_bytes", mutableListOf(ImmutableTag("nodeName", nodeName)), metricsByteCount)
    }

    fun setByteCount(curVal: Double) {
        metricsByteCount.set(curVal.toLong())
    }
}