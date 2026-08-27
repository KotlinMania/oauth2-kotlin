// port-lint: source basic.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlin.native.HiddenFromObjC
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Basic OAuth2 authorization token types. */
public sealed class BasicTokenType : TokenType {
    /** Bearer token. */
    public data object Bearer : BasicTokenType() {
        override val value: String = "bearer"
    }

    /** MAC token. */
    public data object Mac : BasicTokenType() {
        override val value: String = "mac"
    }

    /** An extension not defined by RFC 6749. */
    public data class Extension(
        public val rawValue: String,
    ) : BasicTokenType() {
        override val value: String = rawValue
    }

    override fun toString(): String = value

    public companion object {
        public fun fromString(value: String): BasicTokenType =
            when (value.lowercase()) {
                "bearer" -> Bearer
                "mac" -> Mac
                else -> Extension(value)
            }
    }
}

/** Basic access token error types. */
public sealed class BasicErrorResponseType : ErrorResponseType {
    /** Client authentication failed. */
    public data object InvalidClient : BasicErrorResponseType()

    /** The provided authorization grant or refresh token is invalid. */
    public data object InvalidGrant : BasicErrorResponseType()

    /** The request is missing a required parameter or is otherwise malformed. */
    public data object InvalidRequest : BasicErrorResponseType()

    /** The requested scope is invalid, unknown, malformed, or exceeds the granted scope. */
    public data object InvalidScope : BasicErrorResponseType()

    /** The authenticated client is not authorized to use this authorization grant type. */
    public data object UnauthorizedClient : BasicErrorResponseType()

    /** The authorization grant type is not supported by the authorization server. */
    public data object UnsupportedGrantType : BasicErrorResponseType()

    /** An extension not defined by RFC 6749. */
    public data class Extension(
        public val rawValue: String,
    ) : BasicErrorResponseType()

    override val value: String
        get() =
            when (this) {
                InvalidClient -> "invalid_client"
                InvalidGrant -> "invalid_grant"
                InvalidRequest -> "invalid_request"
                InvalidScope -> "invalid_scope"
                UnauthorizedClient -> "unauthorized_client"
                UnsupportedGrantType -> "unsupported_grant_type"
                is Extension -> rawValue
            }

    override fun toString(): String = value

    public companion object {
        public fun fromString(value: String): BasicErrorResponseType =
            when (value) {
                "invalid_client" -> InvalidClient
                "invalid_grant" -> InvalidGrant
                "invalid_request" -> InvalidRequest
                "invalid_scope" -> InvalidScope
                "unauthorized_client" -> UnauthorizedClient
                "unsupported_grant_type" -> UnsupportedGrantType
                else -> Extension(value)
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
                StandardTokenResponse.fromJsonString(bytes.decodeToString()) { BasicTokenType.fromString(it) }
            },
            tokenErrorDeserializer = { bytes ->
                val json = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                val errCode = json["error"]?.jsonPrimitive?.contentOrNull ?: ""
                val errDesc = json["error_description"]?.jsonPrimitive?.contentOrNull
                val errUri = json["error_uri"]?.jsonPrimitive?.contentOrNull
                BasicErrorResponse.new(BasicErrorResponseType.fromString(errCode), errDesc, errUri)
            },
            introspectionResponseDeserializer = { bytes ->
                StandardTokenIntrospectionResponse.fromJsonString(bytes.decodeToString()) {
                    BasicTokenType.fromString(it)
                }
            },
            revocationErrorDeserializer = { bytes ->
                val json = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                val errCode = json["error"]?.jsonPrimitive?.contentOrNull ?: ""
                val errDesc = json["error_description"]?.jsonPrimitive?.contentOrNull
                val errUri = json["error_uri"]?.jsonPrimitive?.contentOrNull
                BasicRevocationErrorResponse.new(RevocationErrorResponseType.fromString(errCode), errDesc, errUri)
            },
        )
}
