// port-lint: source introspection.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlin.native.HiddenFromObjC
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Common methods shared by all OAuth2 token introspection implementations.
 */
public interface TokenIntrospectionResponse {
    /** REQUIRED. Boolean indicator of whether or not the presented token is currently active. */
    public fun active(): Boolean

    /** OPTIONAL. A list of scopes for this token. */
    public fun scopes(): List<Scope>?

    /** OPTIONAL. Client identifier for the OAuth 2.0 client that requested this token. */
    public fun clientId(): ClientId?

    /** OPTIONAL. Human-readable identifier for the resource owner who authorized this token. */
    public fun username(): String?

    /** OPTIONAL. Type of the token. */
    public fun tokenType(): TokenType?

    /** OPTIONAL. Integer timestamp, measured in the number of seconds, indicating when this token will expire. */
    public fun exp(): Long?

    /** OPTIONAL. Integer timestamp, measured in the number of seconds, indicating when this token was originally issued. */
    public fun iat(): Long?

    /** OPTIONAL. Integer timestamp, measured in the number of seconds, indicating when this token is not to be used before. */
    public fun nbf(): Long?

    /** OPTIONAL. Subject of the token. */
    public fun sub(): String?

    /** OPTIONAL. List of intended audiences for the token. */
    public fun aud(): List<String>?

    /** OPTIONAL. String representing the issuer of this token. */
    public fun iss(): String?

    /** OPTIONAL. String identifier for the token. */
    public fun jti(): String?
}

/** Standard implementation of [TokenIntrospectionResponse]. */
@HiddenFromObjC
public class StandardTokenIntrospectionResponse<EF : ExtraTokenFields, TT : TokenType>(
    private var active: Boolean,
    private var scopes: List<Scope>? = null,
    private var clientId: ClientId? = null,
    private var username: String? = null,
    private var tokenType: TT? = null,
    private var exp: Long? = null,
    private var iat: Long? = null,
    private var nbf: Long? = null,
    private var sub: String? = null,
    private var aud: List<String>? = null,
    private var iss: String? = null,
    private var jti: String? = null,
    private var extraFields: EF,
) : TokenIntrospectionResponse {
    override fun active(): Boolean = active
    override fun scopes(): List<Scope>? = scopes
    override fun clientId(): ClientId? = clientId
    override fun username(): String? = username
    override fun tokenType(): TT? = tokenType
    override fun exp(): Long? = exp
    override fun iat(): Long? = iat
    override fun nbf(): Long? = nbf
    override fun sub(): String? = sub
    override fun aud(): List<String>? = aud
    override fun iss(): String? = iss
    override fun jti(): String? = jti

    public fun extraFields(): EF = extraFields

    public fun setActive(active: Boolean) { this.active = active }
    public fun setScopes(scopes: List<Scope>?) { this.scopes = scopes }
    public fun setClientId(clientId: ClientId?) { this.clientId = clientId }
    public fun setUsername(username: String?) { this.username = username }
    public fun setTokenType(tokenType: TT?) { this.tokenType = tokenType }
    public fun setExp(exp: Long?) { this.exp = exp }
    public fun setIat(iat: Long?) { this.iat = iat }
    public fun setNbf(nbf: Long?) { this.nbf = nbf }
    public fun setSub(sub: String?) { this.sub = sub }
    public fun setAud(aud: List<String>?) { this.aud = aud }
    public fun setIss(iss: String?) { this.iss = iss }
    public fun setJti(jti: String?) { this.jti = jti }
    public fun setExtraFields(extraFields: EF) { this.extraFields = extraFields }

    public companion object {
        public fun <EF : ExtraTokenFields, TT : TokenType> new(
            active: Boolean,
            extraFields: EF,
        ): StandardTokenIntrospectionResponse<EF, TT> =
            StandardTokenIntrospectionResponse(
                active = active,
                extraFields = extraFields,
            )

        public fun <TT : TokenType> fromJsonString(
            jsonString: String,
            tokenTypeFactory: (String) -> TT,
        ): StandardTokenIntrospectionResponse<EmptyExtraTokenFields, TT> {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val active = requireNotNull(json["active"]?.jsonPrimitive?.booleanOrNull) { "Missing active" }
            val scopes: List<Scope>? = json["scope"]?.jsonPrimitive?.contentOrNull?.let { scopeStr ->
                deserializeSpaceDelimitedVec(scopeStr).map { Scope(it) }
            }
            val clientId: ClientId? = json["client_id"]?.jsonPrimitive?.contentOrNull?.let { ClientId(it) }
            val username = json["username"]?.jsonPrimitive?.contentOrNull
            val tokenType: TT? = json["token_type"]?.jsonPrimitive?.contentOrNull?.let { tokenTypeFactory(it) }
            val exp = json["exp"]?.jsonPrimitive?.longOrNull
            val iat = json["iat"]?.jsonPrimitive?.longOrNull
            val nbf = json["nbf"]?.jsonPrimitive?.longOrNull
            val sub = json["sub"]?.jsonPrimitive?.contentOrNull
            val aud = deserializeOptionalStringOrVecString(json["aud"])
            val iss = json["iss"]?.jsonPrimitive?.contentOrNull
            val jti = json["jti"]?.jsonPrimitive?.contentOrNull

            return StandardTokenIntrospectionResponse(
                active = active,
                scopes = scopes,
                clientId = clientId,
                username = username,
                tokenType = tokenType,
                exp = exp,
                iat = iat,
                nbf = nbf,
                sub = sub,
                aud = aud,
                iss = iss,
                jti = jti,
                extraFields = EmptyExtraTokenFields(),
            )
        }
    }
}

/** A request to introspect an access token. */
@HiddenFromObjC
public class IntrospectionRequest<TE : ErrorResponse, TIR : TokenIntrospectionResponse>(
    private val authType: AuthType,
    private val clientId: ClientId,
    private val clientSecret: ClientSecret?,
    private val introspectionUrl: IntrospectionUrl,
    private val token: AccessToken,
    private val responseDeserializer: (ByteArray) -> TIR,
    private val errorDeserializer: (ByteArray) -> TE,
) {
    private var tokenTypeHint: String? = null
    private val extraParams = mutableListOf<Pair<String, String>>()

    public fun setTokenTypeHint(value: String): IntrospectionRequest<TE, TIR> = apply {
        this.tokenTypeHint = value
    }

    public fun addExtraParam(name: String, value: String): IntrospectionRequest<TE, TIR> = apply {
        extraParams.add(name to value)
    }

    public fun prepareRequest(): HttpRequest {
        val params = mutableListOf("token" to token.secret())
        tokenTypeHint?.let {
            params.add("token_type_hint" to it)
        }
        return endpointRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            extraParams = extraParams,
            redirectUrl = null,
            scopes = null,
            url = introspectionUrl.value,
            params = params,
        )
    }

    public fun request(httpClient: SyncHttpClient): TIR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TIR {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }
}
