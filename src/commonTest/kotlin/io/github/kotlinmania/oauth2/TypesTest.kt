// port-lint: tests oauth2/src/types.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals

class TypesTest {
    @Test
    fun string_wrappers_expose_values() {
        assertEquals("client", ClientId("client").toString())
        assertEquals("code", ResponseType("code").toString())
        assertEquals("user", ResourceOwnerUsername("user").toString())
        assertEquals("read", Scope("read").toString())
        assertEquals("S256", PkceCodeChallengeMethod("S256").toString())
    }

    @Test
    fun pkce_verifier_redacts_debug_text() {
        val verifier = PkceCodeVerifier("secret")

        assertEquals("secret", verifier.secret())
        assertEquals("secret", verifier.intoSecret())
        assertEquals("PkceCodeVerifier([redacted])", verifier.toString())
    }
}
