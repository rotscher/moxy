package ch.postfinance.moxy.moxy

import io.prometheus.client.Collector
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test as test

class JmxEndpointVerticleTest {

    @test
    fun `no scrape errors in empty list`() {
        val endpointVerticle = JmxEndpointVerticle("testNode", "")
        val dataList = mutableListOf< Collector.MetricFamilySamples>()
        assertFalse { endpointVerticle.hasScrapeError(dataList) }
    }

    @test
    fun `no scrape error sample in list`() {
        val endpointVerticle = JmxEndpointVerticle("testNode", "")
        val mfsList = listOf(
                Collector.MetricFamilySamples("test_sample_1", Collector.Type.GAUGE, "help text 1",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_1", emptyList(), emptyList(), 1344.0))),
                Collector.MetricFamilySamples("test_sample_2", Collector.Type.GAUGE, "help text 2",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_2", emptyList(), emptyList(), 5544.0))))

        assertFalse { endpointVerticle.hasScrapeError(mfsList) }
    }

    @test
    fun `scrape error sample with value 0 in list`() {
        val endpointVerticle = JmxEndpointVerticle("testNode", "")
        val mfsList = listOf(
                Collector.MetricFamilySamples("test_sample_1", Collector.Type.GAUGE, "help text 1",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_1", emptyList(), emptyList(), 1344.0))),
                Collector.MetricFamilySamples("test_sample_2", Collector.Type.GAUGE, "help text 2",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_2", emptyList(), emptyList(), 5544.0))),
                Collector.MetricFamilySamples("jmx_scrape_error", Collector.Type.GAUGE, "help scrape error",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("jmx_scrape_error", emptyList(), emptyList(), 0.0))))

        assertFalse { endpointVerticle.hasScrapeError(mfsList) }
    }

    @test
    fun `multipe scrape error sample with value 1 in list`() {
        val endpointVerticle = JmxEndpointVerticle("testNode", "")
        val mfsList = listOf(
                Collector.MetricFamilySamples("jmx_scrape_error", Collector.Type.GAUGE, "help scrape error",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("jmx_scrape_error", emptyList(), emptyList(), 3.0))),
                Collector.MetricFamilySamples("test_sample_1", Collector.Type.GAUGE, "help text 1",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_1", emptyList(), emptyList(), 1344.0))),
                Collector.MetricFamilySamples("test_sample_2", Collector.Type.GAUGE, "help text 2",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_2", emptyList(), emptyList(), 5544.0))),
                Collector.MetricFamilySamples("jmx_scrape_error", Collector.Type.GAUGE, "help scrape error",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("jmx_scrape_error", emptyList(), emptyList(), 2.0))))

        assertTrue { endpointVerticle.hasScrapeError(mfsList) }
    }

    @test
    fun `scrape error sample with value 1 in list`() {
        val endpointVerticle = JmxEndpointVerticle("testNode", "")
        val mfsList = listOf(
                Collector.MetricFamilySamples("test_sample_1", Collector.Type.GAUGE, "help text 1",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_1", emptyList(), emptyList(), 1344.0))),
                Collector.MetricFamilySamples("test_sample_2", Collector.Type.GAUGE, "help text 2",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_2", emptyList(), emptyList(), 5544.0))),
                Collector.MetricFamilySamples("jmx_scrape_error", Collector.Type.GAUGE, "help scrape error",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("jmx_scrape_error", emptyList(), emptyList(), 1.0))))

        assertTrue { endpointVerticle.hasScrapeError(mfsList) }
    }

    @test
    fun `only one scrape error sample with value 1 in list`() {
        val endpointVerticle = JmxEndpointVerticle("testNode", "")
        val mfsList = listOf(
                Collector.MetricFamilySamples("jmx_scrape_error", Collector.Type.GAUGE, "help scrape error",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("jmx_scrape_error", emptyList(), emptyList(), 1.0))))

        assertTrue { endpointVerticle.hasScrapeError(mfsList) }
    }

    @test
    fun `no valid scrape error family in list`() {
        val endpointVerticle = JmxEndpointVerticle("testNode", "")
        val mfsList = listOf(
                Collector.MetricFamilySamples("test_sample_1", Collector.Type.GAUGE, "help text 1",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_1", emptyList(), emptyList(), 1344.0))),
                Collector.MetricFamilySamples("test_sample_2", Collector.Type.GAUGE, "help text 2",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_2", emptyList(), emptyList(), 5544.0))),
                Collector.MetricFamilySamples("some_other_error", Collector.Type.GAUGE, "help scrape error",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("jmx_scrape_error", emptyList(), emptyList(), 1.0))))

        assertFalse { endpointVerticle.hasScrapeError(mfsList) }
    }

    @test
    fun `no valid scrape error sample in list`() {
        val endpointVerticle = JmxEndpointVerticle("testNode", "")
        val mfsList = listOf(
                Collector.MetricFamilySamples("test_sample_1", Collector.Type.GAUGE, "help text 1",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_1", emptyList(), emptyList(), 1344.0))),
                Collector.MetricFamilySamples("test_sample_2", Collector.Type.GAUGE, "help text 2",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("test_sample_2", emptyList(), emptyList(), 5544.0))),
                Collector.MetricFamilySamples("jmx_scrape_error", Collector.Type.GAUGE, "help scrape error",
                        /* samples */ listOf(Collector.MetricFamilySamples.Sample("some_other_error", emptyList(), emptyList(), 1.0))))

        assertFalse { endpointVerticle.hasScrapeError(mfsList) }
    }
}