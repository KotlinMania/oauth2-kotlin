// port-lint: source types.rs
package io.github.kotlinmania.oauth2

import io.github.kotlinmania.base64.alphabet.Alphabet
import io.github.kotlinmania.base64.encodedLen
import io.github.kotlinmania.getrandom.getrandom
import io.github.kotlinmania.sha2.Sha256
import kotlin.jvm.JvmInline

/** Client identifier issued to the client during registration. */
@JvmInline
public value class ClientId(
    public val value: String,
) {
    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(value: String): ClientId = ClientId(value)
    }
}

/** URL of the authorization server's authorization endpoint. */
@JvmInline
public value class AuthUrl(
    public val value: String,
) {
    public fun url(): String = value

    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(url: String): AuthUrl = AuthUrl(url)

        public fun fromUrl(url: String): AuthUrl = AuthUrl(url)
    }
}

/** URL of the authorization server's token endpoint. */
@JvmInline
public value class TokenUrl(
    public val value: String,
) {
    public fun url(): String = value

    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(url: String): TokenUrl = TokenUrl(url)

        public fun fromUrl(url: String): TokenUrl = TokenUrl(url)
    }
}

/** URL of the client's redirection endpoint. */
@JvmInline
public value class RedirectUrl(
    public val value: String,
) {
    public fun url(): String = value

    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(url: String): RedirectUrl = RedirectUrl(url)

        public fun fromUrl(url: String): RedirectUrl = RedirectUrl(url)
    }
}

/** URL of the client's token introspection endpoint. */
@JvmInline
public value class IntrospectionUrl(
    public val value: String,
) {
    public fun url(): String = value

    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(url: String): IntrospectionUrl = IntrospectionUrl(url)

        public fun fromUrl(url: String): IntrospectionUrl = IntrospectionUrl(url)
    }
}

/** URL of the authorization server's RFC 7009 token revocation endpoint. */
@JvmInline
public value class RevocationUrl(
    public val value: String,
) {
    public fun url(): String = value

    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(url: String): RevocationUrl = RevocationUrl(url)

        public fun fromUrl(url: String): RevocationUrl = RevocationUrl(url)
    }
}

/** URL of the client's device authorization endpoint. */
@JvmInline
public value class DeviceAuthorizationUrl(
    public val value: String,
) {
    public fun url(): String = value

    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(url: String): DeviceAuthorizationUrl = DeviceAuthorizationUrl(url)

        public fun fromUrl(url: String): DeviceAuthorizationUrl = DeviceAuthorizationUrl(url)
    }
}

/** URL of the end-user verification URI on the authorization server. */
@JvmInline
public value class EndUserVerificationUrl(
    public val value: String,
) {
    public fun url(): String = value

    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(url: String): EndUserVerificationUrl = EndUserVerificationUrl(url)

        public fun fromUrl(url: String): EndUserVerificationUrl = EndUserVerificationUrl(url)
    }
}

/** Authorization endpoint response grant type. */
@JvmInline
public value class ResponseType(
    public val value: String,
) {
    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(value: String): ResponseType = ResponseType(value)
    }
}

/** Resource owner's username used directly as an authorization grant. */
@JvmInline
public value class ResourceOwnerUsername(
    public val value: String,
) {
    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(value: String): ResourceOwnerUsername = ResourceOwnerUsername(value)
    }
}

/** Access token scope, as defined by the authorization server. */
@JvmInline
public value class Scope(
    public val value: String,
) {
    public fun asRef(): String = value

    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public fun new(value: String): Scope = Scope(value)
    }
}

/** Code challenge method used for PKCE protection. */
@JvmInline
public value class PkceCodeChallengeMethod(
    public val value: String,
) {
    public fun asStr(): String = value

    override fun toString(): String = value

    public companion object {
        public val S256: PkceCodeChallengeMethod = PkceCodeChallengeMethod("S256")
        public val Plain: PkceCodeChallengeMethod = PkceCodeChallengeMethod("plain")
    }
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

    override fun equals(other: Any?): Boolean =
        this === other || (other is PkceCodeVerifier && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): PkceCodeVerifier = PkceCodeVerifier(secret)
    }
}

/**
 * Code Challenge used for PKCE protection via the code challenge parameter.
 */
public data class PkceCodeChallenge(
    private val codeChallenge: String,
    private val codeChallengeMethod: PkceCodeChallengeMethod,
) {
    /** Returns the PKCE code challenge as a string. */
    public fun asStr(): String = codeChallenge

    /** Returns the PKCE code challenge method. */
    public fun method(): PkceCodeChallengeMethod = codeChallengeMethod

    public companion object {
        public fun new(challenge: String): PkceCodeChallenge =
            PkceCodeChallenge(challenge, PkceCodeChallengeMethod.Plain)

        /** Generate a new random, base64-encoded SHA-256 PKCE code. */
        public fun newRandomSha256(): Pair<PkceCodeChallenge, PkceCodeVerifier> =
            newRandomSha256Len(32)

        /** Generate a new random, base64-encoded SHA-256 PKCE challenge code and verifier. */
        public fun newRandomSha256Len(numBytes: Int): Pair<PkceCodeChallenge, PkceCodeVerifier> {
            val codeVerifier = newRandomLen(numBytes)
            return Pair(fromCodeVerifierSha256(codeVerifier), codeVerifier)
        }

        /** Generate a new random, base64-encoded PKCE code verifier. */
        public fun newRandomLen(numBytes: Int): PkceCodeVerifier {
            require(numBytes in 32..96) { "numBytes must be between 32 and 96 inclusive" }
            val randomBytes = ByteArray(numBytes)
            getrandom(randomBytes)
            return PkceCodeVerifier.new(base64UrlSafeNoPad(randomBytes))
        }

        /** Generate a SHA-256 PKCE code challenge from the supplied PKCE code verifier. */
        public fun fromCodeVerifierSha256(codeVerifier: PkceCodeVerifier): PkceCodeChallenge {
            val secret = codeVerifier.secret()
            require(secret.length in 43..128) { "codeVerifier length must be between 43 and 128" }
            val digest = Sha256.digest(secret.encodeToByteArray())
            val codeChallenge = base64UrlSafeNoPad(digest)
            return PkceCodeChallenge(codeChallenge, PkceCodeChallengeMethod.S256)
        }

        /** Generate a new random, base64-encoded PKCE code (plain). */
        public fun newRandomPlain(): Pair<PkceCodeChallenge, PkceCodeVerifier> {
            val codeVerifier = newRandomLen(32)
            return Pair(fromCodeVerifierPlain(codeVerifier), codeVerifier)
        }

        /** Generate a plain PKCE code challenge from the supplied PKCE code verifier. */
        public fun fromCodeVerifierPlain(codeVerifier: PkceCodeVerifier): PkceCodeChallenge {
            val secret = codeVerifier.secret()
            require(secret.length in 43..128) { "codeVerifier length must be between 43 and 128" }
            return PkceCodeChallenge(secret, PkceCodeChallengeMethod.Plain)
        }
    }
}

/** Client secret issued to the client during registration. */
public class ClientSecret public constructor(
    private val value: String,
) {
    public fun secret(): String = value

    public fun intoSecret(): String = value

    override fun toString(): String = "ClientSecret([redacted])"

    override fun equals(other: Any?): Boolean =
        this === other || (other is ClientSecret && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): ClientSecret = ClientSecret(secret)
    }
}

/** Value used for CSRF protection via the `state` parameter. */
public class CsrfToken public constructor(
    private val value: String,
) {
    public fun secret(): String = value

    public fun intoSecret(): String = value

    override fun toString(): String = "CsrfToken([redacted])"

    override fun equals(other: Any?): Boolean =
        this === other || (other is CsrfToken && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): CsrfToken = CsrfToken(secret)

        public fun newRandom(): CsrfToken = newRandomLen(16)

        public fun newRandomLen(numBytes: Int): CsrfToken {
            val randomBytes = ByteArray(numBytes)
            getrandom(randomBytes)
            return CsrfToken.new(base64UrlSafeNoPad(randomBytes))
        }
    }
}

/** Authorization code returned from the authorization endpoint. */
public class AuthorizationCode public constructor(
    private val value: String,
) {
    public fun secret(): String = value

    public fun intoSecret(): String = value

    override fun toString(): String = "AuthorizationCode([redacted])"

    override fun equals(other: Any?): Boolean =
        this === other || (other is AuthorizationCode && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): AuthorizationCode = AuthorizationCode(secret)
    }
}

/** Refresh token used to obtain a new access token. */
public class RefreshToken public constructor(
    private val value: String,
) {
    public fun secret(): String = value

    public fun intoSecret(): String = value

    override fun toString(): String = "RefreshToken([redacted])"

    override fun equals(other: Any?): Boolean =
        this === other || (other is RefreshToken && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): RefreshToken = RefreshToken(secret)
    }
}

/** Access token returned by the token endpoint. */
public class AccessToken public constructor(
    private val value: String,
) {
    public fun secret(): String = value

    public fun intoSecret(): String = value

    override fun toString(): String = "AccessToken([redacted])"

    override fun equals(other: Any?): Boolean =
        this === other || (other is AccessToken && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): AccessToken = AccessToken(secret)
    }
}

/** Resource owner's password used directly as an authorization grant. */
public class ResourceOwnerPassword public constructor(
    private val value: String,
) {
    public fun secret(): String = value

    public fun intoSecret(): String = value

    override fun toString(): String = "ResourceOwnerPassword([redacted])"

    override fun equals(other: Any?): Boolean =
        this === other || (other is ResourceOwnerPassword && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): ResourceOwnerPassword = ResourceOwnerPassword(secret)
    }
}

/** Device code returned by the device authorization endpoint. */
public class DeviceCode public constructor(
    private val value: String,
) {
    public fun secret(): String = value

    public fun intoSecret(): String = value

    override fun toString(): String = "DeviceCode([redacted])"

    override fun equals(other: Any?): Boolean =
        this === other || (other is DeviceCode && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): DeviceCode = DeviceCode(secret)
    }
}

/** Verification URI returned by the device authorization endpoint. */
public class VerificationUriComplete public constructor(
    private val value: String,
) {
    public fun secret(): String = value

    public fun intoSecret(): String = value

    override fun toString(): String = "VerificationUriComplete([redacted])"

    override fun equals(other: Any?): Boolean =
        this === other || (other is VerificationUriComplete && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): VerificationUriComplete = VerificationUriComplete(secret)
    }
}

/** User code returned by the device authorization endpoint. */
public class UserCode public constructor(
    private val value: String,
) {
    public fun secret(): String = value

    public fun intoSecret(): String = value

    override fun toString(): String = "UserCode([redacted])"

    override fun equals(other: Any?): Boolean =
        this === other || (other is UserCode && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    public companion object {
        public fun new(secret: String): UserCode = UserCode(secret)
    }
}

private fun base64UrlSafeNoPad(data: ByteArray): String {
    val chars = Alphabet.URL_SAFE.asString()
    val sb = StringBuilder(encodedLen(data.size, false) ?: ((data.size * 4 + 2) / 3))
    var i = 0
    while (i < data.size) {
        val b0 = data[i++].toInt() and 0xFF
        val b1 = if (i < data.size) data[i++].toInt() and 0xFF else -1
        val b2 = if (i < data.size) data[i++].toInt() and 0xFF else -1

        sb.append(chars[b0 ushr 2])
        if (b1 != -1) {
            sb.append(chars[((b0 and 0x03) shl 4) or (b1 ushr 4)])
            if (b2 != -1) {
                sb.append(chars[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
                sb.append(chars[b2 and 0x3F])
            } else {
                sb.append(chars[(b1 and 0x0F) shl 2])
            }
        } else {
            sb.append(chars[(b0 and 0x03) shl 4])
        }
    }
    return sb.toString()
}
