// port-lint: source oauth2/src/code.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlin.native.HiddenFromObjC

/** A request to the authorization endpoint. */
public class AuthorizationRequest(
    private val authUrl: AuthUrl,
    private val clientId: ClientId,
    private val state: CsrfToken,
) {
    private val extraParams = mutableListOf<Pair<String, String>>()
    private var pkceChallenge: PkceCodeChallenge? = null
    private var redirectUrl: RedirectUrl? = null
    private var responseType: String = "code"
    private val scopes = mutableListOf<Scope>()

    public fun addScope(scope: Scope): AuthorizationRequest = apply {
        scopes.add(scope)
    }

    public fun addScopes(scopes: Iterable<Scope>): AuthorizationRequest = apply {
        this.scopes.addAll(scopes)
    }

    public fun addExtraParam(name: String, value: String): AuthorizationRequest = apply {
        extraParams.add(name to value)
    }

    public fun useImplicitFlow(): AuthorizationRequest = apply {
        this.responseType = "token"
    }

    public fun setResponseType(responseType: ResponseType): AuthorizationRequest = apply {
        this.responseType = responseType.value
    }

    public fun setPkceChallenge(pkceCodeChallenge: PkceCodeChallenge): AuthorizationRequest = apply {
        this.pkceChallenge = pkceCodeChallenge
    }

    public fun setRedirectUri(redirectUrl: RedirectUrl): AuthorizationRequest = apply {
        this.redirectUrl = redirectUrl
    }

    public fun url(): Pair<String, CsrfToken> {
        val pairs = mutableListOf(
            "response_type" to responseType,
            "client_id" to clientId.value,
            "state" to state.secret(),
        )

        pkceChallenge?.let {
            pairs.add("code_challenge" to it.asStr())
            pairs.add("code_challenge_method" to it.method().asStr())
        }

        redirectUrl?.let {
            pairs.add("redirect_uri" to it.value)
        }

        if (scopes.isNotEmpty()) {
            pairs.add("scope" to scopes.joinToString(" ") { it.value })
        }

        pairs.addAll(extraParams)

        val queryString = pairs.joinToString("&") { (k, v) ->
            "${formUrlEncode(k)}=${formUrlEncode(v)}"
        }

        val baseUrl = authUrl.url()
        val fullUrl = if (baseUrl.contains("?")) {
            if (baseUrl.endsWith("?") || baseUrl.endsWith("&")) "$baseUrl$queryString"
            else "$baseUrl&$queryString"
        } else {
            "$baseUrl?$queryString"
        }

        return Pair(fullUrl, state)
    }
}
