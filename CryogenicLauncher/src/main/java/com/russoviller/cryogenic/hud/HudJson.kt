package com.russoviller.cryogenic.hud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/** Utilitários de JSON tolerantes (arquivos de outros launchers podem vir incompletos). */
internal object HudJson {
    val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    fun parseObject(text: String): JsonObject {
        val clean = text.trimStart('﻿', ' ', '\n', '\r', '\t')
        return json.parseToJsonElement(clean) as? JsonObject
            ?: throw IllegalArgumentException("Not a JSON object")
    }

    fun encode(obj: JsonObject): String = json.encodeToString(JsonObject.serializer(), obj)
}

internal fun JsonObject.prim(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }

internal fun JsonObject.str(key: String, def: String = ""): String =
    prim(key)?.let { if (it.isString) it.content else it.content } ?: def

internal fun JsonObject.int(key: String, def: Int = 0): Int =
    prim(key)?.let { it.intOrNull ?: it.longOrNull?.toInt() ?: it.doubleOrNull?.toInt() ?: it.content.toIntOrNull() } ?: def

internal fun JsonObject.float(key: String, def: Float = 0f): Float =
    prim(key)?.let { it.doubleOrNull?.toFloat() ?: it.content.toFloatOrNull() } ?: def

internal fun JsonObject.bool(key: String, def: Boolean = false): Boolean =
    prim(key)?.let { it.booleanOrNull ?: it.content.toBooleanStrictOrNull() } ?: def

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonObject.objects(key: String): List<JsonObject> = arr(key)?.mapNotNull { it as? JsonObject } ?: emptyList()

internal fun JsonObject.ints(key: String): List<Int> = arr(key)?.mapNotNull { e ->
    (e as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }
} ?: emptyList()

internal fun JsonObject.strings(key: String): List<String> = arr(key)?.mapNotNull { e ->
    (e as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
} ?: emptyList()

/** Copia o objeto trocando/adicionando chaves. */
internal fun JsonObject.with(vararg pairs: Pair<String, JsonElement>): JsonObject =
    JsonObject(this.toMutableMap().apply { pairs.forEach { (k, v) -> put(k, v) } })

internal fun jp(v: String) = JsonPrimitive(v)
internal fun jp(v: Number) = JsonPrimitive(v)
internal fun jp(v: Boolean) = JsonPrimitive(v)
internal fun jarr(list: List<JsonElement>) = JsonArray(list)
internal fun jobj(vararg pairs: Pair<String, JsonElement>) = JsonObject(linkedMapOf(*pairs))
