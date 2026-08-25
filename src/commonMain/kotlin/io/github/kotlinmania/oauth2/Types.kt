// port-lint: source types.rs
package io.github.kotlinmania.oauth2

import kotlin.jvm.JvmInline
import kotlin.random.Random

/** Client identifier issued to the client during registration. */
@JvmInline
public value class ClientId(
    public val value: String,
) {
    public fun asStr(): String = value

    override fun toString(): String = value
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
}

/** Resource owner's username used directly as an authorization grant. */
@JvmInline
public value class ResourceOwnerUsername(
    public val value: String,
) {
    public fun asStr(): String = value

    override fun toString(): String = value
}

/** Access token scope, as defined by the authorization server. */
@JvmInline
public value class Scope(
    public val value: String,
) {
    public fun asRef(): String = value

    public fun asStr(): String = value

    override fun toString(): String = value
}

/** Code challenge method used for PKCE protection. */
@JvmInline
public value class PkceCodeChallengeMethod(
    public val value: String,
) {
    public fun asStr(): String = value

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
            val randomBytes = Random.Default.nextBytes(numBytes)
            return PkceCodeVerifier.new(base64UrlSafeNoPad(randomBytes))
        }

        /** Generate a SHA-256 PKCE code challenge from the supplied PKCE code verifier. */
        public fun fromCodeVerifierSha256(codeVerifier: PkceCodeVerifier): PkceCodeChallenge {
            val secret = codeVerifier.secret()
            require(secret.length in 43..128) { "codeVerifier length must be between 43 and 128" }
            val digest = sha256(secret.encodeToByteArray())
            val codeChallenge = base64UrlSafeNoPad(digest)
            return PkceCodeChallenge(codeChallenge, PkceCodeChallengeMethod("S256"))
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
            return PkceCodeChallenge(secret, PkceCodeChallengeMethod("plain"))
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
            val randomBytes = Random.Default.nextBytes(numBytes)
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

private const val BASE64_URL_SAFE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

private fun base64UrlSafeNoPad(data: ByteArray): String {
    val sb = StringBuilder((data.size * 4 + 2) / 3)
    var i = 0
    while (i < data.size) {
        val b0 = data[i++].toInt() and 0xFF
        val b1 = if (i < data.size) data[i++].toInt() and 0xFF else -1
        val b2 = if (i < data.size) data[i++].toInt() and 0xFF else -1

        sb.append(BASE64_URL_SAFE_CHARS[b0 ushr 2])
        if (b1 != -1) {
            sb.append(BASE64_URL_SAFE_CHARS[((b0 and 0x03) shl 4) or (b1 ushr 4)])
            if (b2 != -1) {
                sb.append(BASE64_URL_SAFE_CHARS[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
                sb.append(BASE64_URL_SAFE_CHARS[b2 and 0x3F])
            } else {
                sb.append(BASE64_URL_SAFE_CHARS[(b1 and 0x0F) shl 2])
            }
        } else {
            sb.append(BASE64_URL_SAFE_CHARS[(b0 and 0x03) shl 4])
        }
    }
    return sb.toString()
}

private fun sha256(data: ByteArray): ByteArray {
    val h = intArrayOf(
        0x6a09e667.toInt(),
        0xbb67ae85.toInt(),
        0x3c6ef372.toInt(),
        0xa54ff53a.toInt(),
        0x510e527f.toInt(),
        0x9b05688c.toInt(),
        0x1f83d9ab.toInt(),
        0x5be0cd19.toInt(),
    )
    val k = intArrayOf(
        0x428a2f98.toInt(), 0x71374491.toInt(), 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
        0x3956c25b.toInt(), 0x59f111f1.toInt(), 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
        0xd807aa98.toInt(), 0x12835b01.toInt(), 0x243185be.toInt(), 0x550c7dc3.toInt(),
        0x72be5d74.toInt(), 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
        0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6.toInt(), 0x240ca1cc.toInt(),
        0x2de92c6f.toInt(), 0x4a7484aa.toInt(), 0x5cb0a9dc.toInt(), 0x76f988da.toInt(),
        0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
        0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351.toInt(), 0x14292967.toInt(),
        0x27b70a85.toInt(), 0x2e1b2138.toInt(), 0x4d2c6dfc.toInt(), 0x53380d13.toInt(),
        0x650a7354.toInt(), 0x766a0abb.toInt(), 0x81c2c92e.toInt(), 0x92722c85.toInt(),
        0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
        0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070.toInt(),
        0x19a4c116.toInt(), 0x1e376c08.toInt(), 0x2748774c.toInt(), 0x34b0bcb5.toInt(),
        0x391c0cb3.toInt(), 0x4ed8aa4a.toInt(), 0x5b9cca4f.toInt(), 0x682e6ff3.toInt(),
        0x748f82ee.toInt(), 0x78a5636f.toInt(), 0x84c87814.toInt(), 0x8cc70208.toInt(),
        0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt(),
    )

    val msgLen = data.size
    val bitLen = msgLen.toLong() * 8L
    val padLen = (if (msgLen % 64 < 56) 56 - (msgLen % 64) else 120 - (msgLen % 64))
    val padded = ByteArray(msgLen + padLen + 8)
    data.copyInto(padded)
    padded[msgLen] = 0x80.toByte()
    for (i in 0 until 8) {
        padded[padded.size - 1 - i] = ((bitLen ushr (i * 8)) and 0xFF).toByte()
    }

    val w = IntArray(64)
    for (chunk in 0 until padded.size step 64) {
        for (i in 0 until 16) {
            val j = chunk + i * 4
            w[i] = ((padded[j].toInt() and 0xFF) shl 24) or
                ((padded[j + 1].toInt() and 0xFF) shl 16) or
                ((padded[j + 2].toInt() and 0xFF) shl 8) or
                (padded[j + 3].toInt() and 0xFF)
        }
        for (i in 16 until 64) {
            val s0 = (w[i - 15] ushr 7 or (w[i - 15] shl 25)) xor
                (w[i - 15] ushr 18 or (w[i - 15] shl 14)) xor
                (w[i - 15] ushr 3)
            val s1 = (w[i - 2] ushr 17 or (w[i - 2] shl 15)) xor
                (w[i - 2] ushr 19 or (w[i - 2] shl 13)) xor
                (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }

        var a = h[0]
        var b = h[1]
        var c = h[2]
        var d = h[3]
        var e = h[4]
        var f = h[5]
        var g = h[6]
        var hVar = h[7]

        for (i in 0 until 64) {
            val s1 = (e ushr 6 or (e shl 26)) xor (e ushr 11 or (e shl 21)) xor (e ushr 25 or (e shl 7))
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = hVar + s1 + ch + k[i] + w[i]
            val s0 = (a ushr 2 or (a shl 30)) xor (a ushr 13 or (a shl 19)) xor (a ushr 22 or (a shl 10))
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = s0 + maj

            hVar = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        h[0] += a
        h[1] += b
        h[2] += c
        h[3] += d
        h[4] += e
        h[5] += f
        h[6] += g
        h[7] += hVar
    }

    val result = ByteArray(32)
    for (i in 0 until 8) {
        result[i * 4] = (h[i] ushr 24).toByte()
        result[i * 4 + 1] = (h[i] ushr 16).toByte()
        result[i * 4 + 2] = (h[i] ushr 8).toByte()
        result[i * 4 + 3] = h[i].toByte()
    }
    return result
}
