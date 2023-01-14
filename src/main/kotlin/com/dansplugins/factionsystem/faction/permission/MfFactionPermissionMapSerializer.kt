package com.dansplugins.factionsystem.faction.permission

import com.dansplugins.factionsystem.MedievalFactions
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class MfFactionPermissionMapSerializer(
    private val plugin: MedievalFactions
) : JsonDeserializer<Map<MfFactionPermission, Boolean?>> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Map<MfFactionPermission, Boolean?>? {
        if (json?.isJsonObject != true) return null
        val permissionsJsonObject = json.asJsonObject
        return permissionsJsonObject.keySet()
            .associateWith { plugin.factionPermissions.parse(it) }
            .filter { (_, value) -> value != null }
            .toList()
            .associate { (key, value) ->
                if (!permissionsJsonObject.get(key).isJsonPrimitive || !permissionsJsonObject.getAsJsonPrimitive(key).isBoolean) {
                    value!! to null
                } else {
                    value!! to permissionsJsonObject.getAsJsonPrimitive(key)?.asBoolean
                }
            }
    }
}
