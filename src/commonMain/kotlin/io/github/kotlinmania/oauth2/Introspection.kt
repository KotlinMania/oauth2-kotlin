// port-lint: source introspection.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.BooleanDeserialize
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.IgnoredAny
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.fromStr
import io.github.kotlinmania.serdejson.toStr
import kotlin.native.HiddenFromObjC

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
) : TokenIntrospectionResponse,
    Serialize {
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

    public fun setActive(active: Boolean) {
        this.active = active
    }

    public fun setScopes(scopes: List<Scope>?) {
        this.scopes = scopes
    }

    public fun setClientId(clientId: ClientId?) {
        this.clientId = clientId
    }

    public fun setUsername(username: String?) {
        this.username = username
    }

    public fun setTokenType(tokenType: TT?) {
        this.tokenType = tokenType
    }

    public fun setExp(exp: Long?) {
        this.exp = exp
    }

    public fun setIat(iat: Long?) {
        this.iat = iat
    }

    public fun setNbf(nbf: Long?) {
        this.nbf = nbf
    }

    public fun setSub(sub: String?) {
        this.sub = sub
    }

    public fun setAud(aud: List<String>?) {
        this.aud = aud
    }

    public fun setIss(iss: String?) {
        this.iss = iss
    }

    public fun setJti(jti: String?) {
        this.jti = jti
    }

    public fun setExtraFields(extraFields: EF) {
        this.extraFields = extraFields
    }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        val sc = scopes
        val cid = clientId
        val un = username
        val tt = tokenType
        val e = exp
        val i = iat
        val nb = nbf
        val sb = sub
        val au = aud
        val is_ = iss
        val jt = jti
        var count = 1
        if (sc != null) count++
        if (cid != null) count++
        if (un != null) count++
        if (tt != null) count++
        if (e != null) count++
        if (i != null) count++
        if (nb != null) count++
        if (sb != null) count++
        if (au != null) count++
        if (is_ != null) count++
        if (jt != null) count++
        val struct = serializer.serializeStruct("StandardTokenIntrospectionResponse", count).getOrThrow()
        struct.serializeField("active", active).getOrThrow()
        if (sc != null) {
            struct.serializeField("scope", sc.joinToString(" ") { it.value }).getOrThrow()
        }
        if (cid != null) {
            struct.serializeField("client_id", cid.value).getOrThrow()
        }
        if (un != null) {
            struct.serializeField("username", un).getOrThrow()
        }
        if (tt != null) {
            struct.serializeField("token_type", tt).getOrThrow()
        }
        if (e != null) {
            struct.serializeField("exp", e).getOrThrow()
        }
        if (i != null) {
            struct.serializeField("iat", i).getOrThrow()
        }
        if (nb != null) {
            struct.serializeField("nbf", nb).getOrThrow()
        }
        if (sb != null) {
            struct.serializeField("sub", sb).getOrThrow()
        }
        if (au != null) {
            if (au.size == 1) {
                struct.serializeField("aud", au[0]).getOrThrow()
            } else {
                struct.serializeField("aud", au.joinToString(" ")).getOrThrow()
            }
        }
        if (is_ != null) {
            struct.serializeField("iss", is_).getOrThrow()
        }
        if (jt != null) {
            struct.serializeField("jti", jt).getOrThrow()
        }
        extraFields.appendFields(struct)
        return struct.end()
    }

    public fun toJsonString(): String = toStr(this).getOrThrow()

    public class DeserializerImpl<EF : ExtraTokenFields, TT : TokenType>(
        private val extraFieldsDeserialize: Deserialize<EF>,
        private val tokenTypeDeserialize: Deserialize<TT>,
    ) : Deserialize<StandardTokenIntrospectionResponse<EF, TT>> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<StandardTokenIntrospectionResponse<EF, TT>> {
            class VisitorImpl : Visitor<StandardTokenIntrospectionResponse<EF, TT>> {
                override fun expecting(): String = "struct StandardTokenIntrospectionResponse"

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<StandardTokenIntrospectionResponse<EF, TT>> {
                    var active: Boolean? = null
                    var scopes: List<Scope>? = null
                    var clientId: ClientId? = null
                    var username: String? = null
                    var tokenType: TT? = null
                    var exp: Long? = null
                    var iat: Long? = null
                    var nbf: Long? = null
                    var sub: String? = null
                    var aud: List<String>? = null
                    var iss: String? = null
                    var jti: String? = null

                    while (true) {
                        val key = access.nextKey(StringDeserialize).getOrThrow() ?: break
                        when (key) {
                            "active" -> {
                                active = access.nextValue(BooleanDeserialize).getOrThrow()
                            }

                            "scope" -> {
                                val s = access.nextValue(StringDeserialize).getOrThrow()
                                scopes = deserializeSpaceDelimitedVec(s).map { Scope.new(it) }
                            }

                            "client_id" -> {
                                val s = access.nextValue(StringDeserialize).getOrThrow()
                                clientId = ClientId.new(s)
                            }

                            "username" -> {
                                username = access.nextValue(StringDeserialize).getOrThrow()
                            }

                            "token_type" -> {
                                tokenType = access.nextValue(tokenTypeDeserialize).getOrThrow()
                            }

                            "exp" -> {
                                exp = access.nextValue(I64Deserialize).getOrThrow()
                            }

                            "iat" -> {
                                iat = access.nextValue(I64Deserialize).getOrThrow()
                            }

                            "nbf" -> {
                                nbf = access.nextValue(I64Deserialize).getOrThrow()
                            }

                            "sub" -> {
                                sub = access.nextValue(StringDeserialize).getOrThrow()
                            }

                            "aud" -> {
                                aud =
                                    access
                                        .nextValue(
                                            object : Deserialize<List<String>?> {
                                                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<List<String>?> =
                                                    deserializeOptionalStringOrVecString(deserializer)
                                            },
                                        ).getOrThrow()
                            }

                            "iss" -> {
                                iss = access.nextValue(StringDeserialize).getOrThrow()
                            }

                            "jti" -> {
                                jti = access.nextValue(StringDeserialize).getOrThrow()
                            }

                            else -> {
                                access.nextValue(IgnoredAny).getOrThrow()
                            }
                        }
                    }
                    val act = active ?: return SerdeResult.failure(SerdeError.missingField("active"))
                    val ef = extraFieldsDeserialize.deserialize(deserializer).getOrThrow()
                    return SerdeResult.success(
                        StandardTokenIntrospectionResponse(
                            active = act,
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
                            extraFields = ef,
                        ),
                    )
                }
            }
            return deserializer.deserializeStruct(
                "StandardTokenIntrospectionResponse",
                listOf("active", "scope", "client_id", "username", "token_type", "exp", "iat", "nbf", "sub", "aud", "iss", "jti"),
                VisitorImpl(),
            )
        }
    }

    public companion object {
        public fun <EF : ExtraTokenFields, TT : TokenType> new(
            active: Boolean,
            extraFields: EF,
        ): StandardTokenIntrospectionResponse<EF, TT> =
            StandardTokenIntrospectionResponse(
                active = active,
                extraFields = extraFields,
            )

        public fun <EF : ExtraTokenFields, TT : TokenType> deserializer(
            extraFieldsDeserialize: Deserialize<EF>,
            tokenTypeDeserialize: Deserialize<TT>,
        ): Deserialize<StandardTokenIntrospectionResponse<EF, TT>> =
            DeserializerImpl(extraFieldsDeserialize, tokenTypeDeserialize)

        public fun <TT : TokenType> fromJsonString(
            jsonString: String,
            tokenTypeFactory: (String) -> TT,
        ): StandardTokenIntrospectionResponse<EmptyExtraTokenFields, TT> {
            val ttDe =
                object : Deserialize<TT> {
                    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<TT> {
                        val s = StringDeserialize.deserialize(deserializer).getOrThrow()
                        return SerdeResult.success(tokenTypeFactory(s))
                    }
                }
            return fromStr(jsonString, deserializer(EmptyExtraTokenFields, ttDe)).getOrThrow()
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

    public fun setTokenTypeHint(value: String): IntrospectionRequest<TE, TIR> =
        apply {
            this.tokenTypeHint = value
        }

    public fun addExtraParam(name: String, value: String): IntrospectionRequest<TE, TIR> =
        apply {
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
        val response = invokeSyncHttpClient(httpClient, request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }

    public suspend fun requestAsync(httpClient: AsyncHttpClient): TIR {
        val request = prepareRequest()
        val response = invokeAsyncHttpClient(httpClient, request)
        return endpointResponse(response, responseDeserializer, errorDeserializer)
    }
}
