// port-lint: source error.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlin.native.HiddenFromObjC

/**
 * Server Error Response
 *
 * See [Section 5.2](https://datatracker.ietf.org/doc/html/rfc6749#section-5.2) of RFC 6749.
 * This interface exists separately from the `StandardErrorResponse` class
 * to support customization by clients, such as supporting interoperability with
 * non-standards-compliant OAuth2 providers.
 *
 * The `toString` implementation for types implementing [ErrorResponse] should be a
 * human-readable string suitable for printing (e.g., within a [RequestTokenError]).
 */
public interface ErrorResponse

/**
 * Error types interface.
 *
 * NOTE: The serialization must return the snake case representation of
 * this error type. This value must match the error type from the relevant OAuth 2.0 standards
 * (RFC 6749 or an extension).
 */
public interface ErrorResponseType {
    public val code: String
}

/**
 * Error response returned by server after requesting an access token.
 *
 * The fields in this structure are defined in
 * [Section 5.2 of RFC 6749](https://tools.ietf.org/html/rfc6749#section-5.2). This
 * class is parameterized by an [ErrorResponseType] to support error types specific to future OAuth2
 * authentication schemes and extensions.
 */
@HiddenFromObjC
public open class StandardErrorResponse<T : ErrorResponseType>(
    /** REQUIRED. A single ASCII error code deserialized to the generic parameter [ErrorResponseType]. */
    public val error: T,
    /**
     * OPTIONAL. Human-readable ASCII text providing additional
     * information, used to assist the client developer in understanding the error that
     * occurred.
     */
    public val errorDescription: String? = null,
    /**
     * OPTIONAL. A URI identifying a human-readable web page with information
     * about the error used to provide the client developer with additional information about
     * the error.
     */
    public val errorUri: String? = null,
) : ErrorResponse {
    /**
     * REQUIRED. A single ASCII error code deserialized to the generic parameter
     * `ErrorResponseType`.
     */
    public fun error(): T = error

    /**
     * OPTIONAL. Human-readable ASCII text providing additional information, used to assist
     * the client developer in understanding the error that occurred.
     */
    public fun errorDescription(): String? = errorDescription

    /**
     * OPTIONAL. URI identifying a human-readable web page with information about the error,
     * used to provide the client developer with additional information about the error.
     */
    public fun errorUri(): String? = errorUri

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

    public companion object {
        /**
         * Instantiate a new `StandardErrorResponse`.
         *
         * @param error REQUIRED. A single ASCII error code deserialized to the generic parameter [ErrorResponseType].
         * @param errorDescription OPTIONAL. Human-readable ASCII text providing additional information.
         * @param errorUri OPTIONAL. A URI identifying a human-readable web page with information about the error.
         */
        public fun <T : ErrorResponseType> new(
            error: T,
            errorDescription: String? = null,
            errorUri: String? = null,
        ): StandardErrorResponse<T> = StandardErrorResponse(error, errorDescription, errorUri)
    }
}

/** Error encountered while requesting access token. */
@HiddenFromObjC
public sealed class RequestTokenError<out RequestError, out TokenError : ErrorResponse> {
    /**
     * Error response returned by authorization server. Contains the parsed `ErrorResponse`
     * returned by the server.
     */
    public data class ServerResponse<out TokenError : ErrorResponse>(
        public val error: TokenError,
    ) : RequestTokenError<Nothing, TokenError>()

    /**
     * An error occurred while sending the request or receiving the response (e.g., network
     * connectivity failed).
     */
    public data class Request<out RequestError>(
        public val error: RequestError,
    ) : RequestTokenError<RequestError, Nothing>()

    /**
     * Failed to parse server response. Parse errors may occur while parsing either successful
     * or error responses.
     */
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

    /** Some other type of error occurred (e.g., an unexpected server response). */
    public data class Other(
        public val message: String,
    ) : RequestTokenError<Nothing, Nothing>()
}
