// port-lint: source revocation.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.ser.Serializer
import kotlin.native.HiddenFromObjC

/**
 * Trait representing a token that can be revoked.
 */
public interface RevocableToken {
    public fun secret(): String

    public fun typeHint(): String?
}

/**
 * A token representation usable with authorization servers that support RFC 7009 token revocation.
 */
public sealed class StandardRevocableToken : RevocableToken {
    /** A representation of an AccessToken. */
    public data class AccessToken(
        public val token: io.github.kotlinmania.oauth2.AccessToken,
    ) : StandardRevocableToken() {
        override fun secret(): String = token.secret()

        override fun typeHint(): String? = "access_token"
    }

    /** A representation of a RefreshToken. */
    public data class RefreshToken(
        public val token: io.github.kotlinmania.oauth2.RefreshToken,
    ) : StandardRevocableToken() {
        override fun secret(): String = token.secret()

        override fun typeHint(): String? = "refresh_token"
    }

    public companion object {
        public fun from(token: io.github.kotlinmania.oauth2.AccessToken): StandardRevocableToken =
            AccessToken(token)

        public fun from(token: io.github.kotlinmania.oauth2.RefreshToken): StandardRevocableToken =
            RefreshToken(token)
    }
}

/**
 * OAuth 2.0 Token Revocation error response types.
 */
public sealed class RevocationErrorResponseType : ErrorResponseType {
    /** The authorization server does not support the revocation of the presented token type. */
    public data object UnsupportedTokenType : RevocationErrorResponseType() {
        override val value: String get() = "unsupported_token_type"

        override fun toString(): String = value
    }

    /** The authorization server responded with some other error as defined in RFC 6749. */
    public data class Basic(
        public val error: BasicErrorResponseType,
    ) : RevocationErrorResponseType() {
        override val value: String get() = error.asRef()

        override fun toString(): String = value
    }

    public fun asRef(): String =
        when (this) {
            UnsupportedTokenType -> "unsupported_token_type"
            is Basic -> error.asRef()
        }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStr(asRef())

    override fun toString(): String = asRef()

    public companion object : Deserialize<RevocationErrorResponseType> {
        public fun fromStr(s: String): RevocationErrorResponseType =
            if (s == "unsupported_token_type") {
                UnsupportedTokenType
            } else {
                Basic(BasicErrorResponseType.fromStr(s))
            }

        public fun fromString(s: String): RevocationErrorResponseType = fromStr(s)

        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<RevocationErrorResponseType> {
            val variantStr = StringDeserialize.deserialize(deserializer).getOrThrow()
            return SerdeResult.success(fromStr(variantStr))
        }
    }
}

/** A request to revoke a token via an RFC 7009 compatible endpoint. */
@HiddenFromObjC
public class RevocationRequest<RT : RevocableToken, TE : ErrorResponse>(
    private val authType: AuthType,
    private val clientId: ClientId,
    private val clientSecret: ClientSecret?,
    private val revocationUrl: RevocationUrl,
    private val token: RT,
    private val errorDeserializer: (ByteArray) -> TE,
) {
    private val extraParams = mutableListOf<Pair<String, String>>()

    public fun addExtraParam(name: String, value: String): RevocationRequest<RT, TE> =
        apply {
            extraParams.add(name to value)
        }

    public fun prepareRequest(): HttpRequest {
        val params = mutableListOf("token" to token.secret())
        token.typeHint()?.let {
            params.add("token_type_hint" to it)
        }
        return endpointRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            extraParams = extraParams,
            redirectUrl = null,
            scopes = null,
            url = revocationUrl.url(),
            params = params,
        )
    }

    public fun request(httpClient: SyncHttpClient) {
        val request = prepareRequest()
        val response = invokeSyncHttpClient(httpClient, request)
        endpointResponseStatusOnly(response, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient) {
        val request = prepareRequest()
        val response = invokeAsyncHttpClient(httpClient, request)
        endpointResponseStatusOnly(response, errorDeserializer)
    }
}
