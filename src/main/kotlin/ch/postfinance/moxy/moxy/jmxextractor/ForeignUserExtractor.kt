package ch.postfinance.moxy.moxy.jmxextractor

import ch.postfinance.moxy.moxy.MoxyConfiguration
import ch.postfinance.moxy.moxy.incrementErrorCount
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ForeignUserExtractor(val user: String, val group: String) : JmxExtractor {
    override fun extractJmxUrl(pid: Int): String {
        var url = ""

        var javaHome = System.getProperty("java.home")
        javaHome = javaHome.substring(0, javaHome.length - 4)

        val builder = ProcessBuilder("scripts/getjmxurl.sh", pid.toString(), user, group)
        val env = builder.environment()
        env.clear()
        env["JAVA_HOME"] = javaHome


        val process: Process
        try {
            process = builder.start()
        } catch (ioe: IOException) {
            incrementErrorCount("processBuilder_start", "n/A")
            throw Exception(ioe)
        }

        val result: Boolean
        try {
            result = process.waitFor(MoxyConfiguration.configuration.jmxRetrievalDelay, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            incrementErrorCount("processBuilder_wait", "n/A")
            process.destroy()
            throw Exception(e)
        }

        try {
            if (!result) {
                process.destroy()
            } else {
                val br = BufferedReader(InputStreamReader(process.inputStream))
                url = br.readLines().last()
            }
        } catch (ioe: IOException) {
            incrementErrorCount("processBuilder_readLines", "n/A")

        }

        return url
    }
}