// port-lint: tests code.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeTest {
    private fun newClient(): BasicClient =
        BasicClientFactory.new(ClientId.new("aaa"))
            .setAuthUri(AuthUrl.new("https://example.com/auth"))
            .setTokenUri(TokenUrl.new("https://example.com/token"))
            .setClientSecret(ClientSecret.new("bbb"))

    @Test
    fun testAuthorizeUrl() {
        val client = newClient()
        val (url, _) = client
            .authorizeUrl { CsrfToken.new("csrf_token") }
            .url()

        assertEquals(
            "https://example.com/auth?response_type=code&client_id=aaa&state=csrf_token",
            url
        )
    }

    @Test
    fun testAuthorizeRandom() {
        val client = newClient()
        val (url, csrfState) = client.authorizeUrl { CsrfToken.newRandom() }.url()

        assertTrue(url.startsWith("https://example.com/auth?response_type=code&client_id=aaa&state="))
        assertTrue(url.contains(csrfState.secret()))
    }

    @Test
    fun testAuthorizeUrlPkce() {
        val client = newClient()
        val (url, _) = client
            .authorizeUrl { CsrfToken.new("csrf_token") }
            .setPkceChallenge(
                PkceCodeChallenge.fromCodeVerifierSha256(
                    PkceCodeVerifier.new("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
                )
            )
            .url()

        assertEquals(
            "https://example.com/auth?response_type=code&client_id=aaa&state=csrf_token&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256",
            url
        )
    }

    @Test
    fun testAuthorizeUrlImplicit() {
        val client = newClient()
        val (url, _) = client
            .authorizeUrl { CsrfToken.new("csrf_token") }
            .useImplicitFlow()
            .url()

        assertEquals(
            "https://example.com/auth?response_type=token&client_id=aaa&state=csrf_token",
            url
        )
    }

    @Test
    fun testAuthorizeUrlWithParam() {
        val client = BasicClientFactory.new(ClientId.new("aaa"))
            .setClientSecret(ClientSecret.new("bbb"))
            .setAuthUri(AuthUrl.new("https://example.com/auth?foo=bar"))
            .setTokenUri(TokenUrl.new("https://example.com/token"))

        val (url, _) = client
            .authorizeUrl { CsrfToken.new("csrf_token") }
            .url()

        assertEquals(
            "https://example.com/auth?foo=bar&response_type=code&client_id=aaa&state=csrf_token",
            url
        )
    }

    @Test
    fun testAuthorizeUrlWithScopes() {
        val scopes = listOf(
            Scope.new("read"),
            Scope.new("write"),
        )
        val (url, _) = newClient()
            .authorizeUrl { CsrfToken.new("csrf_token") }
            .addScopes(scopes)
            .url()

        assertEquals(
            "https://example.com/auth?response_type=code&client_id=aaa&state=csrf_token&scope=read+write",
            url
        )
    }

    @Test
    fun testAuthorizeUrlWithOneScope() {
        val (url, _) = newClient()
            .authorizeUrl { CsrfToken.new("csrf_token") }
            .addScope(Scope.new("read"))
            .url()

        assertEquals(
            "https://example.com/auth?response_type=code&client_id=aaa&state=csrf_token&scope=read",
            url
        )
    }

    @Test
    fun testAuthorizeUrlWithExtensionResponseType() {
        val client = newClient()
        val (url, _) = client
            .authorizeUrl { CsrfToken.new("csrf_token") }
            .setResponseType(ResponseType.new("code token"))
            .addExtraParam("foo", "bar")
            .url()

        assertEquals(
            "https://example.com/auth?response_type=code+token&client_id=aaa&state=csrf_token&foo=bar",
            url
        )
    }

    @Test
    fun testAuthorizeUrlWithRedirectUrl() {
        val client = newClient()
            .setRedirectUri(RedirectUrl.new("https://localhost/redirect"))

        val (url, _) = client
            .authorizeUrl { CsrfToken.new("csrf_token") }
            .url()

        assertEquals(
            "https://example.com/auth?response_type=code&client_id=aaa&state=csrf_token&redirect_uri=https%3A%2F%2Flocalhost%2Fredirect",
            url
        )
    }

    @Test
    fun testAuthorizeUrlWithRedirectUrlOverride() {
        val client = newClient()
            .setRedirectUri(RedirectUrl.new("https://localhost/redirect"))

        val (url, _) = client
            .authorizeUrl { CsrfToken.new("csrf_token") }
            .setRedirectUri(RedirectUrl.new("https://localhost/alternative"))
            .url()

        assertEquals(
            "https://example.com/auth?response_type=code&client_id=aaa&state=csrf_token&redirect_uri=https%3A%2F%2Flocalhost%2Falternative",
            url
        )
    }
}
