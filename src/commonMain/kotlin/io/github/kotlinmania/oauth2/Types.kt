// port-lint: source oauth2/src/types.rs
package io.github.kotlinmania.oauth2

import kotlin.jvm.JvmInline

/** Client identifier issued to the client during registration. */
@JvmInline
public value class ClientId(
    public val value: String,
) {
    override fun toString(): String = value
}

/** Authorization endpoint response grant type. */
@JvmInline
public value class ResponseType(
    public val value: String,
) {
    override fun toString(): String = value
}

/** Resource owner's username used directly as an authorization grant. */
@JvmInline
public value class ResourceOwnerUsername(
    public val value: String,
) {
    override fun toString(): String = value
}

/** Access token scope, as defined by the authorization server. */
@JvmInline
public value class Scope(
    public val value: String,
) {
    override fun toString(): String = value
}

/** Code challenge method used for PKCE protection. */
@JvmInline
public value class PkceCodeChallengeMethod(
    public val value: String,
) {
    override fun toString(): String = value
}

/**
 * Code verifier used for PKCE protection.
 *
 * Leaking this value may compromise the security of the OAuth2 flow.
 */
public class PkceCodeVerifier public constructor(
    private val value: String,
) {
    /** Gets the secret contained within this verifier. */
    public fun secret(): String = value

    /** Returns the secret contained within this verifier. */
    public fun intoSecret(): String = value

    override fun toString(): String = "PkceCodeVerifier([redacted])"
}
