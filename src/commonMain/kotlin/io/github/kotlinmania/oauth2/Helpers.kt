// port-lint: source helpers.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeMap
import io.github.kotlinmania.serdecore.ser.SerializeSeq
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.SerializeStructVariant
import io.github.kotlinmania.serdecore.ser.SerializeTuple
import io.github.kotlinmania.serdecore.ser.SerializeTupleStruct
import io.github.kotlinmania.serdecore.ser.SerializeTupleVariant
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.Value
import io.github.kotlinmania.serdejson.deserialize

/**
 * Serde case-insensitive deserializer for an untagged `enum`.
 *
 * This function converts values to lowercase before deserializing as the `enum`.
 */
public fun <T, D : Deserializer> deserializeUntaggedEnumCaseInsensitive(
    deserializer: D,
    deserialize: Deserialize<T>,
): SerdeResult<T> {
    val s = StringDeserialize.deserialize(deserializer).getOrThrow()
    return Value.Str(s.lowercase()).deserialize(deserialize)
}

/**
 * Serde space-delimited string deserializer for a `List<String>`.
 *
 * This function splits a JSON string at each space character into a `List<String>`.
 */
public fun <T, D : Deserializer> deserializeSpaceDelimitedVec(
    deserializer: D,
    deserialize: Deserialize<T>,
    default: () -> T,
): SerdeResult<T> {
    val spaceDelimited = nullableDeserialize(StringDeserialize).deserialize(deserializer).getOrThrow()
    return if (spaceDelimited != null) {
        val entries = spaceDelimited.split(' ').map { Value.Str(it) }
        Value.Array(entries).deserialize(deserialize)
    } else {
        SerdeResult.success(default())
    }
}

/**
 * Overload for parsing a space-delimited string directly.
 */
public fun deserializeSpaceDelimitedVec(value: String?): List<String> {
    if (value == null) return emptyList()
    return value.split(' ').filter { it.isNotEmpty() }
}

/**
 * Deserializes a string or array of strings into an array of strings.
 */
public fun <D : Deserializer> deserializeOptionalStringOrVecString(
    deserializer: D,
): SerdeResult<List<String>?> {
    class StringOrVec : Visitor<List<String>?> {
        override fun expecting(): String = "string or list of strings"

        override fun visitStr(v: String): SerdeResult<List<String>?> =
            SerdeResult.success(listOf(v))

        override fun visitString(v: String): SerdeResult<List<String>?> =
            SerdeResult.success(listOf(v))

        override fun visitNone(): SerdeResult<List<String>?> =
            SerdeResult.success(null)

        override fun visitUnit(): SerdeResult<List<String>?> =
            SerdeResult.success(null)

        override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<List<String>?> {
            val list = mutableListOf<String>()
            while (true) {
                val element = access.nextElement(StringDeserialize).getOrThrow() ?: break
                list.add(element)
            }
            return SerdeResult.success(list)
        }
    }

    return deserializer.deserializeAny(StringOrVec())
}

/**
 * Serde space-delimited string serializer for an optional list of items.
 *
 * This function serializes a string vector into a single space-delimited string.
 */
public fun <T, Ok> serializeSpaceDelimitedVec(
    vecOpt: List<T>?,
    serializer: Serializer<Ok>,
    asRef: (T) -> String = { it.toString() },
): SerdeResult<Ok> =
    if (vecOpt != null) {
        val spaceDelimited = vecOpt.joinToString(" ") { asRef(it) }
        serializer.serializeStr(spaceDelimited)
    } else {
        serializer.serializeNone()
    }

/**
 * Overload for space-delimited string serialization returning String?.
 */
public fun <T> serializeSpaceDelimitedVec(
    items: List<T>?,
    toString: (T) -> String = { it.toString() },
): String? {
    if (items == null) return null
    return items.joinToString(" ") { toString(it) }
}

internal class NotEnum(
    message: String = "not struct",
) : Exception(message)

/**
 * Serde string serializer for an enum variant name.
 */
public fun <T : Serialize> variantName(t: T): String {
    class Enum(
        val name: String,
    ) : SerializeStructVariant<String>,
        SerializeTupleVariant<String> {
        override fun <V : Serialize> serializeField(key: String, value: V): SerdeResult<Unit> =
            SerdeResult.success(Unit)

        override fun <V : Serialize> serializeField(value: V): SerdeResult<Unit> =
            SerdeResult.success(Unit)

        override fun end(): SerdeResult<String> =
            SerdeResult.success(name)
    }

    class VariantName : Serializer<String> {
        override fun serializeBool(v: Boolean): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeI8(v: Byte): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeI16(v: Short): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeI32(v: Int): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeI64(v: Long): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeU8(v: UByte): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeU16(v: UShort): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeU32(v: UInt): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeU64(v: ULong): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeF32(v: Float): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeF64(v: Double): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeChar(v: Char): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeStr(v: String): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeBytes(v: ByteArray): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeNone(): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun <V : Serialize> serializeSome(value: V): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeUnit(): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeUnitStruct(name: String): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeUnitVariant(
            name: String,
            variantIndex: UInt,
            variant: String,
        ): SerdeResult<String> = SerdeResult.success(variant)

        override fun <V : Serialize> serializeNewtypeStruct(name: String, value: V): SerdeResult<String> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun <V : Serialize> serializeNewtypeVariant(
            name: String,
            variantIndex: UInt,
            variant: String,
            value: V,
        ): SerdeResult<String> = SerdeResult.success(variant)

        override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<String>> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<String>> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<String>> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeTupleVariant(
            name: String,
            variantIndex: UInt,
            variant: String,
            len: Int,
        ): SerdeResult<SerializeTupleVariant<String>> = SerdeResult.success(Enum(variant))

        override fun serializeMap(len: Int?): SerdeResult<SerializeMap<String>> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<String>> =
            SerdeResult.failure(SerdeError.custom("not struct"))

        override fun serializeStructVariant(
            name: String,
            variantIndex: UInt,
            variant: String,
            len: Int,
        ): SerdeResult<SerializeStructVariant<String>> = SerdeResult.success(Enum(variant))
    }

    return t.serialize(VariantName()).getOrThrow()
}

public class SerdeStr(
    public val value: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(value)
}

public class SerdeI64(
    public val value: Long,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI64(value)
}

public class SerdeBool(
    public val value: Boolean,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeBool(value)
}

public class SerdeInt(
    public val value: Int,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeI32(value)
}

public fun <Ok> SerializeStruct<Ok>.serializeField(key: String, value: String): SerdeResult<Unit> =
    serializeField(key, SerdeStr(value))

public fun <Ok> SerializeStruct<Ok>.serializeField(key: String, value: Long): SerdeResult<Unit> =
    serializeField(key, SerdeI64(value))

public fun <Ok> SerializeStruct<Ok>.serializeField(key: String, value: Int): SerdeResult<Unit> =
    serializeField(key, SerdeInt(value))

public fun <Ok> SerializeStruct<Ok>.serializeField(key: String, value: Boolean): SerdeResult<Unit> =
    serializeField(key, SerdeBool(value))
