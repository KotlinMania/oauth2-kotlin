// port-lint: tests oauth2/src/token/mod.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class TokenTest {
    private fun newClient(): BasicClient =
        BasicClientFactory
            .new(ClientId.new("aaa"))
            .setAuthUri(AuthUrl.new("https://example.com/auth"))
            .setTokenUri(TokenUrl.new("https://example.com/token"))
            .setClientSecret(ClientSecret.new("bbb"))

    private fun mockHttpClient(
        expectedUrl: String = "https://example.com/token",
        expectedBody: String,
        responseStatus: Int,
        responseBody: ByteArray,
    ): SyncHttpClient =
        SyncHttpClient { req ->
            assertEquals(expectedUrl, req.url)
            assertEquals(expectedBody, req.body.decodeToString())
            HttpResponse(
                status = responseStatus,
                headers = mapOf("content-type" to "application/json"),
                body = responseBody,
            )
        }

    @Test
    fun testExchangeCodeSuccessfulWithMinimalJsonResponse() {
        val client =
            BasicClientFactory
                .new(ClientId.new("aaa"))
                .setClientSecret(ClientSecret.new("bbb"))
                .setAuthUri(AuthUrl.new("https://example.com/auth"))
                .setTokenUri(TokenUrl.new("https://example.com/token"))

        val token =
            client
                .exchangeCode(AuthorizationCode.new("ccc"))
                .request(
                    mockHttpClient(
                        expectedBody = "grant_type=authorization_code&code=ccc",
                        responseStatus = 200,
                        responseBody = "{\"access_token\": \"12/34\", \"token_type\": \"BEARER\"}".encodeToByteArray(),
                    ),
                )

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
        assertNull(token.expiresIn())
        assertNull(token.refreshToken())
    }

    @Test
    fun testExchangeCodeSuccessfulWithCompleteJsonResponse() {
        val client = newClient().setAuthType(AuthType.RequestBody)
        val token =
            client
                .exchangeCode(AuthorizationCode.new("ccc"))
                .request(
                    mockHttpClient(
                        expectedBody = "grant_type=authorization_code&code=ccc&client_id=aaa&client_secret=bbb",
                        responseStatus = 200,
                        responseBody =
                            """
                            {
                                "access_token": "12/34",
                                "token_type": "bearer",
                                "scope": "read write",
                                "expires_in": 3600,
                                "refresh_token": "foobar"
                            }
                            """.trimIndent().encodeToByteArray(),
                    ),
                )

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
        assertEquals(listOf(Scope.new("read"), Scope.new("write")), token.scopes())
        assertEquals(3600.seconds, token.expiresIn())
        assertEquals("foobar", token.refreshToken()?.secret())
    }

    @Test
    fun testExchangeClientCredentialsWithBasicAuth() {
        val client =
            BasicClientFactory
                .new(ClientId.new("aaa/;&"))
                .setClientSecret(ClientSecret.new("bbb/;&"))
                .setAuthUri(AuthUrl.new("https://example.com/auth"))
                .setTokenUri(TokenUrl.new("https://example.com/token"))
                .setAuthType(AuthType.BasicAuth)

        val token =
            client
                .exchangeClientCredentials()
                .request(
                    mockHttpClient(
                        expectedBody = "grant_type=client_credentials",
                        responseStatus = 200,
                        responseBody = "{\"access_token\": \"12/34\", \"token_type\": \"bearer\", \"scope\": \"read write\"}".encodeToByteArray(),
                    ),
                )

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
        assertEquals(listOf(Scope.new("read"), Scope.new("write")), token.scopes())
        assertNull(token.expiresIn())
        assertNull(token.refreshToken())
    }

    @Test
    fun testExchangeRefreshTokenWithBasicAuth() {
        val client = newClient().setAuthType(AuthType.BasicAuth)
        val token =
            client
                .exchangeRefreshToken(RefreshToken.new("ccc"))
                .request(
                    mockHttpClient(
                        expectedBody = "grant_type=refresh_token&refresh_token=ccc",
                        responseStatus = 200,
                        responseBody = "{\"access_token\": \"12/34\", \"token_type\": \"bearer\", \"scope\": \"read write\"}".encodeToByteArray(),
                    ),
                )

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
        assertEquals(listOf(Scope.new("read"), Scope.new("write")), token.scopes())
        assertNull(token.expiresIn())
        assertNull(token.refreshToken())
    }

    @Test
    fun testExchangePasswordWithJsonResponse() {
        val client = newClient()
        val token =
            client
                .exchangePassword(
                    ResourceOwnerUsername.new("user"),
                    ResourceOwnerPassword.new("pass"),
                ).addScope(Scope.new("read"))
                .addScope(Scope.new("write"))
                .request(
                    mockHttpClient(
                        expectedBody = "grant_type=password&username=user&password=pass&scope=read+write",
                        responseStatus = 200,
                        responseBody = "{\"access_token\": \"12/34\", \"token_type\": \"bearer\", \"scope\": \"read write\"}".encodeToByteArray(),
                    ),
                )

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
        assertEquals(listOf(Scope.new("read"), Scope.new("write")), token.scopes())
        assertNull(token.expiresIn())
        assertNull(token.refreshToken())
    }

    @Test
    fun testExchangeCodeSuccessfulWithRedirectUrl() {
        val client =
            newClient()
                .setAuthType(AuthType.RequestBody)
                .setRedirectUri(RedirectUrl.new("https://redirect/here"))

        val token =
            client
                .exchangeCode(AuthorizationCode.new("ccc"))
                .request(
                    mockHttpClient(
                        expectedBody = "grant_type=authorization_code&code=ccc&client_id=aaa&client_secret=bbb&redirect_uri=https%3A%2F%2Fredirect%2Fhere",
                        responseStatus = 200,
                        responseBody = "{\"access_token\": \"12/34\", \"token_type\": \"bearer\", \"scope\": \"read write\"}".encodeToByteArray(),
                    ),
                )

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
    }

    @Test
    fun testExchangeCodeWithSimpleJsonError() {
        val client = newClient()
        val httpClient =
            mockHttpClient(
                expectedBody = "grant_type=authorization_code&code=ccc",
                responseStatus = 400,
                responseBody = "{\"error\": \"invalid_request\", \"error_description\": \"stuff happened\"}".encodeToByteArray(),
            )

        val err =
            assertFailsWith<RequestTokenError.ServerResponse> {
                client.exchangeCode(AuthorizationCode.new("ccc")).request(httpClient)
            }

        val response = err.typedResponse<BasicErrorResponse>()
        assertEquals(BasicErrorResponseType.InvalidRequest, response.error())
        assertEquals("stuff happened", response.errorDescription())
        assertNull(response.errorUri())
        assertEquals("invalid_request: stuff happened", response.toString())
    }

    @Test
    fun testExchangeCodeWith400StatusCode() {
        val body = """{"error":"invalid_request","error_description":"Expired code."}"""
        val client = newClient()
        val httpClient =
            mockHttpClient(
                expectedBody = "grant_type=authorization_code&code=ccc",
                responseStatus = 400,
                responseBody = body.encodeToByteArray(),
            )

        val err =
            assertFailsWith<RequestTokenError.ServerResponse> {
                client.exchangeCode(AuthorizationCode.new("ccc")).request(httpClient)
            }

        val response = err.typedResponse<BasicErrorResponse>()
        assertEquals(BasicErrorResponseType.InvalidRequest, response.error())
        assertEquals("Expired code.", response.errorDescription())
        assertNull(response.errorUri())
    }
}
