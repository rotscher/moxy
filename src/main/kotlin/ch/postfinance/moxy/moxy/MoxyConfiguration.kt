package ch.postfinance.moxy.moxy

import org.yaml.snakeyaml.Yaml
import java.io.FileReader

object MoxyConfiguration {

    var configuration: Configuration =  Yaml().loadAs(
            FileReader(System.getProperty("moxy.configuration.path", "src/main/resources/moxy.yaml")),
            Configuration::class.java)
}

data class Configuration(var name: String, var enabled: Boolean, var httpServerPort: Int, var dataFile: String, var scrapeDelay: Long, var metricsLimit: Int, var bootstrapMaxWait: Long, var debug: DebugConf) {


    constructor(): this("", true, 8181, "", 15000, -1,15, DebugConf())
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