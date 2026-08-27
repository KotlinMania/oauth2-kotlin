// port-lint: tests helpers.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

class HelpersTest {
    @Test
    fun testDeserializeOptionalStringOrVecStringNone() {
        val result = deserializeOptionalStringOrVecString(JsonNull)
        assertNull(result)
    }

    @Test
    fun testDeserializeOptionalStringOrVecStringSingleValue() {
        val result = deserializeOptionalStringOrVecString(JsonPrimitive("v1"))
        assertEquals(listOf("v1"), result)
    }

    @Test
    fun testDeserializeOptionalStringOrVecStringVec() {
        val array = buildJsonArray {
            add(JsonPrimitive("v1"))
            add(JsonPrimitive("v2"))
        }
        val result = deserializeOptionalStringOrVecString(array)
        assertEquals(listOf("v1", "v2"), result)
    }

    @Test
    fun testSpaceDelimitedVec() {
        val list = deserializeSpaceDelimitedVec("foo  bar baz")
        assertEquals(listOf("foo", "bar", "baz"), list)

        val serialized = serializeSpaceDelimitedVec(listOf("foo", "bar", "baz"))
        assertEquals("foo bar baz", serialized)
    }
}
