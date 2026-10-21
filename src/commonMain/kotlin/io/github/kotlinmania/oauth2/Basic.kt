// port-lint: source basic.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.fromSlice

/** Basic OAuth2 authorization token types. */
public sealed class BasicTokenType : TokenType {
    /** Bearer token. */
    public data object Bearer : BasicTokenType() {
        override val value: String = "bearer"

        override fun toString(): String = value
    }

    /** MAC token. */
    public data object Mac : BasicTokenType() {
        override val value: String = "mac"

        override fun toString(): String = value
    }

    /** An extension not defined by RFC 6749. */
    public data class Extension(
        public val rawValue: String,
    ) : BasicTokenType() {
        override val value: String = rawValue

        override fun toString(): String = value
    }

    public fun asRef(): String =
        when (this) {
            Bearer -> "bearer"
            Mac -> "mac"
            is Extension -> rawValue
        }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStr(asRef())

    override fun toString(): String = asRef()

    public companion object : Deserialize<BasicTokenType> {
        public fun fromStr(s: String): BasicTokenType =
            when (s) {
                "bearer" -> Bearer
                "mac" -> Mac
                else -> Extension(s)
            }

        public fun fromString(value: String): BasicTokenType = fromStr(value.lowercase())

        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<BasicTokenType> {
            val variantStr = StringDeserialize.deserialize(deserializer).getOrThrow()
            return SerdeResult.success(fromStr(variantStr))
        }
    }
}

/** Basic access token error types. */
public sealed class BasicErrorResponseType : ErrorResponseType {
    /** Client authentication failed. */
    public data object InvalidClient : BasicErrorResponseType() {
        override fun toString(): String = value
    }

    /** The provided authorization grant or refresh token is invalid. */
    public data object InvalidGrant : BasicErrorResponseType() {
        override fun toString(): String = value
    }

    /** The request is missing a required parameter or is otherwise malformed. */
    public data object InvalidRequest : BasicErrorResponseType() {
        override fun toString(): String = value
    }

    /** The requested scope is invalid, unknown, malformed, or exceeds the granted scope. */
    public data object InvalidScope : BasicErrorResponseType() {
        override fun toString(): String = value
    }

    /** The authenticated client is not authorized to use this authorization grant type. */
    public data object UnauthorizedClient : BasicErrorResponseType() {
        override fun toString(): String = value
    }

    /** The authorization grant type is not supported by the authorization server. */
    public data object UnsupportedGrantType : BasicErrorResponseType() {
        override fun toString(): String = value
    }

    /** An extension not defined by RFC 6749. */
    public data class Extension(
        public val rawValue: String,
    ) : BasicErrorResponseType() {
        override fun toString(): String = value
    }

    public fun asRef(): String =
        when (this) {
            InvalidClient -> "invalid_client"
            InvalidGrant -> "invalid_grant"
            InvalidRequest -> "invalid_request"
            InvalidScope -> "invalid_scope"
            UnauthorizedClient -> "unauthorized_client"
            UnsupportedGrantType -> "unsupported_grant_type"
            is Extension -> rawValue
        }

    override val value: String
        get() = asRef()

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStr(asRef())

    override fun toString(): String = asRef()

    public companion object : Deserialize<BasicErrorResponseType> {
        public fun fromStr(s: String): BasicErrorResponseType =
            when (s) {
                "invalid_client" -> InvalidClient
                "invalid_grant" -> InvalidGrant
                "invalid_request" -> InvalidRequest
                "invalid_scope" -> InvalidScope
                "unauthorized_client" -> UnauthorizedClient
                "unsupported_grant_type" -> UnsupportedGrantType
                else -> Extension(s)
            }

        public fun fromString(value: String): BasicErrorResponseType = fromStr(value)

        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<BasicErrorResponseType> {
            val variantStr = StringDeserialize.deserialize(deserializer).getOrThrow()
            return SerdeResult.success(fromStr(variantStr))
        }
    }
}

/** Error response specialization for basic OAuth2 implementation. */
public typealias BasicErrorResponse = StandardErrorResponse<BasicErrorResponseType>

/** Revocation error response specialization for basic OAuth2 implementation. */
public typealias BasicRevocationErrorResponse = StandardErrorResponse<RevocationErrorResponseType>

/** Basic OAuth2 token response. */
public typealias BasicTokenResponse = StandardTokenResponse<EmptyExtraTokenFields, BasicTokenType>

/** Basic OAuth2 token introspection response. */
public typealias BasicTokenIntrospectionResponse =
    StandardTokenIntrospectionResponse<EmptyExtraTokenFields, BasicTokenType>

/** Token error specialization for basic OAuth2 implementation. */
public typealias BasicRequestTokenError = RequestTokenError

/** Basic OAuth2 client specialization, suitable for most applications. */
public typealias BasicClient = Client<
    BasicErrorResponse,
    BasicTokenResponse,
    BasicTokenIntrospectionResponse,
    StandardRevocableToken,
    BasicRevocationErrorResponse,
>

public object BasicClientFactory {
    public fun new(clientId: ClientId): BasicClient =
        Client(
            clientId = clientId,
            tokenResponseDeserializer = { bytes ->
                fromSlice(
                    bytes,
                    StandardTokenResponse.deserializer(BasicTokenType, EmptyExtraTokenFields),
                ).getOrThrow()
            },
            tokenErrorDeserializer = { bytes ->
                fromSlice(
                    bytes,
                    StandardErrorResponse.deserializer(BasicErrorResponseType),
                ).getOrThrow()
            },
            introspectionResponseDeserializer = { bytes ->
                fromSlice(
                    bytes,
                    StandardTokenIntrospectionResponse.deserializer(EmptyExtraTokenFields, BasicTokenType),
                ).getOrThrow()
            },
            revocationErrorDeserializer = { bytes ->
                fromSlice(
                    bytes,
                    StandardErrorResponse.deserializer(RevocationErrorResponseType),
                ).getOrThrow()
            },
        )
}
