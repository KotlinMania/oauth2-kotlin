// port-lint: source token/mod.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.native.HiddenFromObjC
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Type of OAuth2 access token. */
public interface TokenType {
    public val value: String
}

/** Trait for adding extra fields to the TokenResponse. */
public interface ExtraTokenFields

/** Empty (default) extra token fields. */
public class EmptyExtraTokenFields : ExtraTokenFields {
    override fun equals(other: Any?): Boolean = other is EmptyExtraTokenFields

    override fun hashCode(): Int = 0

    override fun toString(): String = "EmptyExtraTokenFields"
}

/**
 * Common methods shared by all OAuth2 token implementations.
 *
 * The methods in this interface are defined in Section 5.1 of RFC 6749.
 */
public interface TokenResponse {
    /** REQUIRED. The access token issued by the authorization server. */
    public fun accessToken(): AccessToken

    /**
     * REQUIRED. The type of the token issued as described in Section 7.1.
     * Value is case insensitive.
     */
    public fun tokenType(): TokenType

    /**
     * RECOMMENDED. The lifetime of the access token. If omitted, the authorization server
     * SHOULD provide the expiration time via other means or document the default value.
     */
    public fun expiresIn(): Duration?

    /**
     * OPTIONAL. The refresh token, which can be used to obtain new access tokens using the same
     * authorization grant as described in Section 6.
     */
    public fun refreshToken(): RefreshToken?

    /**
     * OPTIONAL, if identical to the scope requested by the client; otherwise, REQUIRED. The
     * scope of the access token as described by Section 3.3.
     */
    public fun scopes(): List<Scope>?
}

/** Standard implementation of [TokenResponse]. */
@HiddenFromObjC
public class StandardTokenResponse<EF : ExtraTokenFields, TT : TokenType>(
    private var accessToken: AccessToken,
    private var tokenType: TT,
    private var expiresInSeconds: Long? = null,
    private var refreshToken: RefreshToken? = null,
    private var scopes: List<Scope>? = null,
    private var extraFields: EF,
) : TokenResponse {
    override fun accessToken(): AccessToken = accessToken

    override fun tokenType(): TT = tokenType

    override fun expiresIn(): Duration? = expiresInSeconds?.seconds

    override fun refreshToken(): RefreshToken? = refreshToken

    override fun scopes(): List<Scope>? = scopes

    public fun extraFields(): EF = extraFields

    public fun setAccessToken(accessToken: AccessToken) {
        this.accessToken = accessToken
    }

    public fun setTokenType(tokenType: TT) {
        this.tokenType = tokenType
    }

    public fun setExpiresIn(duration: Duration?) {
        this.expiresInSeconds = duration?.inWholeSeconds
    }

    public fun setRefreshToken(refreshToken: RefreshToken?) {
        this.refreshToken = refreshToken
    }

    public fun setScopes(scopes: List<Scope>?) {
        this.scopes = scopes
    }

    public fun setExtraFields(extraFields: EF) {
        this.extraFields = extraFields
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is StandardTokenResponse<*, *> &&
            accessToken.secret() == other.accessToken.secret() &&
            tokenType == other.tokenType &&
            expiresInSeconds == other.expiresInSeconds &&
            refreshToken?.secret() == other.refreshToken?.secret() &&
            scopes == other.scopes &&
            extraFields == other.extraFields

    override fun hashCode(): Int {
        var result = accessToken.hashCode()
        result = 31 * result + tokenType.hashCode()
        result = 31 * result + (expiresInSeconds?.hashCode() ?: 0)
        result = 31 * result + (refreshToken?.hashCode() ?: 0)
        result = 31 * result + (scopes?.hashCode() ?: 0)
        result = 31 * result + extraFields.hashCode()
        return result
    }

    public companion object {
        public fun <EF : ExtraTokenFields, TT : TokenType> new(
            accessToken: AccessToken,
            tokenType: TT,
            extraFields: EF,
        ): StandardTokenResponse<EF, TT> =
            StandardTokenResponse(
                accessToken = accessToken,
                tokenType = tokenType,
                extraFields = extraFields,
            )

        public fun <TT : TokenType> fromJsonString(
            jsonString: String,
            tokenTypeFactory: (String) -> TT,
        ): StandardTokenResponse<EmptyExtraTokenFields, TT> {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val accessToken =
                AccessToken.new(
                    requireNotNull(json["access_token"]?.jsonPrimitive?.contentOrNull) { "Missing access_token" },
                )
            val tokenTypeStr = requireNotNull(json["token_type"]?.jsonPrimitive?.contentOrNull) { "Missing token_type" }
            val tokenType = tokenTypeFactory(tokenTypeStr)
            val expiresIn = json["expires_in"]?.jsonPrimitive?.longOrNull
            val refreshToken = json["refresh_token"]?.jsonPrimitive?.contentOrNull?.let { RefreshToken(it) }
            val scopes: List<Scope>? =
                json["scope"]?.jsonPrimitive?.contentOrNull?.let { scopeStr ->
                    deserializeSpaceDelimitedVec(scopeStr).map { Scope(it) }
                }

            return StandardTokenResponse(
                accessToken = accessToken,
                tokenType = tokenType,
                expiresInSeconds = expiresIn,
                refreshToken = refreshToken,
                scopes = scopes,
                extraFields = EmptyExtraTokenFields(),
            )
        }
    }
}

/** A request to exchange an authorization code for an access token. */
@HiddenFromObjC
public class CodeTokenRequest<TE : ErrorResponse, TR : TokenResponse>(
    private val authType: AuthType,
    private val clientId: ClientId,
    private val clientSecret: ClientSecret?,
    private val code: AuthorizationCode,
    private val tokenUrl: TokenUrl,
    private var redirectUrl: RedirectUrl? = null,
    private val responseDeserializer: (ByteArray) -> TR,
    private val errorDeserializer: (ByteArray) -> TE,
) {
    private val extraParams = mutableListOf<Pair<String, String>>()
    private var pkceVerifier: PkceCodeVerifier? = null

    public fun addExtraParam(name: String, value: String): CodeTokenRequest<TE, TR> =
        apply {
            extraParams.add(name to value)
        }

    public fun setPkceVerifier(pkceVerifier: PkceCodeVerifier): CodeTokenRequest<TE, TR> =
        apply {
            this.pkceVerifier = pkceVerifier
        }

    public fun setRedirectUri(redirectUrl: RedirectUrl): CodeTokenRequest<TE, TR> =
        apply {
            this.redirectUrl = redirectUrl
        }

    public fun prepareRequest(): HttpRequest {
        val params =
            mutableListOf(
                "grant_type" to "authorization_code",
                "code" to code.secret(),
            )
        pkceVerifier?.let {
            params.add("code_verifier" to it.secret())
        }
        return endpointRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            extraParams = extraParams,
            redirectUrl = redirectUrl,
            scopes = null,
            url = tokenUrl.value,
            params = params,
        )
    }

    public fun request(httpClient: SyncHttpClient): TR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }
}

/** A request to exchange a refresh token for an access token. */
@HiddenFromObjC
public class RefreshTokenRequest<TE : ErrorResponse, TR : TokenResponse>(
    private val authType: AuthType,
    private val clientId: ClientId,
    private val clientSecret: ClientSecret?,
    private val refreshToken: RefreshToken,
    private val tokenUrl: TokenUrl,
    private val responseDeserializer: (ByteArray) -> TR,
    private val errorDeserializer: (ByteArray) -> TE,
) {
    private val extraParams = mutableListOf<Pair<String, String>>()
    private val scopes = mutableListOf<Scope>()

    public fun addExtraParam(name: String, value: String): RefreshTokenRequest<TE, TR> =
        apply {
            extraParams.add(name to value)
        }

    public fun addScope(scope: Scope): RefreshTokenRequest<TE, TR> =
        apply {
            scopes.add(scope)
        }

    public fun addScopes(scopes: Iterable<Scope>): RefreshTokenRequest<TE, TR> =
        apply {
            this.scopes.addAll(scopes)
        }

    public fun prepareRequest(): HttpRequest {
        val params =
            mutableListOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken.secret(),
            )
        return endpointRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            extraParams = extraParams,
            redirectUrl = null,
            scopes = if (scopes.isNotEmpty()) scopes else null,
            url = tokenUrl.value,
            params = params,
        )
    }

    public fun request(httpClient: SyncHttpClient): TR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }
}

/** A request to exchange resource owner credentials for an access token. */
@HiddenFromObjC
public class PasswordTokenRequest<TE : ErrorResponse, TR : TokenResponse>(
    private val authType: AuthType,
    private val clientId: ClientId,
    private val clientSecret: ClientSecret?,
    private val username: ResourceOwnerUsername,
    private val password: ResourceOwnerPassword,
    private val tokenUrl: TokenUrl,
    private val responseDeserializer: (ByteArray) -> TR,
    private val errorDeserializer: (ByteArray) -> TE,
) {
    private val extraParams = mutableListOf<Pair<String, String>>()
    private val scopes = mutableListOf<Scope>()

    public fun addExtraParam(name: String, value: String): PasswordTokenRequest<TE, TR> =
        apply {
            extraParams.add(name to value)
        }

    public fun addScope(scope: Scope): PasswordTokenRequest<TE, TR> =
        apply {
            scopes.add(scope)
        }

    public fun addScopes(scopes: Iterable<Scope>): PasswordTokenRequest<TE, TR> =
        apply {
            this.scopes.addAll(scopes)
        }

    public fun prepareRequest(): HttpRequest {
        val params =
            mutableListOf(
                "grant_type" to "password",
                "username" to username.value,
                "password" to password.secret(),
            )
        return endpointRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            extraParams = extraParams,
            redirectUrl = null,
            scopes = if (scopes.isNotEmpty()) scopes else null,
            url = tokenUrl.value,
            params = params,
        )
    }

    public fun request(httpClient: SyncHttpClient): TR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }
}

/** A request to exchange client credentials for an access token. */
@HiddenFromObjC
public class ClientCredentialsTokenRequest<TE : ErrorResponse, TR : TokenResponse>(
    private val authType: AuthType,
    private val clientId: ClientId,
    private val clientSecret: ClientSecret?,
    private val tokenUrl: TokenUrl,
    private val responseDeserializer: (ByteArray) -> TR,
    private val errorDeserializer: (ByteArray) -> TE,
) {
    private val extraParams = mutableListOf<Pair<String, String>>()
    private val scopes = mutableListOf<Scope>()

    public fun addExtraParam(name: String, value: String): ClientCredentialsTokenRequest<TE, TR> =
        apply {
            extraParams.add(name to value)
        }

    public fun addScope(scope: Scope): ClientCredentialsTokenRequest<TE, TR> =
        apply {
            scopes.add(scope)
        }

    public fun addScopes(scopes: Iterable<Scope>): ClientCredentialsTokenRequest<TE, TR> =
        apply {
            this.scopes.addAll(scopes)
        }

    public fun prepareRequest(): HttpRequest {
        val params =
            mutableListOf(
                "grant_type" to "client_credentials",
            )
        return endpointRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            extraParams = extraParams,
            redirectUrl = null,
            scopes = if (scopes.isNotEmpty()) scopes else null,
            url = tokenUrl.value,
            params = params,
        )
    }

    public fun request(httpClient: SyncHttpClient): TR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }
}
