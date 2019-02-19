package ch.postfinance.moxy.moxy.metrics

import ch.postfinance.moxy.moxy.metricsRegistry
import io.micrometer.core.instrument.ImmutableTag
import java.util.concurrent.atomic.AtomicLong

class MetricsDataSizeGauge(val nodeName: String) {

    private val metricsByteCount = AtomicLong()
    private val metricsCount = AtomicLong()
    private val samplesCount = AtomicLong()

    init {
        metricsRegistry.gauge("moxy_metrics_data_size_bytes", mutableListOf(ImmutableTag("nodeName", nodeName)), metricsByteCount)
        metricsRegistry.gauge("moxy_metrics_data_size_count", mutableListOf(ImmutableTag("nodeName", nodeName)), metricsCount)
        metricsRegistry.gauge("moxy_samples_data_size_count", mutableListOf(ImmutableTag("nodeName", nodeName)), samplesCount)
    }

    fun setByteCount(curVal: Double) {
        metricsByteCount.set(curVal.toLong())
    }

    fun setMetricsCount(curVal: Double) {
        metricsCount.set(curVal.toLong())
    }

    fun setSamplesCount(curVal: Double) {
        samplesCount.set(curVal.toLong())
    }
}