package com.dansplugins.factionsystem.faction.permission

import com.dansplugins.factionsystem.MedievalFactions
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class MfFactionPermissionSerializer(
    private val plugin: MedievalFactions
) : JsonSerializer<MfFactionPermission?>, JsonDeserializer<MfFactionPermission?> {
    override fun serialize(
        src: MfFactionPermission?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.name)
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): MfFactionPermission? {
        if (json !is JsonPrimitive) return null
        if (!json.isString) return null
        return plugin.factionPermissions.parse(json.asString)
    }
}
