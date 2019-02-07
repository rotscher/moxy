package ch.postfinance.moxy.moxy

import io.vertx.core.json.JsonObject

class NodeModel(val nodeName: String, val jmxUrl: String, val configFile: String, val user: String, val group: String) {

    constructor(jsonObject: JsonObject) : this(jsonObject.getString("nodeName"),
            jsonObject.getString("jmxUrl", ""),
            jsonObject.getString("configFile"),
            jsonObject.getString("user", ""),
            jsonObject.getString("group", "")) {

    }

    fun asJsonObject(): JsonObject {
        return JsonObject(mapOf(
                "nodeName" to nodeName,
                "jmxUrl" to jmxUrl,
                "configFile" to configFile,
                "user" to user,
                "group" to group))
    }
}