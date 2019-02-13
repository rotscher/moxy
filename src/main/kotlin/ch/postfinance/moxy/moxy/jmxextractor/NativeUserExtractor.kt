package ch.postfinance.moxy.moxy.jmxextractor

import javax.management.remote.JMXServiceURL

class NativeUserExtractor : JmxExtractor {
    override fun extractJmxUrl(pid: Int): String {
        val jmxUrl: String
        val vm = com.sun.tools.attach.VirtualMachine.attach(pid.toString())

        try {
            var connectorAddress = vm.getAgentProperties().getProperty(
                    "com.sun.management.jmxremote.localConnectorAddress")
            if (connectorAddress == null) {
                val agent = vm.getSystemProperties()
                        .getProperty("java.home") + "/lib/management-agent.jar"
                vm.loadAgent(agent)
                connectorAddress = vm.getAgentProperties().getProperty(
                        "com.sun.management.jmxremote.localConnectorAddress")
            }
            jmxUrl = "service:jmx:rmi://127.0.0.1" + JMXServiceURL(connectorAddress!!).urlPath
        } finally {
            vm.detach()
        }

        return jmxUrl
    }
}