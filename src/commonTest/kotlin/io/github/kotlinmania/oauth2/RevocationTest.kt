// port-lint: tests revocation.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RevocationTest {
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
        responseBody: ByteArray = ByteArray(0),
    ): SyncHttpClient =
        SyncHttpClient { req ->
            assertEquals(expectedUrl, req.url)
            assertEquals(expectedBody, req.body.decodeToString())
            HttpResponse(status = responseStatus, headers = emptyMap(), body = responseBody)
        }

    @Test
    fun testTokenRevocationWithMissingUrl() {
        val client = newClient().setRevocationUrlOption(null)
        val err =
            assertFailsWith<IllegalArgumentException> {
                client.revokeToken(StandardRevocableToken.from(AccessToken.new("access_token_123")))
            }
        assertEquals("No revocation endpoint URL specified", err.message)
    }

    @Test
    fun testTokenRevocationWithNonHttpsUrl() {
        val client = newClient()
        val err =
            assertFailsWith<ConfigurationError.InsecureUrl> {
                client
                    .setRevocationUrl(RevocationUrl.new("http://revocation/url"))
                    .revokeToken(StandardRevocableToken.from(AccessToken.new("access_token_123")))
            }
        assertTrue(err.message?.contains("HTTPS") == true || err.urlType == "revocation")
    }

    @Test
    fun testTokenRevocationWithUnsupportedTokenType() {
        val client =
            newClient()
                .setRevocationUrl(RevocationUrl.new("https://revocation/url"))

        val httpClient =
            mockHttpClient(
                expectedUrl = "https://revocation/url",
                expectedBody = "token=access_token_123&token_type_hint=access_token",
                responseStatus = 400,
                responseBody = "{\"error\": \"unsupported_token_type\", \"error_description\": \"stuff happened\", \"error_uri\": \"https://errors\"}".encodeToByteArray(),
            )

        val err =
            assertFailsWith<RequestTokenError.ServerResponse> {
                client
                    .revokeToken(StandardRevocableToken.from(AccessToken.new("access_token_123")))
                    .request(httpClient)
            }
        val response = err.typedResponse<BasicRevocationErrorResponse>()
        assertEquals(RevocationErrorResponseType.UnsupportedTokenType, response.error())
    }

    @Test
    fun testTokenRevocationWithAccessTokenAndEmptyJsonResponse() {
        val client =
            newClient()
                .setRevocationUrl(RevocationUrl.new("https://revocation/url"))

        val httpClient =
            mockHttpClient(
                expectedUrl = "https://revocation/url",
                expectedBody = "token=access_token_123&token_type_hint=access_token",
                responseStatus = 200,
                responseBody = "{}".encodeToByteArray(),
            )

        client
            .revokeToken(StandardRevocableToken.from(AccessToken.new("access_token_123")))
            .request(httpClient)
    }

    @Test
    fun testTokenRevocationWithAccessTokenAndEmptyResponse() {
        val client =
            newClient()
                .setRevocationUrl(RevocationUrl.new("https://revocation/url"))

        val httpClient =
            mockHttpClient(
                expectedUrl = "https://revocation/url",
                expectedBody = "token=access_token_123&token_type_hint=access_token",
                responseStatus = 200,
                responseBody = ByteArray(0),
            )

        client
            .revokeToken(StandardRevocableToken.from(AccessToken.new("access_token_123")))
            .request(httpClient)
    }

    @Test
    fun testTokenRevocationWithAccessTokenAndNonJsonResponse() {
        val client =
            newClient()
                .setRevocationUrl(RevocationUrl.new("https://revocation/url"))

        val httpClient =
            mockHttpClient(
                expectedUrl = "https://revocation/url",
                expectedBody = "token=access_token_123&token_type_hint=access_token",
                responseStatus = 200,
                responseBody = byteArrayOf(1, 2, 3),
            )

        client
            .revokeToken(StandardRevocableToken.from(AccessToken.new("access_token_123")))
            .request(httpClient)
    }

    @Test
    fun testTokenRevocationWithRefreshToken() {
        val client =
            newClient()
                .setRevocationUrl(RevocationUrl.new("https://revocation/url"))

        val httpClient =
            mockHttpClient(
                expectedUrl = "https://revocation/url",
                expectedBody = "token=refresh_token_123&token_type_hint=refresh_token",
                responseStatus = 200,
                responseBody = "{}".encodeToByteArray(),
            )

        client
            .revokeToken(StandardRevocableToken.from(RefreshToken.new("refresh_token_123")))
            .request(httpClient)
    }
}
