// port-lint: source oauth2/src/error.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlin.native.HiddenFromObjC
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        get() = value
    public val value: String
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

    public fun toJsonString(): String =
        buildString {
            append("{\"error\":\"")
            append(error.code)
            append('"')
            if (errorDescription != null) {
                append(",\"error_description\":\"")
                append(errorDescription)
                append('"')
            }
            if (errorUri != null) {
                append(",\"error_uri\":\"")
                append(errorUri)
                append('"')
            }
            append('}')
        }

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

        public fun fromJsonString(
            jsonString: String,
            errorTypeFactory: (String) -> BasicErrorResponseType = { BasicErrorResponseType.fromString(it) },
        ): StandardErrorResponse<BasicErrorResponseType> {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val errCode = json["error"]?.jsonPrimitive?.contentOrNull ?: ""
            val errDesc = json["error_description"]?.jsonPrimitive?.contentOrNull
            val errUri = json["error_uri"]?.jsonPrimitive?.contentOrNull
            return StandardErrorResponse(errorTypeFactory(errCode), errDesc, errUri)
        }
    }
}

/** Error encountered while requesting access token. */
@HiddenFromObjC
public sealed class RequestTokenError(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * Error response returned by authorization server. Contains the parsed `ErrorResponse`
     * returned by the server.
     */
    public data class ServerResponse(
        public val response: ErrorResponse,
    ) : RequestTokenError("Server returned error response: $response") {
        public fun response(): ErrorResponse = response
        public fun error(): ErrorResponse = response

        @Suppress("UNCHECKED_CAST")
        public fun <T : ErrorResponse> typedResponse(): T = response as T
    }

    /**
     * An error occurred while sending the request or receiving the response (e.g., network
     * connectivity failed).
     */
    public data class Request(
        public val error: Throwable,
    ) : RequestTokenError("Request error: ${error.message}", error)

    /**
     * Failed to parse server response. Parse errors may occur while parsing either successful
     * or error responses.
     */
    public data class Parse(
        public val error: String,
        public val responseBody: ByteArray,
    ) : RequestTokenError("Failed to parse response: $error") {
        override fun equals(other: Any?): Boolean =
            this === other ||
                other is Parse &&
                error == other.error &&
                responseBody.contentEquals(other.responseBody)

        override fun hashCode(): Int = 31 * error.hashCode() + responseBody.contentHashCode()
    }

    /** Some other type of error occurred (e.g., an unexpected server response). */
    public data class Other(
        public val error: String,
    ) : RequestTokenError(error)
}
