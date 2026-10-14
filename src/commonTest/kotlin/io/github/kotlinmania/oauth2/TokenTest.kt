// port-lint: tests token/mod.rs
package io.github.kotlinmania.oauth2

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

internal data class CustomErrorResponse(
    val customError: String,
) : ErrorResponse {
    override fun toString(): String = "Custom Error from server"

    companion object {
        fun fromJsonString(jsonString: String): CustomErrorResponse {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val customError =
                requireNotNull(json["custom_error"]?.jsonPrimitive?.contentOrNull) { "Missing custom_error" }
            return CustomErrorResponse(customError)
        }
    }
}

internal typealias CustomErrorClient = Client<
    CustomErrorResponse,
    ColorfulTokenResponse,
    StandardTokenIntrospectionResponse<ColorfulFields, ColorfulTokenType>,
    ColorfulRevocableToken,
    CustomErrorResponse,
>

internal object CustomErrorClientFactory {
    fun new(clientId: ClientId): CustomErrorClient =
        Client(
            clientId = clientId,
            tokenResponseDeserializer = { bytes ->
                StandardTokenResponse.fromJsonString(
                    jsonString = bytes.decodeToString(),
                    tokenTypeFactory = { ColorfulTokenType.fromString(it) },
                    extraFieldsFactory = { ColorfulFields.fromJsonObject(it) },
                )
            },
            tokenErrorDeserializer = { bytes ->
                CustomErrorResponse.fromJsonString(bytes.decodeToString())
            },
            introspectionResponseDeserializer = { bytes ->
                val json = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
                val base =
                    StandardTokenIntrospectionResponse.fromJsonString(bytes.decodeToString()) {
                        ColorfulTokenType.fromString(it)
                    }
                StandardTokenIntrospectionResponse(
                    active = base.active(),
                    scopes = base.scopes(),
                    clientId = base.clientId(),
                    username = base.username(),
                    tokenType = base.tokenType(),
                    exp = base.exp(),
                    iat = base.iat(),
                    nbf = base.nbf(),
                    sub = base.sub(),
                    aud = base.aud(),
                    iss = base.iss(),
                    jti = base.jti(),
                    extraFields = ColorfulFields.fromJsonObject(json),
                )
            },
            revocationErrorDeserializer = { bytes ->
                CustomErrorResponse.fromJsonString(bytes.decodeToString())
            },
        )
}

class TokenTest {
    private fun newClient(): BasicClient =
        BasicClientFactory
            .new(ClientId.new("aaa"))
            .setAuthUri(AuthUrl.new("https://example.com/auth"))
            .setTokenUri(TokenUrl.new("https://example.com/token"))
            .setClientSecret(ClientSecret.new("bbb"))

    private fun <EF : ExtraTokenFields, TT : TokenType> assertTokenEq(
        a: StandardTokenResponse<EF, TT>,
        b: StandardTokenResponse<EF, TT>,
    ) {
        assertEquals(a.accessToken().secret(), b.accessToken().secret())
        assertEquals(a.tokenType(), b.tokenType())
        assertEquals(a.expiresIn(), b.expiresIn())
        assertEquals(a.refreshToken()?.secret(), b.refreshToken()?.secret())
        assertEquals(a.scopes(), b.scopes())
        assertEquals(a.extraFields(), b.extraFields())
    }

    private fun mockHttpClient(
        expectedUrl: String = "https://example.com/token",
        expectedHeaders: Map<String, String>? = null,
        expectedBody: String,
        responseStatus: Int,
        responseHeaders: Map<String, String> = mapOf("content-type" to "application/json"),
        responseBody: ByteArray,
    ): SyncHttpClient =
        SyncHttpClient { req ->
            assertEquals(expectedUrl, req.url)
            if (expectedHeaders != null) {
                assertEquals(expectedHeaders, req.headers)
            }
            assertEquals(expectedBody, req.body.decodeToString())
            HttpResponse(
                status = responseStatus,
                headers = responseHeaders,
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
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                                "authorization" to "Basic YWFhOmJiYg==",
                            ),
                        expectedBody = "grant_type=authorization_code&code=ccc",
                        responseStatus = 200,
                        responseHeaders = emptyMap(),
                        responseBody = "{\"access_token\": \"12/34\", \"token_type\": \"BEARER\"}".encodeToByteArray(),
                    ),
                )

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(BasicTokenType.Bearer, token.tokenType())
        assertNull(token.expiresIn())
        assertNull(token.refreshToken())

        val serializedJson = token.toJsonString()
        assertEquals("{\"access_token\":\"12/34\",\"token_type\":\"bearer\"}", serializedJson)

        val deserializedToken =
            StandardTokenResponse.fromJsonString(serializedJson) { BasicTokenType.fromString(it) }
        assertTokenEq(token, deserializedToken)
    }

    @Test
    fun testExchangeCodeSuccessfulWithCompleteJsonResponse() {
        val client = newClient().setAuthType(AuthType.RequestBody)
        val token =
            client
                .exchangeCode(AuthorizationCode.new("ccc"))
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                            ),
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

        val serializedJson = token.toJsonString()
        assertEquals(
            "{\"access_token\":\"12/34\",\"token_type\":\"bearer\",\"expires_in\":3600," +
                "\"refresh_token\":\"foobar\",\"scope\":\"read write\"}",
            serializedJson,
        )

        val deserializedToken =
            StandardTokenResponse.fromJsonString(serializedJson) { BasicTokenType.fromString(it) }
        assertTokenEq(token, deserializedToken)
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
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                                "authorization" to "Basic YWFhJTJGJTNCJTI2OmJiYiUyRiUzQiUyNg==",
                            ),
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
    fun testExchangeClientCredentialsWithBasicAuthButNoClientSecret() {
        val client =
            BasicClientFactory
                .new(ClientId.new("aaa/;&"))
                .setAuthUri(AuthUrl.new("https://example.com/auth"))
                .setTokenUri(TokenUrl.new("https://example.com/token"))
                .setAuthType(AuthType.BasicAuth)

        val token =
            client
                .exchangeClientCredentials()
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                            ),
                        expectedBody = "grant_type=client_credentials&client_id=aaa%2F%3B%26",
                        responseStatus = 200,
                        responseHeaders = emptyMap(),
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
    fun testExchangeClientCredentialsWithBodyAuthAndScope() {
        val client = newClient().setAuthType(AuthType.RequestBody)
        val token =
            client
                .exchangeClientCredentials()
                .addScope(Scope.new("read"))
                .addScope(Scope.new("write"))
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                            ),
                        expectedBody = "grant_type=client_credentials&scope=read+write&client_id=aaa&client_secret=bbb",
                        responseStatus = 200,
                        responseHeaders = mapOf("content-type" to "APPLICATION/jSoN"),
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
    fun testExchangeRefreshTokenWithJsonResponse() {
        val client = newClient()
        val token =
            client
                .exchangeRefreshToken(RefreshToken.new("ccc"))
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                                "authorization" to "Basic YWFhOmJiYg==",
                            ),
                        expectedBody = "grant_type=refresh_token&refresh_token=ccc",
                        responseStatus = 200,
                        responseHeaders = emptyMap(),
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
    fun testExchangeCodeSuccessfulWithRedirectUrlOverride() {
        val client =
            newClient()
                .setAuthType(AuthType.RequestBody)
                .setRedirectUri(RedirectUrl.new("https://redirect/here"))

        val token =
            client
                .exchangeCode(AuthorizationCode.new("ccc"))
                .setRedirectUri(RedirectUrl.new("https://redirect/alternative"))
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                            ),
                        expectedBody =
                            "grant_type=authorization_code&code=ccc&client_id=aaa&client_secret=bbb&" +
                                "redirect_uri=https%3A%2F%2Fredirect%2Falternative",
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
    fun testExchangeCodeSuccessfulWithBasicAuth() {
        val client =
            newClient()
                .setAuthType(AuthType.BasicAuth)
                .setRedirectUri(RedirectUrl.new("https://redirect/here"))

        val token =
            client
                .exchangeCode(AuthorizationCode.new("ccc"))
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                                "authorization" to "Basic YWFhOmJiYg==",
                            ),
                        expectedBody = "grant_type=authorization_code&code=ccc&redirect_uri=https%3A%2F%2Fredirect%2Fhere",
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
    fun testExchangeCodeSuccessfulWithPkceAndExtension() {
        val client =
            newClient()
                .setAuthType(AuthType.BasicAuth)
                .setRedirectUri(RedirectUrl.new("https://redirect/here"))

        val token =
            client
                .exchangeCode(AuthorizationCode.new("ccc"))
                .setPkceVerifier(
                    PkceCodeVerifier.new("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
                ).addExtraParam("foo", "bar")
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                                "authorization" to "Basic YWFhOmJiYg==",
                            ),
                        expectedBody =
                            "grant_type=authorization_code" +
                                "&code=ccc" +
                                "&code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk" +
                                "&redirect_uri=https%3A%2F%2Fredirect%2Fhere" +
                                "&foo=bar",
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
    fun testExchangeRefreshTokenSuccessfulWithExtension() {
        val client =
            newClient()
                .setAuthType(AuthType.BasicAuth)
                .setRedirectUri(RedirectUrl.new("https://redirect/here"))

        val token =
            client
                .exchangeRefreshToken(RefreshToken.new("ccc"))
                .addExtraParam("foo", "bar")
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                                "authorization" to "Basic YWFhOmJiYg==",
                            ),
                        expectedBody = "grant_type=refresh_token&refresh_token=ccc&foo=bar",
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
        assertEquals("invalid_request", response.error().toString())

        val serializedJson = response.toJsonString()
        assertEquals(
            "{\"error\":\"invalid_request\",\"error_description\":\"stuff happened\"}",
            serializedJson,
        )
        val deserializedError = StandardErrorResponse.fromJsonString(serializedJson)
        assertEquals(response, deserializedError)
        assertEquals("Server returned error response: invalid_request: stuff happened", err.message)
    }

    @Test
    fun testExchangeCodeWithJsonParseError() {
        val client = newClient()
        val httpClient =
            mockHttpClient(
                expectedHeaders =
                    mapOf(
                        "accept" to "application/json",
                        "content-type" to "application/x-www-form-urlencoded",
                        "authorization" to "Basic YWFhOmJiYg==",
                    ),
                expectedBody = "grant_type=authorization_code&code=ccc",
                responseStatus = 200,
                responseBody = "broken json".encodeToByteArray(),
            )

        val err =
            assertFailsWith<RequestTokenError.Parse> {
                client.exchangeCode(AuthorizationCode.new("ccc")).request(httpClient)
            }
        assertTrue(err.responseBody.contentEquals("broken json".encodeToByteArray()))
    }

    @Test
    fun testExchangeCodeWithUnexpectedContentType() {
        val client = newClient()
        val httpClient =
            mockHttpClient(
                expectedHeaders =
                    mapOf(
                        "accept" to "application/json",
                        "content-type" to "application/x-www-form-urlencoded",
                        "authorization" to "Basic YWFhOmJiYg==",
                    ),
                expectedBody = "grant_type=authorization_code&code=ccc",
                responseStatus = 200,
                responseHeaders = mapOf("content-type" to "text/plain"),
                responseBody = "broken json".encodeToByteArray(),
            )

        val err =
            assertFailsWith<RequestTokenError.Other> {
                client.exchangeCode(AuthorizationCode.new("ccc")).request(httpClient)
            }
        assertEquals(
            "unexpected response Content-Type: \"text/plain\", should be `application/json`",
            err.error,
        )
    }

    @Test
    fun testExchangeCodeWithInvalidTokenType() {
        val client =
            BasicClientFactory
                .new(ClientId.new("aaa"))
                .setAuthUri(AuthUrl.new("https://example.com/auth"))
                .setTokenUri(TokenUrl.new("https://example.com/token"))

        val httpClient =
            mockHttpClient(
                expectedHeaders =
                    mapOf(
                        "accept" to "application/json",
                        "content-type" to "application/x-www-form-urlencoded",
                    ),
                expectedBody = "grant_type=authorization_code&code=ccc&client_id=aaa",
                responseStatus = 200,
                responseBody = "{\"access_token\": \"12/34\", \"token_type\": 123}".encodeToByteArray(),
            )

        val err =
            assertFailsWith<RequestTokenError.Parse> {
                client.exchangeCode(AuthorizationCode.new("ccc")).request(httpClient)
            }
        assertTrue(err.error.contains("token_type"))
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
        assertEquals("Server returned error response: invalid_request: Expired code.", err.message)
    }

    @Test
    fun testExchangeCodeFailsGracefullyOnTransportError() {
        val client =
            BasicClientFactory
                .new(ClientId.new("aaa"))
                .setClientSecret(ClientSecret.new("bbb"))
                .setAuthUri(AuthUrl.new("https://auth"))
                .setTokenUri(TokenUrl.new("https://token"))

        val err =
            assertFailsWith<RequestTokenError.Request> {
                client
                    .exchangeCode(AuthorizationCode.new("ccc"))
                    .request { throw FakeError() }
            }
        assertIs<FakeError>(err.error)
    }

    @Test
    fun testExtensionSuccessfulWithMinimalJsonResponse() {
        val client =
            ColorfulClientFactory
                .new(ClientId.new("aaa"))
                .setClientSecret(ClientSecret.new("bbb"))
                .setAuthUri(AuthUrl.new("https://example.com/auth"))
                .setTokenUri(TokenUrl.new("https://example.com/token"))

        val token =
            client
                .exchangeCode(AuthorizationCode.new("ccc"))
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                                "authorization" to "Basic YWFhOmJiYg==",
                            ),
                        expectedBody = "grant_type=authorization_code&code=ccc",
                        responseStatus = 200,
                        responseBody =
                            "{\"access_token\": \"12/34\", \"token_type\": \"green\", \"height\": 10}"
                                .encodeToByteArray(),
                    ),
                )

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(ColorfulTokenType.Green, token.tokenType())
        assertNull(token.expiresIn())
        assertNull(token.refreshToken())
        assertNull(token.extraFields().shape())
        assertEquals(10, token.extraFields().height())

        val serializedJson = token.toJsonString()
        assertEquals(
            "{\"access_token\":\"12/34\",\"token_type\":\"green\",\"height\":10}",
            serializedJson,
        )

        val deserializedToken =
            StandardTokenResponse.fromJsonString(
                jsonString = serializedJson,
                tokenTypeFactory = { ColorfulTokenType.fromString(it) },
                extraFieldsFactory = { ColorfulFields.fromJsonObject(it) },
            )
        assertTokenEq(token, deserializedToken)
    }

    @Test
    fun testExtensionSuccessfulWithCompleteJsonResponse() {
        val client =
            ColorfulClientFactory
                .new(ClientId.new("aaa"))
                .setClientSecret(ClientSecret.new("bbb"))
                .setAuthUri(AuthUrl.new("https://example.com/auth"))
                .setTokenUri(TokenUrl.new("https://example.com/token"))
                .setAuthType(AuthType.RequestBody)

        val token =
            client
                .exchangeCode(AuthorizationCode.new("ccc"))
                .request(
                    mockHttpClient(
                        expectedHeaders =
                            mapOf(
                                "accept" to "application/json",
                                "content-type" to "application/x-www-form-urlencoded",
                            ),
                        expectedBody = "grant_type=authorization_code&code=ccc&client_id=aaa&client_secret=bbb",
                        responseStatus = 200,
                        responseBody =
                            (
                                "{\"access_token\": \"12/34\", " +
                                    "\"token_type\": \"red\", " +
                                    "\"scope\": \"read write\", " +
                                    "\"expires_in\": 3600, " +
                                    "\"refresh_token\": \"foobar\", " +
                                    "\"shape\": \"round\", " +
                                    "\"height\": 12}"
                            ).encodeToByteArray(),
                    ),
                )

        assertEquals("12/34", token.accessToken().secret())
        assertEquals(ColorfulTokenType.Red, token.tokenType())
        assertEquals(listOf(Scope.new("read"), Scope.new("write")), token.scopes())
        assertEquals(3600.seconds, token.expiresIn())
        assertEquals("foobar", token.refreshToken()?.secret())
        assertEquals("round", token.extraFields().shape())
        assertEquals(12, token.extraFields().height())

        val serializedJson = token.toJsonString()
        assertEquals(
            "{\"access_token\":\"12/34\",\"token_type\":\"red\",\"expires_in\":3600," +
                "\"refresh_token\":\"foobar\",\"scope\":\"read write\",\"shape\":\"round\",\"height\":12}",
            serializedJson,
        )

        val deserializedToken =
            StandardTokenResponse.fromJsonString(
                jsonString = serializedJson,
                tokenTypeFactory = { ColorfulTokenType.fromString(it) },
                extraFieldsFactory = { ColorfulFields.fromJsonObject(it) },
            )
        assertTokenEq(token, deserializedToken)
    }

    @Test
    fun testExtensionWithSimpleJsonError() {
        val client =
            ColorfulClientFactory
                .new(ClientId.new("aaa"))
                .setClientSecret(ClientSecret.new("bbb"))
                .setAuthUri(AuthUrl.new("https://example.com/auth"))
                .setTokenUri(TokenUrl.new("https://example.com/token"))

        val httpClient =
            mockHttpClient(
                expectedHeaders =
                    mapOf(
                        "accept" to "application/json",
                        "content-type" to "application/x-www-form-urlencoded",
                        "authorization" to "Basic YWFhOmJiYg==",
                    ),
                expectedBody = "grant_type=authorization_code&code=ccc",
                responseStatus = 400,
                responseBody =
                    (
                        "{\"error\": \"too_light\", \"error_description\": \"stuff happened\", " +
                            "\"error_uri\": \"https://errors\"}"
                    ).encodeToByteArray(),
            )

        val err =
            assertFailsWith<RequestTokenError.ServerResponse> {
                client.exchangeCode(AuthorizationCode.new("ccc")).request(httpClient)
            }

        val errorResponse = err.typedResponse<StandardErrorResponse<ColorfulErrorResponseType>>()
        assertEquals(ColorfulErrorResponseType.TooLight, errorResponse.error())
        assertEquals("stuff happened", errorResponse.errorDescription())
        assertEquals("https://errors", errorResponse.errorUri())

        val serializedJson = errorResponse.toJsonString()
        assertEquals(
            "{\"error\":\"too_light\",\"error_description\":\"stuff happened\",\"error_uri\":\"https://errors\"}",
            serializedJson,
        )

        val json = Json.parseToJsonElement(serializedJson).jsonObject
        val deserializedError =
            StandardErrorResponse.new(
                ColorfulErrorResponseType.fromString(json["error"]?.jsonPrimitive?.contentOrNull.orEmpty()),
                json["error_description"]?.jsonPrimitive?.contentOrNull,
                json["error_uri"]?.jsonPrimitive?.contentOrNull,
            )
        assertEquals(errorResponse, deserializedError)
        assertEquals(
            "Server returned error response: too_light: stuff happened (see https://errors)",
            err.message,
        )
    }

    @Test
    fun testExtensionWithCustomJsonError() {
        val client =
            CustomErrorClientFactory
                .new(ClientId.new("aaa"))
                .setClientSecret(ClientSecret.new("bbb"))
                .setAuthUri(AuthUrl.new("https://example.com/auth"))
                .setTokenUri(TokenUrl.new("https://example.com/token"))

        val httpClient =
            mockHttpClient(
                expectedHeaders =
                    mapOf(
                        "accept" to "application/json",
                        "content-type" to "application/x-www-form-urlencoded",
                        "authorization" to "Basic YWFhOmJiYg==",
                    ),
                expectedBody = "grant_type=authorization_code&code=ccc",
                responseStatus = 400,
                responseBody = "{\"custom_error\": \"non-compliant oauth implementation ;-)\"}".encodeToByteArray(),
            )

        val err =
            assertFailsWith<RequestTokenError.ServerResponse> {
                client.exchangeCode(AuthorizationCode.new("ccc")).request(httpClient)
            }

        val customError = err.typedResponse<CustomErrorResponse>()
        assertEquals("non-compliant oauth implementation ;-)", customError.customError)
    }

    @Test
    fun testExtensionSerializer() {
        val tokenResponse =
            ColorfulTokenResponse.new(
                AccessToken.new("mysecret"),
                ColorfulTokenType.Red,
                ColorfulFields(
                    shape = "circle",
                    height = 10,
                ),
            )
        tokenResponse.setExpiresIn(3600.seconds)
        tokenResponse.setRefreshToken(RefreshToken.new("myothersecret"))
        val serialized = tokenResponse.toJsonString()
        assertEquals(
            "{" +
                "\"access_token\":\"mysecret\"," +
                "\"token_type\":\"red\"," +
                "\"expires_in\":3600," +
                "\"refresh_token\":\"myothersecret\"," +
                "\"shape\":\"circle\"," +
                "\"height\":10" +
                "}",
            serialized,
        )
    }
}
