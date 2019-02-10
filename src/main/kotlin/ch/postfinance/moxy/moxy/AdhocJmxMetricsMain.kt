package ch.postfinance.moxy.moxy

import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.jmx.JmxCollector
import java.io.PrintWriter
import java.util.*

fun main() {
    var hostPort = "rotscher-pc:9010"
    val jc = JmxCollector(("{"
            + "`hostPort`: `" + hostPort + "`,"
            + "}").replace('`', '"'))
    val vector = Vector(jc.collect()).elements()
    TextFormat.write004(PrintWriter(System.out), vector)
}
