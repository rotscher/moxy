package ch.postfinance.moxy.moxy

import io.vertx.core.json.JsonObject

class NodeModel(val nodeName: String, val jmxUrl: String, val configFile: String, val pid: Int = -1, val user: String, val group: String) {

    constructor(jsonObject: JsonObject) : this(jsonObject.getString("nodeName"),
            jsonObject.getString("jmxUrl", ""),
            jsonObject.getString("configFile"),
            jsonObject.getInteger("pid"),
            jsonObject.getString("user", ""),
            jsonObject.getString("group", "")) {

    }

    fun asJsonObject(): JsonObject {
        return JsonObject(mapOf(
                "nodeName" to nodeName,
                "jmxUrl" to jmxUrl,
                "configFile" to configFile,
                "pid" to pid,
                "user" to user,
                "group" to group))
    }
}