package eu.riseoftheblacksun.payments.utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonUtil {
    val prettyJson = Json { prettyPrint = true }
    val json = Json

    inline fun <reified T> toJson(obj: T, readable: Boolean = true): String {
        return if (readable) prettyJson.encodeToString(obj)
        else json.encodeToString(obj)
    }

    inline fun <reified T> fromJson(jsonString: String): T {
        return prettyJson.decodeFromString(jsonString)
    }
}