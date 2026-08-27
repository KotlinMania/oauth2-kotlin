// port-lint: source oauth2/src/helpers.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Case-insensitive untagged enum parser. */
public fun <T> deserializeUntaggedEnumCaseInsensitive(
    value: String,
    fromString: (String) -> T,
): T = fromString(value.lowercase())

/** Space-delimited string deserializer for a list of strings. */
public fun deserializeSpaceDelimitedVec(value: String?): List<String> {
    if (value == null) return emptyList()
    return value.split(' ').filter { it.isNotEmpty() }
}

/** Deserializes a string or array of strings into a list of strings. */
public fun deserializeOptionalStringOrVecString(element: JsonElement?): List<String>? {
    if (element == null || element is JsonNull) return null
    return when (element) {
        is JsonPrimitive -> {
            if (element.isString) listOf(element.content)
            else emptyList()
        }
        is JsonArray -> {
            element.map { it.jsonPrimitive.content }
        }
        else -> null
    }
}

/** Space-delimited string serializer for an optional list of items. */
public fun <T> serializeSpaceDelimitedVec(items: List<T>?, toString: (T) -> String = { it.toString() }): String? {
    if (items == null) return null
    return items.joinToString(" ") { toString(it) }
}

/** Returns the lowercase/snake_case variant name for an object/enum. */
public fun <T : Any> variantName(instance: T): String =
    instance::class.simpleName?.lowercase() ?: ""
