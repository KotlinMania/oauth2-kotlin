// port-lint: tests tests.rs
package io.github.kotlinmania.oauth2

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.IgnoredAny
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.JsonMap
import io.github.kotlinmania.serdejson.Value
import io.github.kotlinmania.serdejson.fromSlice
import io.github.kotlinmania.thiserror.StdError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class FakeError :
    Exception("error"),
    StdError

internal enum class ColorfulTokenType(
    override val value: String,
) : TokenType {
    Green("green"),
    Red("red"),
    ;

    override fun toString(): String = value

    companion object : Deserialize<ColorfulTokenType> {
        fun fromStr(s: String): ColorfulTokenType =
            when (s.lowercase()) {
                "green" -> Green
                "red" -> Red
                else -> throw IllegalArgumentException("Unknown ColorfulTokenType: $s")
            }

        fun fromString(s: String): ColorfulTokenType = fromStr(s)

        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<ColorfulTokenType> {
            val s = StringDeserialize.deserialize(deserializer).getOrThrow()
            return SerdeResult.success(fromStr(s))
        }
    }
}

internal data class ColorfulFields(
    val shape: String? = null,
    val height: Int,
) : ExtraTokenFields {
    fun shape(): String? = shape

    fun height(): Int = height

    override fun <Ok> appendFields(struct: SerializeStruct<Ok>) {
        if (shape != null) {
            struct.serializeField("shape", shape)
        }
        struct.serializeField("height", height)
    }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        val count = (if (shape != null) 1 else 0) + 1
        val struct = serializer.serializeStruct("ColorfulFields", count).getOrThrow()
        appendFields(struct)
        return struct.end()
    }

    companion object : Deserialize<ColorfulFields> {
        fun fromJsonObject(obj: Value.Object): ColorfulFields {
            val shape = (obj.get("shape") as? Value.Str)?.value
            val height =
                (obj.get("height") as? Value.Number)?.value?.asI64()?.toInt()
                    ?: throw IllegalArgumentException("Missing height")
            return ColorfulFields(shape = shape, height = height)
        }

        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<ColorfulFields> {
            class VisitorImpl : Visitor<ColorfulFields> {
                override fun expecting(): String = "struct ColorfulFields"

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<ColorfulFields> {
                    var shape: String? = null
                    var height: Int? = null
                    while (true) {
                        val key = access.nextKey(StringDeserialize).getOrThrow() ?: break
                        when (key) {
                            "shape" -> shape = access.nextValue(StringDeserialize).getOrThrow()
                            "height" -> height = access.nextValue(I32Deserialize).getOrThrow()
                            else -> access.nextValue(IgnoredAny).getOrThrow()
                        }
                    }
                    val h = height ?: return SerdeResult.failure(SerdeError.missingField("height"))
                    return SerdeResult.success(ColorfulFields(shape = shape, height = h))
                }
            }
            return deserializer.deserializeStruct("ColorfulFields", listOf("shape", "height"), VisitorImpl())
        }
    }
}

internal enum class ColorfulErrorResponseType(
    override val value: String,
) : ErrorResponseType {
    TooDark("too_dark"),
    TooLight("too_light"),
    WrongColorSpace("wrong_color_space"),
    ;

    override fun toString(): String = value

    companion object : Deserialize<ColorfulErrorResponseType> {
        fun fromStr(s: String): ColorfulErrorResponseType =
            when (s) {
                "too_dark" -> TooDark
                "too_light" -> TooLight
                "wrong_color_space" -> WrongColorSpace
                else -> throw IllegalArgumentException("Unknown ColorfulErrorResponseType: $s")
            }

        fun fromString(s: String): ColorfulErrorResponseType = fromStr(s)

        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<ColorfulErrorResponseType> {
            val s = StringDeserialize.deserialize(deserializer).getOrThrow()
            return SerdeResult.success(fromStr(s))
        }
    }
}

internal typealias ColorfulTokenResponse = StandardTokenResponse<ColorfulFields, ColorfulTokenType>

internal sealed class ColorfulRevocableToken : RevocableToken {
    data class Red(
        val tokenSecret: String,
    ) : ColorfulRevocableToken() {
        override fun secret(): String = tokenSecret

        override fun typeHint(): String? = "red_token"
    }
}

internal typealias ColorfulClient = Client<
    StandardErrorResponse<ColorfulErrorResponseType>,
    ColorfulTokenResponse,
    StandardTokenIntrospectionResponse<ColorfulFields, ColorfulTokenType>,
    ColorfulRevocableToken,
    StandardErrorResponse<ColorfulErrorResponseType>,
>

internal object ColorfulClientFactory {
    fun new(clientId: ClientId): ColorfulClient =
        Client(
            clientId = clientId,
            tokenResponseDeserializer = { bytes ->
                StandardTokenResponse.fromJsonString(
                    jsonString = bytes.decodeToString(),
                    tokenTypeFactory = { ColorfulTokenType.fromString(it) },
                    extraFieldsFactory = { ColorfulFields.fromJsonObject(it) },
                )
            },
            tokenErrorDeserializer = { bytes ->
                StandardErrorResponse.fromJsonString(bytes.decodeToString()) {
                    ColorfulErrorResponseType.fromString(it)
                }
            },
            introspectionResponseDeserializer = { bytes ->
                val outcome = fromSlice(bytes).getOrThrow()
                val json = outcome.asObject() ?: JsonMap()
                val base =
                    StandardTokenIntrospectionResponse.fromJsonString(bytes.decodeToString()) {
                        ColorfulTokenType.fromString(it)
                    }
                StandardTokenIntrospectionResponse(
                    active = base.active(),
                    scopes = base.scopes(),
                    clientId = base.clientId(),
                    username = base.username(),
                    tokenType = base.tokenType(),
                    exp = base.exp(),
                    iat = base.iat(),
                    nbf = base.nbf(),
                    sub = base.sub(),
                    aud = base.aud(),
                    iss = base.iss(),
                    jti = base.jti(),
                    extraFields = ColorfulFields.fromJsonObject(Value.Object(json)),
                )
            },
            revocationErrorDeserializer = { bytes ->
                StandardErrorResponse.fromJsonString(bytes.decodeToString()) {
                    ColorfulErrorResponseType.fromString(it)
                }
            },
        )
}

class Tests {
    @Test
    fun testNewClient() {
        val client =
            BasicClientFactory
                .new(ClientId.new("aaa"))
                .setAuthUri(AuthUrl.new("https://example.com/auth"))
                .setTokenUri(TokenUrl.new("https://example.com/token"))
                .setClientSecret(ClientSecret.new("bbb"))

        assertEquals(ClientId.new("aaa"), client.clientId())
        assertEquals(AuthUrl.new("https://example.com/auth"), client.authUrl())
        assertEquals(TokenUrl.new("https://example.com/token"), client.tokenUrl())
        assertEquals(ClientSecret.new("bbb"), client.clientSecret())
    }

    // Upstream test_send_sync_impl asserts Rust compile-time Send + Sync marker traits;
    // in Kotlin Multiplatform we verify all public protocol types are instantiable.
    @Test
    fun testAllTypesInstantiable() {
        assertNotNull(AccessToken.new("token"))
        assertNotNull(AuthUrl.new("https://example.com/auth"))
        assertNotNull(AuthorizationCode.new("code"))
        assertNotNull(ClientId.new("client_id"))
        assertNotNull(ClientSecret.new("secret"))
        assertNotNull(CsrfToken.new("csrf"))
        assertNotNull(EmptyExtraTokenFields())
        assertNotNull(HttpRequest(url = "https://example.com", body = ByteArray(0)))
        assertNotNull(HttpResponse(status = 200, headers = emptyMap(), body = ByteArray(0)))
        assertNotNull(PkceCodeChallenge.new("chal"))
        assertNotNull(PkceCodeChallengeMethod.S256)
        assertNotNull(PkceCodeVerifier.new("verif"))
        assertNotNull(RedirectUrl.new("https://example.com/redirect"))
        assertNotNull(RefreshToken.new("refresh"))
        assertNotNull(ResourceOwnerPassword.new("pwd"))
        assertNotNull(ResourceOwnerUsername.new("user"))
        assertNotNull(ResponseType.new("code"))
        assertNotNull(Scope.new("scope"))
        assertNotNull(TokenUrl.new("https://example.com/token"))
        assertNotNull(DeviceCode.new("code"))
        assertNotNull(EndUserVerificationUrl.new("https://example.com/verify"))
        assertNotNull(UserCode.new("user_code"))
        assertNotNull(DeviceAuthorizationUrl.new("https://example.com/device"))
        assertNotNull(EmptyExtraDeviceAuthorizationFields())
    }
}
