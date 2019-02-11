package ch.postfinance.moxy.moxy

import org.yaml.snakeyaml.Yaml
import java.io.FileReader

object MoxyConfiguration {

    var configuration: Configuration =  Yaml().loadAs(
            FileReader(System.getProperty("moxy.configuration.path", "src/main/resources/moxy.yaml")),
            Configuration::class.java)
   /* var isMoxyEnabled = false
    var metricsLimit = -1

    /* for debugging and testing performance, not to be used in production */
    var performanceDebug = false
    var fakeMetricsNbr = 50_000

    /* for testing and debugging of jmx url retrieval */
    var staticJmxRetrieval = false
    var jmxUrl: String? = null
    var hostPort: String? = null
    var jmxDelay = false
    var jmxDelayCount = 10*/

    fun init() {

        //TODO: make filepath configurable
        System.out.println(configuration)

       /* configuration = Yaml().loadAs(
                FileReader(System.getProperty("moxy.configuration.path", "src/main/resources/moxy.yaml")),
                Configuration::class.java)
*/
/*
        isMoxyEnabled = yamlLoad.getOrDefault("moxy.enabled", "false").toBoolean()
        metricsLimit = yamlLoad.getOrDefault("moxy.scrape.limit", "-1").toInt()
        performanceDebug = yamlLoad.getOrDefault("moxy.debug.performance.enabled", "false").toBoolean()
        fakeMetricsNbr = yamlLoad.getOrDefault("moxy.debug.performance.fakeMetrics", "50000").toInt()
        staticJmxRetrieval = yamlLoad.getOrDefault("moxy.debug.jmxRetrieval.static", "false").toBoolean()
        jmxUrl = yamlLoad.get("moxy.debug.jmxRetrieval.jmxUrl")
        hostPort = yamlLoad.get("moxy.debug.jmxRetrieval.hostPort")
        jmxDelay = yamlLoad.getOrDefault("moxy.debug.jmxRetrieval.delay.enabled", "false").toBoolean()
        jmxDelayCount = yamlLoad.getOrDefault("moxy.debug.jmxRetrieval.delay.count", "10").toInt()*/
    }



}

data class Configuration(var name: String, var enabled: Boolean, var metricsLimit: Int, var debug: DebugConf) {
    constructor(): this("", true, -1, DebugConf())
}

data class DebugConf(var performance: PerformanceConf, var jmxRetrieval: JmxRetrievalConf) {
    constructor(): this(PerformanceConf(), JmxRetrievalConf())
}
data class PerformanceConf(var enabled: Boolean, var fakeMetrics: Int) {
    constructor(): this(false, 1000)
}
data class JmxRetrievalConf(var static: Boolean, var hostPort: String?, var jmxUrl: String?, var delay: DelayConf) {
    constructor(): this(false, null, null, DelayConf())
}
data class DelayConf(var enabled: Boolean, var count: Int) {
    constructor(): this(false, 10)
}