package com.mato.syai.note.data.local.parser

import com.google.gson.*
import com.mato.syai.note.domain.local.model.*
import java.lang.reflect.Type

class ObjectPayloadAdapter : JsonSerializer<ObjectPayload>, JsonDeserializer<ObjectPayload> {

    override fun serialize(
        src: ObjectPayload?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        if (src == null || context == null) return JsonNull.INSTANCE

        val jsonObject = when (src) {
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
        }

        return jsonObject
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

            // 🔥 BACKWARD COMPAT (important)
            else -> {
                when {
                    obj.has("text") -> context.deserialize(obj, TextPayload::class.java)
                    obj.has("uri") -> context.deserialize(obj, ImagePayload::class.java)
                    obj.has("strokes") -> context.deserialize(obj, DrawingPayload::class.java)
                    else -> TextPayload("")
                }
            }
        }
    }
}