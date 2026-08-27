// port-lint: tests introspection.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntrospectionTest {
    private fun newClient(): BasicClient =
        BasicClientFactory
            .new(ClientId.new("aaa"))
            .setAuthUri(AuthUrl.new("https://example.com/auth"))
            .setTokenUri(TokenUrl.new("https://example.com/token"))
            .setClientSecret(ClientSecret.new("bbb"))

    private fun mockHttpClient(
        expectedUrl: String,
        expectedBody: String,
        responseStatus: Int,
        responseBody: ByteArray,
    ): SyncHttpClient =
        SyncHttpClient { req ->
            assertEquals(expectedUrl, req.url)
            assertEquals(expectedBody, req.body.decodeToString())
            HttpResponse(status = responseStatus, headers = emptyMap(), body = responseBody)
        }

    @Test
    fun testTokenIntrospectionSuccessfulWithBasicAuthMinimalResponse() {
        val client =
            newClient()
                .setAuthType(AuthType.BasicAuth)
                .setRedirectUri(RedirectUrl.new("https://redirect/here"))
                .setIntrospectionUrl(IntrospectionUrl.new("https://introspection/url"))

        val introspectionResponse =
            client
                .introspect(AccessToken.new("access_token_123"))
                .request(
                    mockHttpClient(
                        expectedUrl = "https://introspection/url",
                        expectedBody = "token=access_token_123",
                        responseStatus = 200,
                        responseBody = "{\"active\": true}".encodeToByteArray(),
                    ),
                )

        assertTrue(introspectionResponse.active())
        assertNull(introspectionResponse.scopes())
        assertNull(introspectionResponse.clientId())
        assertNull(introspectionResponse.username())
        assertNull(introspectionResponse.tokenType())
        assertNull(introspectionResponse.exp())
        assertNull(introspectionResponse.iat())
        assertNull(introspectionResponse.nbf())
        assertNull(introspectionResponse.sub())
        assertNull(introspectionResponse.aud())
        assertNull(introspectionResponse.iss())
        assertNull(introspectionResponse.jti())
    }

    @Test
    fun testTokenIntrospectionSuccessfulWithBasicAuthFullResponse() {
        val client =
            newClient()
                .setAuthType(AuthType.BasicAuth)
                .setRedirectUri(RedirectUrl.new("https://redirect/here"))
                .setIntrospectionUrl(IntrospectionUrl.new("https://introspection/url"))

        val json =
            """
            {
                "active": true,
                "scope": "email profile",
                "client_id": "aaa",
                "username": "demo",
                "token_type": "bearer",
                "exp": 1604073517,
                "iat": 1604073217,
                "nbf": 1604073317,
                "sub": "demo",
                "aud": "demo",
                "iss": "http://127.0.0.1:8080/auth/realms/test-realm",
                "jti": "be1b7da2-fc18-47b3-bdf1-7a4f50bcf53f"
            }
            """.trimIndent()

        val introspectionResponse =
            client
                .introspect(AccessToken.new("access_token_123"))
                .setTokenTypeHint("access_token")
                .request(
                    mockHttpClient(
                        expectedUrl = "https://introspection/url",
                        expectedBody = "token=access_token_123&token_type_hint=access_token",
                        responseStatus = 200,
                        responseBody = json.encodeToByteArray(),
                    ),
                )

        assertTrue(introspectionResponse.active())
        assertEquals(listOf(Scope.new("email"), Scope.new("profile")), introspectionResponse.scopes())
        assertEquals(ClientId.new("aaa"), introspectionResponse.clientId())
        assertEquals("demo", introspectionResponse.username())
        assertEquals(BasicTokenType.Bearer, introspectionResponse.tokenType())
        assertEquals(1604073517L, introspectionResponse.exp())
        assertEquals(1604073217L, introspectionResponse.iat())
        assertEquals(1604073317L, introspectionResponse.nbf())
        assertEquals("demo", introspectionResponse.sub())
        assertEquals(listOf("demo"), introspectionResponse.aud())
        assertEquals("http://127.0.0.1:8080/auth/realms/test-realm", introspectionResponse.iss())
        assertEquals("be1b7da2-fc18-47b3-bdf1-7a4f50bcf53f", introspectionResponse.jti())
    }
}
