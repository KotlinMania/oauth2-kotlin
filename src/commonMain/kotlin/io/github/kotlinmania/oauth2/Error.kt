// port-lint: source oauth2/src/error.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlin.native.HiddenFromObjC

/** Server error response. */
public interface ErrorResponse

/** Error types enum. */
public interface ErrorResponseType {
    public val code: String
}

/**
 * Error response returned by server after requesting an access token.
 *
 * The fields in this structure are defined in Section 5.2 of RFC 6749. This
 * class is parameterized by an `ErrorResponseType` to support error types
 * specific to future OAuth2 authentication schemes and extensions.
 */
@HiddenFromObjC
public open class StandardErrorResponse<T : ErrorResponseType>(
    /** REQUIRED. A single ASCII error code. */
    public val error: T,
    /** OPTIONAL. Human-readable ASCII text providing additional information. */
    public val errorDescription: String? = null,
    /** OPTIONAL. URI identifying a human-readable web page with information about the error. */
    public val errorUri: String? = null,
) : ErrorResponse {
    override fun toString(): String =
        buildString {
            append(error.code)
            if (errorDescription != null) {
                append(": ")
                append(errorDescription)
            }
            if (errorUri != null) {
                append(" (see ")
                append(errorUri)
                append(')')
            }
        }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is StandardErrorResponse<*> &&
            error == other.error &&
            errorDescription == other.errorDescription &&
            errorUri == other.errorUri

    override fun hashCode(): Int {
        var result = error.hashCode()
        result = 31 * result + (errorDescription?.hashCode() ?: 0)
        result = 31 * result + (errorUri?.hashCode() ?: 0)
        return result
    }
}

/** Error encountered while requesting access token. */
@HiddenFromObjC
public sealed class RequestTokenError<out RequestError, out TokenError : ErrorResponse> {
    /** Error response returned by authorization server. */
    public data class ServerResponse<out TokenError : ErrorResponse>(
        public val error: TokenError,
    ) : RequestTokenError<Nothing, TokenError>()

    /** An error occurred while sending the request or receiving the response. */
    public data class Request<out RequestError>(
        public val error: RequestError,
    ) : RequestTokenError<RequestError, Nothing>()

    /** Failed to parse server response. */
    public data class Parse(
        public val error: String,
        public val responseBody: ByteArray,
    ) : RequestTokenError<Nothing, Nothing>() {
        override fun equals(other: Any?): Boolean =
            this === other ||
                other is Parse &&
                error == other.error &&
                responseBody.contentEquals(other.responseBody)

        override fun hashCode(): Int = 31 * error.hashCode() + responseBody.contentHashCode()
    }

    /** Some other type of error occurred, such as an unexpected server response. */
    public data class Other(
        public val message: String,
    ) : RequestTokenError<Nothing, Nothing>()
}
