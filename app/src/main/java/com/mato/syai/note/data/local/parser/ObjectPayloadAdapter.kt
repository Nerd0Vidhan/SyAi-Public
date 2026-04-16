package com.mato.syai.note.data.local.parser

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mato.syai.note.domain.local.model.ChecklistPayload
import com.mato.syai.note.domain.local.model.DrawingPayload
import com.mato.syai.note.domain.local.model.ImagePayload
import com.mato.syai.note.domain.local.model.LinearTextPayload
import com.mato.syai.note.domain.local.model.ListPayload
import com.mato.syai.note.domain.local.model.ObjectPayload
import com.mato.syai.note.domain.local.model.TextPayload
import com.mato.syai.note.domain.local.model.TextSpan
import java.lang.reflect.Type

class ObjectPayloadAdapter : JsonSerializer<ObjectPayload>, JsonDeserializer<ObjectPayload> {

    override fun serialize(
        src: ObjectPayload?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        if (src == null || context == null) return JsonNull.INSTANCE

        return when (src) {
            is TextPayload -> context.serialize(src).asJsonObject.apply {
                addProperty("payloadType", "TEXT")
            }

            is ImagePayload -> context.serialize(src).asJsonObject.apply {
                addProperty("payloadType", "IMAGE")
            }

            is DrawingPayload -> context.serialize(src).asJsonObject.apply {
                addProperty("payloadType", "DRAWING")
            }

            is ChecklistPayload -> context.serialize(src).asJsonObject.apply {
                addProperty("payloadType", "CHECKLIST")
            }
            is TextSpan -> context.serialize(src).asJsonObject.apply {
                addProperty("payloadType", "TEXTSPAN")
            }

            is LinearTextPayload -> context.serialize(src).asJsonObject.apply {
                addProperty("payloadType", "LINEARTEXTPAYLOAD")
            }
            is ListPayload -> context.serialize(src).asJsonObject.apply {
                addProperty("payloadType", "LISTPAYLOAD")
            }
        }
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ObjectPayload {
        if (json == null || context == null) {
            return TextPayload("")
        }

        val obj = json.asJsonObject
        val type = obj.get("payloadType")?.asString

        return when (type) {
            "TEXT" -> context.deserialize(obj, TextPayload::class.java)
            "IMAGE" -> context.deserialize(obj, ImagePayload::class.java)
            "DRAWING" -> context.deserialize(obj, DrawingPayload::class.java)
            "CHECKLIST" -> context.deserialize(obj, ChecklistPayload::class.java)
            "TEXTSPAN" -> context.deserialize(obj, TextSpan::class.java)
            "LINEARTEXTPAYLOAD" -> context.deserialize(obj, LinearTextPayload::class.java)
            "LISTPAYLOAD" -> context.deserialize(obj, ListPayload::class.java)
            else -> deserializeLegacyPayload(obj, context)
        }
    }

    private fun deserializeLegacyPayload(
        obj: com.google.gson.JsonObject,
        context: JsonDeserializationContext
    ): ObjectPayload {
        val firstItem = obj.getAsJsonArray("items")?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject

        return when {
            obj.has("uri") -> context.deserialize(obj, ImagePayload::class.java)
            obj.has("strokes") -> context.deserialize(obj, DrawingPayload::class.java)
            obj.has("items") && firstItem?.has("isChecked") == true ->
                context.deserialize(obj, ChecklistPayload::class.java)
            obj.has("listStyle") || obj.has("items") -> context.deserialize(obj, ListPayload::class.java)
            obj.has("start") && obj.has("end") -> context.deserialize(obj, TextSpan::class.java)
            obj.has("text") -> context.deserialize(obj, TextPayload::class.java)
            else -> context.deserialize(obj, TextPayload::class.java)
        }
    }
}
