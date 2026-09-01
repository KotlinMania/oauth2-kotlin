// port-lint: tests devicecode.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class DevicecodeTest {
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

    private fun newDeviceAuthDetails(expiresIn: Long): StandardDeviceAuthorizationResponse {
        val body =
            """
            {
                "device_code": "12345",
                "verification_uri": "https://verify/here",
                "user_code": "abcde",
                "verification_uri_complete": "https://verify/here?abcde",
                "expires_in": $expiresIn,
                "interval": 1
            }
            """.trimIndent()

        val deviceAuthUrl = DeviceAuthorizationUrl.new("https://deviceauth/here")
        val client = newClient().setDeviceAuthorizationUrl(deviceAuthUrl)

        val httpClient =
            mockHttpClient(
                expectedUrl = "https://deviceauth/here",
                expectedBody = "scope=openid&foo=bar",
                responseStatus = 200,
                responseBody = body.encodeToByteArray(),
            )

        return client
            .exchangeDeviceCode()
            .addExtraParam("foo", "bar")
            .addScope(Scope.new("openid"))
            .request(httpClient) { bytes ->
                DeviceAuthorizationResponse.fromJsonString(bytes.decodeToString())
            }
    }

    @Test
    fun testExchangeDeviceCodeAndToken() {
        val details = newDeviceAuthDetails(3600)
        assertEquals("12345", details.deviceCode().secret())
        assertEquals("https://verify/here", details.verificationUri().url())
        assertEquals("abcde", details.userCode().secret())
        assertEquals("https://verify/here?abcde", details.verificationUriComplete()?.secret())
        assertEquals(3600.seconds, details.expiresIn())
        assertEquals(1.seconds, details.interval())

        val httpClient =
            mockHttpClient(
                expectedUrl = "https://example.com/token",
                expectedBody = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code&device_code=12345",
                responseStatus = 200,
                responseBody = "{\"access_token\": \"12/34\", \"token_type\": \"bearer\", \"scope\": \"openid\"}".encodeToByteArray(),
            )

        val token =
            newClient()
                .exchangeDeviceAccessToken(details)
                .request(httpClient, sleepFn = {})

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
        assertEquals(listOf(Scope.new("openid")), token.scopes())
        assertNull(token.expiresIn())
        assertNull(token.refreshToken())
    }

    @Test
    fun testDeviceTokenPendingThenSuccess() {
        val details = newDeviceAuthDetails(20)
        assertEquals("12345", details.deviceCode().secret())
        assertEquals("https://verify/here", details.verificationUri().url())
        assertEquals("abcde", details.userCode().secret())
        assertEquals(20.seconds, details.expiresIn())
        assertEquals(1.seconds, details.interval())

        var callCount = 0
        val httpClient =
            SyncHttpClient { req ->
                assertEquals("https://example.com/token", req.url)
                assertEquals("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Adevice_code&device_code=12345", req.body.decodeToString())
                callCount++
                if (callCount <= 3) {
                    HttpResponse(
                        status = 400,
                        headers = emptyMap(),
                        body = "{\"error\": \"authorization_pending\", \"error_description\": \"Still waiting for user\"}".encodeToByteArray(),
                    )
                } else {
                    HttpResponse(
                        status = 200,
                        headers = emptyMap(),
                        body = "{\"access_token\": \"12/34\", \"token_type\": \"bearer\", \"scope\": \"openid\"}".encodeToByteArray(),
                    )
                }
            }

        val token =
            newClient()
                .exchangeDeviceAccessToken(details)
                .request(httpClient, sleepFn = {})

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
        assertEquals(listOf(Scope.new("openid")), token.scopes())
        assertNull(token.expiresIn())
        assertNull(token.refreshToken())
    }

    @Test
    fun testDeviceTokenSlowdownThenSuccess() {
        val details = newDeviceAuthDetails(3600)
        var callCount = 0
        val httpClient =
            SyncHttpClient { req ->
                assertEquals("https://example.com/token", req.url)
                callCount++
                if (callCount <= 3) {
                    HttpResponse(
                        status = 400,
                        headers = emptyMap(),
                        body = "{\"error\": \"slow_down\", \"error_description\": \"Slow down\"}".encodeToByteArray(),
                    )
                } else {
                    HttpResponse(
                        status = 200,
                        headers = emptyMap(),
                        body = "{\"access_token\": \"12/34\", \"token_type\": \"bearer\", \"scope\": \"openid\"}".encodeToByteArray(),
                    )
                }
            }

        val token =
            newClient()
                .exchangeDeviceAccessToken(details)
                .request(httpClient, sleepFn = {})

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
    }

    @Test
    fun testDeviceTokenAccessDenied() {
        val details = newDeviceAuthDetails(2)
        val httpClient =
            SyncHttpClient {
                HttpResponse(
                    status = 400,
                    headers = emptyMap(),
                    body = "{\"error\": \"access_denied\", \"error_description\": \"Access Denied\"}".encodeToByteArray(),
                )
            }

        val err =
            assertFailsWith<RequestTokenError.ServerResponse> {
                newClient()
                    .exchangeDeviceAccessToken(details)
                    .request(httpClient, sleepFn = {})
            }
        val response = err.typedResponse<DeviceCodeErrorResponse>()
        assertEquals(DeviceCodeErrorResponseType.AccessDenied, response.error())
    }

    @Test
    fun testDeviceTokenExpired() {
        val details = newDeviceAuthDetails(2)
        val httpClient =
            SyncHttpClient {
                HttpResponse(
                    status = 400,
                    headers = emptyMap(),
                    body = "{\"error\": \"expired_token\", \"error_description\": \"Token has expired\"}".encodeToByteArray(),
                )
            }

        val err =
            assertFailsWith<RequestTokenError.ServerResponse> {
                newClient()
                    .exchangeDeviceAccessToken(details)
                    .request(httpClient, sleepFn = {})
            }
        val response = err.typedResponse<DeviceCodeErrorResponse>()
        assertEquals(DeviceCodeErrorResponseType.ExpiredToken, response.error())
    }

    @Test
    fun testDeviceAuthResponseDefaultInterval() {
        val response =
            DeviceAuthorizationResponse.fromJsonString(
                """
                {
                    "device_code": "12345",
                    "verification_uri": "https://verify/here",
                    "user_code": "abcde",
                    "expires_in": 300
                }
                """.trimIndent(),
            )
        assertEquals(5.seconds, response.interval())
    }

    @Test
    fun testDeviceAuthResponseNonDefaultInterval() {
        val response =
            DeviceAuthorizationResponse.fromJsonString(
                """
                {
                    "device_code": "12345",
                    "verification_uri": "https://verify/here",
                    "user_code": "abcde",
                    "expires_in": 300,
                    "interval": 10
                }
                """.trimIndent(),
            )
        assertEquals(10.seconds, response.interval())
    }
}
