// port-lint: tests types.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TypesTest {
    @Test
    fun stringWrappersExposeValues() {
        assertEquals("client", ClientId("client").toString())
        assertEquals("code", ResponseType("code").toString())
        assertEquals("user", ResourceOwnerUsername("user").toString())
        assertEquals("read", Scope("read").toString())
        assertEquals("S256", PkceCodeChallengeMethod("S256").toString())
    }

    @Test
    fun testSecretConversion() {
        val secret = CsrfToken.new("top_secret")
        assertEquals("top_secret", secret.intoSecret())
    }

    @Test
    fun testSecretRedaction() {
        val secret = ClientSecret.new("top_secret")
        assertEquals("ClientSecret([redacted])", secret.toString())
    }

    @Test
    fun testCodeVerifierTooShort() {
        assertFailsWith<IllegalArgumentException> {
            PkceCodeChallenge.newRandomSha256Len(31)
        }
    }

    @Test
    fun testCodeVerifierTooLong() {
        assertFailsWith<IllegalArgumentException> {
            PkceCodeChallenge.newRandomSha256Len(97)
        }
    }

    @Test
    fun testCodeVerifierMin() {
        val code = PkceCodeChallenge.newRandomSha256Len(32)
        assertEquals(43, code.second.secret().length)
    }

    @Test
    fun testCodeVerifierMax() {
        val code = PkceCodeChallenge.newRandomSha256Len(96)
        assertEquals(128, code.second.secret().length)
    }

    @Test
    fun testCodeVerifierChallenge() {
        // Example from https://tools.ietf.org/html/rfc7636#appendix-B
        val codeVerifier = PkceCodeVerifier.new("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            PkceCodeChallenge.fromCodeVerifierSha256(codeVerifier).asStr(),
        )
    }
}
