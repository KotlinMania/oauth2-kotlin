@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.IgnoredAny
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.JsonMap
import io.github.kotlinmania.serdejson.Value
import io.github.kotlinmania.serdejson.deserialize
import io.github.kotlinmania.serdejson.fromStr
import io.github.kotlinmania.serdejson.toStr
import kotlin.native.HiddenFromObjC
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Type of OAuth2 access token. */
public interface TokenType : Serialize {
    public val value: String

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStr(value)
}

/** Trait for adding extra fields to the TokenResponse. */
public interface ExtraTokenFields : Serialize {
    public fun <Ok> appendFields(struct: SerializeStruct<Ok>) {}
}

/** Empty (default) extra token fields. */
public class EmptyExtraTokenFields : ExtraTokenFields {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        val s = serializer.serializeStruct("EmptyExtraTokenFields", 0).getOrThrow()
        return s.end()
    }

    override fun equals(other: Any?): Boolean = other is EmptyExtraTokenFields

    override fun hashCode(): Int = this::class.hashCode()

    override fun toString(): String = "EmptyExtraTokenFields"

    public companion object : Deserialize<EmptyExtraTokenFields> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<EmptyExtraTokenFields> =
            SerdeResult.success(EmptyExtraTokenFields())
    }
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
     * RECOMMENDED. The validity duration of the access token. If omitted, the authorization server
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
) : TokenResponse,
    Serialize {
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

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        val exp = expiresInSeconds
        val rt = refreshToken
        val sc = scopes
        var count = 2
        if (exp != null) count++
        if (rt != null) count++
        if (sc != null) count++
        val struct = serializer.serializeStruct("StandardTokenResponse", count).getOrThrow()
        struct.serializeField("access_token", accessToken.secret()).getOrThrow()
        struct.serializeField("token_type", tokenType).getOrThrow()
        if (exp != null) {
            struct.serializeField("expires_in", exp).getOrThrow()
        }
        if (rt != null) {
            struct.serializeField("refresh_token", rt.secret()).getOrThrow()
        }
        if (sc != null) {
            struct.serializeField("scope", sc.joinToString(" ") { it.value }).getOrThrow()
        }
        extraFields.appendFields(struct)
        return struct.end()
    }

    public fun toJsonString(): String = toStr(this).getOrThrow()

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is StandardTokenResponse<*, *> &&
                    accessToken.secret() == other.accessToken.secret() &&
                    tokenType == other.tokenType &&
                    expiresInSeconds == other.expiresInSeconds &&
                    refreshToken?.secret() == other.refreshToken?.secret() &&
                    scopes == other.scopes &&
                    extraFields == other.extraFields
            )

    override fun hashCode(): Int {
        var result = accessToken.hashCode()
        result = 31 * result + tokenType.hashCode()
        result = 31 * result + (expiresInSeconds?.hashCode() ?: 0)
        result = 31 * result + (refreshToken?.hashCode() ?: 0)
        result = 31 * result + (scopes?.hashCode() ?: 0)
        result = 31 * result + extraFields.hashCode()
        return result
    }

    public class DeserializerImpl<EF : ExtraTokenFields, TT : TokenType>(
        private val tokenTypeDeserialize: Deserialize<TT>,
        private val extraFieldsDeserialize: Deserialize<EF>,
    ) : Deserialize<StandardTokenResponse<EF, TT>> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<StandardTokenResponse<EF, TT>> {
            class VisitorImpl : Visitor<StandardTokenResponse<EF, TT>> {
                override fun expecting(): String = "struct StandardTokenResponse"

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<StandardTokenResponse<EF, TT>> {
                    var accessToken: AccessToken? = null
                    var tokenType: TT? = null
                    var expiresInSeconds: Long? = null
                    var refreshToken: RefreshToken? = null
                    var scopes: List<Scope>? = null

                    while (true) {
                        val key = access.nextKey(StringDeserialize).getOrThrow() ?: break
                        when (key) {
                            "access_token" -> {
                                val tokenStr =
                                    try {
                                        access.nextValue(StringDeserialize).getOrThrow()
                                    } catch (e: Throwable) {
                                        throw IllegalArgumentException("failed to parse field 'access_token': ${e.message}", e)
                                    }
                                accessToken = AccessToken.new(tokenStr)
                            }

                            "token_type" -> {
                                val s =
                                    try {
                                        access.nextValue(StringDeserialize).getOrThrow()
                                    } catch (e: Throwable) {
                                        throw IllegalArgumentException("failed to parse field 'token_type': ${e.message}", e)
                                    }
                                tokenType = Value.Str(s.lowercase()).deserialize(tokenTypeDeserialize).getOrThrow()
                            }

                            "expires_in" -> {
                                expiresInSeconds =
                                    try {
                                        access.nextValue(I64Deserialize).getOrThrow()
                                    } catch (e: Throwable) {
                                        throw IllegalArgumentException("failed to parse field 'expires_in': ${e.message}", e)
                                    }
                            }

                            "refresh_token" -> {
                                val rtStr =
                                    try {
                                        access.nextValue(StringDeserialize).getOrThrow()
                                    } catch (e: Throwable) {
                                        throw IllegalArgumentException("failed to parse field 'refresh_token': ${e.message}", e)
                                    }
                                refreshToken = RefreshToken.new(rtStr)
                            }

                            "scope" -> {
                                val scopeStr =
                                    try {
                                        access.nextValue(StringDeserialize).getOrThrow()
                                    } catch (e: Throwable) {
                                        throw IllegalArgumentException("failed to parse field 'scope': ${e.message}", e)
                                    }
                                scopes = deserializeSpaceDelimitedVec(scopeStr).map { Scope.new(it) }
                            }

                            else -> {
                                access.nextValue(IgnoredAny).getOrThrow()
                            }
                        }
                    }
                    val at = accessToken ?: return SerdeResult.failure(SerdeError.missingField("access_token"))
                    val tt = tokenType ?: return SerdeResult.failure(SerdeError.missingField("token_type"))
                    val ef = extraFieldsDeserialize.deserialize(deserializer).getOrThrow()
                    return SerdeResult.success(
                        StandardTokenResponse(
                            accessToken = at,
                            tokenType = tt,
                            expiresInSeconds = expiresInSeconds,
                            refreshToken = refreshToken,
                            scopes = scopes,
                            extraFields = ef,
                        ),
                    )
                }
            }
            return deserializer.deserializeStruct(
                "StandardTokenResponse",
                listOf("access_token", "token_type", "expires_in", "refresh_token", "scope"),
                VisitorImpl(),
            )
        }
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

        public fun <EF : ExtraTokenFields, TT : TokenType> deserializer(
            tokenTypeDeserialize: Deserialize<TT>,
            extraFieldsDeserialize: Deserialize<EF>,
        ): Deserialize<StandardTokenResponse<EF, TT>> = DeserializerImpl(tokenTypeDeserialize, extraFieldsDeserialize)

        public fun <TT : TokenType> fromJsonString(
            jsonString: String,
            tokenTypeFactory: (String) -> TT,
        ): StandardTokenResponse<EmptyExtraTokenFields, TT> =
            fromJsonString(jsonString, tokenTypeFactory) { EmptyExtraTokenFields() }

        public fun <EF : ExtraTokenFields, TT : TokenType> fromJsonString(
            jsonString: String,
            tokenTypeFactory: (String) -> TT,
            extraFieldsFactory: (Value.Object) -> EF,
        ): StandardTokenResponse<EF, TT> {
            val value = fromStr(jsonString).getOrThrow()
            val obj = value as? Value.Object ?: Value.Object(JsonMap())
            val ef = extraFieldsFactory(obj)
            val ttDe =
                object : Deserialize<TT> {
                    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<TT> {
                        val s = StringDeserialize.deserialize(deserializer).getOrThrow()
                        return SerdeResult.success(tokenTypeFactory(s))
                    }
                }
            val efDe =
                object : Deserialize<EF> {
                    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<EF> =
                        SerdeResult.success(ef)
                }
            return fromStr(jsonString, deserializer(ttDe, efDe)).getOrThrow()
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
        val response = invokeSyncHttpClient(httpClient, request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TR {
        val request = prepareRequest()
        val response = invokeAsyncHttpClient(httpClient, request)
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
        val response = invokeSyncHttpClient(httpClient, request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TR {
        val request = prepareRequest()
        val response = invokeAsyncHttpClient(httpClient, request)
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
        val response = invokeSyncHttpClient(httpClient, request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TR {
        val request = prepareRequest()
        val response = invokeAsyncHttpClient(httpClient, request)
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
        val response = invokeSyncHttpClient(httpClient, request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TR {
        val request = prepareRequest()
        val response = invokeAsyncHttpClient(httpClient, request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }
}
