// port-lint: tests oauth2/src/tests.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class Tests {
    @Test
    fun testNewClient() {
        val client = BasicClientFactory.new(ClientId.new("aaa"))
            .setAuthUri(AuthUrl.new("https://example.com/auth"))
            .setTokenUri(TokenUrl.new("https://example.com/token"))
            .setClientSecret(ClientSecret.new("bbb"))

        assertEquals(ClientId.new("aaa"), client.clientId())
        assertEquals(AuthUrl.new("https://example.com/auth"), client.authUrl())
        assertEquals(TokenUrl.new("https://example.com/token"), client.tokenUrl())
        assertEquals(ClientSecret.new("bbb"), client.clientSecret())
    }

    @Test
    fun testAllTypesInstantiable() {
        assertNotNull(AccessToken.new("token"))
        assertNotNull(AuthUrl.new("https://example.com/auth"))
        assertNotNull(AuthorizationCode.new("code"))
        assertNotNull(ClientId.new("client_id"))
        assertNotNull(ClientSecret.new("secret"))
        assertNotNull(CsrfToken.new("csrf"))
        assertNotNull(EmptyExtraTokenFields())
        assertNotNull(HttpRequest(url = "https://example.com", body = ByteArray(0)))
        assertNotNull(HttpResponse(status = 200, headers = emptyMap(), body = ByteArray(0)))
        assertNotNull(PkceCodeChallenge.new("chal"))
        assertNotNull(PkceCodeChallengeMethod.S256)
        assertNotNull(PkceCodeVerifier.new("verif"))
        assertNotNull(RedirectUrl.new("https://example.com/redirect"))
        assertNotNull(RefreshToken.new("refresh"))
        assertNotNull(ResourceOwnerPassword.new("pwd"))
        assertNotNull(ResourceOwnerUsername.new("user"))
        assertNotNull(ResponseType.new("code"))
        assertNotNull(Scope.new("scope"))
        assertNotNull(TokenUrl.new("https://example.com/token"))
        assertNotNull(DeviceCode.new("code"))
        assertNotNull(EndUserVerificationUrl.new("https://example.com/verify"))
        assertNotNull(UserCode.new("user_code"))
        assertNotNull(DeviceAuthorizationUrl.new("https://example.com/device"))
        assertNotNull(EmptyExtraDeviceAuthorizationFields())
    }
}
