// port-lint: tests tests.rs
package io.github.kotlinmania.oauth2

import io.github.kotlinmania.thiserror.StdError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    companion object {
        fun fromString(s: String): ColorfulTokenType =
            when (s.lowercase()) {
                "green" -> Green
                "red" -> Red
                else -> throw IllegalArgumentException("Unknown ColorfulTokenType: $s")
            }
    }
}

internal data class ColorfulFields(
    val shape: String? = null,
    val height: Int,
) : ExtraTokenFields {
    fun shape(): String? = shape

    fun height(): Int = height

    override fun toJsonFields(): Map<String, JsonElement> =
        buildMap {
            if (shape != null) {
                put("shape", JsonPrimitive(shape))
            }
            put("height", JsonPrimitive(height))
        }

    companion object {
        fun fromJsonObject(json: JsonObject): ColorfulFields {
            val shape = json["shape"]?.jsonPrimitive?.takeIf { it.isString }?.contentOrNull
            val height = requireNotNull(json["height"]?.jsonPrimitive?.intOrNull) { "Missing height" }
            return ColorfulFields(shape = shape, height = height)
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

    companion object {
        fun fromString(s: String): ColorfulErrorResponseType =
            when (s) {
                "too_dark" -> TooDark
                "too_light" -> TooLight
                "wrong_color_space" -> WrongColorSpace
                else -> throw IllegalArgumentException("Unknown ColorfulErrorResponseType: $s")
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
                val json = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                val errCode = json["error"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val errDesc = json["error_description"]?.jsonPrimitive?.contentOrNull
                val errUri = json["error_uri"]?.jsonPrimitive?.contentOrNull
                StandardErrorResponse.new(
                    ColorfulErrorResponseType.fromString(errCode),
                    errDesc,
                    errUri,
                )
            },
            introspectionResponseDeserializer = { bytes ->
                val json = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
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
                    extraFields = ColorfulFields.fromJsonObject(json),
                )
            },
            revocationErrorDeserializer = { bytes ->
                val json = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                val errCode = json["error"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val errDesc = json["error_description"]?.jsonPrimitive?.contentOrNull
                val errUri = json["error_uri"]?.jsonPrimitive?.contentOrNull
                StandardErrorResponse.new(
                    ColorfulErrorResponseType.fromString(errCode),
                    errDesc,
                    errUri,
                )
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
