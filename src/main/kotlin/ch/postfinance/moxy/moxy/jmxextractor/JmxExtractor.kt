package ch.postfinance.moxy.moxy.jmxextractor

interface JmxExtractor {
    @Throws(Exception::class)
    abstract fun extractJmxUrl(pid: Int): String
}