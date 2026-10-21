// port-lint: tests helpers.rs
package io.github.kotlinmania.oauth2

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.IgnoredAny
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdejson.fromStr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private object OptionalStringOrVecStringDeserialize : Deserialize<List<String>?> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<List<String>?> =
        deserializeOptionalStringOrVecString(deserializer)
}

class HelpersTest {
    data class ObjectWithOptionalStringOrVecString(
        val strings: List<String>?,
    ) {
        companion object : Deserialize<ObjectWithOptionalStringOrVecString> {
            override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<ObjectWithOptionalStringOrVecString> {
                class StructVisitor : Visitor<ObjectWithOptionalStringOrVecString> {
                    override fun expecting(): String = "struct ObjectWithOptionalStringOrVecString"

                    override fun <A : MapAccess> visitMap(access: A): SerdeResult<ObjectWithOptionalStringOrVecString> {
                        var strings: List<String>? = null
                        while (true) {
                            val key = access.nextKey(StringDeserialize).getOrThrow() ?: break
                            if (key == "strings") {
                                strings = access.nextValue(OptionalStringOrVecStringDeserialize).getOrThrow()
                            } else {
                                access.nextValue(IgnoredAny).getOrThrow()
                            }
                        }
                        return SerdeResult.success(ObjectWithOptionalStringOrVecString(strings))
                    }
                }
                return deserializer.deserializeStruct(
                    "ObjectWithOptionalStringOrVecString",
                    listOf("strings"),
                    StructVisitor(),
                )
            }
        }
    }

    @Test
    fun testDeserializeOptionalStringOrVecStringNone() {
        val listOfStrings = fromStr("""{ "strings": null }""", ObjectWithOptionalStringOrVecString).getOrThrow()
        assertNull(listOfStrings.strings)
    }

    @Test
    fun testDeserializeOptionalStringOrVecStringSingleValue() {
        val listOfStrings = fromStr("""{ "strings": "v1" }""", ObjectWithOptionalStringOrVecString).getOrThrow()
        assertEquals(listOf("v1"), listOfStrings.strings)
    }

    @Test
    fun testDeserializeOptionalStringOrVecStringVec() {
        val listOfStrings = fromStr("""{ "strings": ["v1", "v2"] }""", ObjectWithOptionalStringOrVecString).getOrThrow()
        assertEquals(listOf("v1", "v2"), listOfStrings.strings)
    }

    @Test
    fun testSpaceDelimitedVec() {
        val list = deserializeSpaceDelimitedVec("foo bar baz")
        assertEquals(listOf("foo", "bar", "baz"), list)

        val serialized = serializeSpaceDelimitedVec(listOf("foo", "bar", "baz"))
        assertEquals("foo bar baz", serialized)
    }
}
