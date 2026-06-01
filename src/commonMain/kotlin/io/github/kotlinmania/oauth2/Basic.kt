// port-lint: source oauth2/src/basic.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlin.native.HiddenFromObjC

/** Basic OAuth2 authorization token types. */
public sealed class BasicTokenType {
    /** Bearer token. */
    public data object Bearer : BasicTokenType() {
        override val value: String = "bearer"
    }

    /** MAC token. */
    public data object Mac : BasicTokenType() {
        override val value: String = "mac"
    }

    /** An extension not defined by RFC 6749. */
    public data class Extension(
        public val rawValue: String,
    ) : BasicTokenType() {
        override val value: String = rawValue
    }

    public abstract val value: String

    override fun toString(): String = value

    public companion object {
        public fun fromString(value: String): BasicTokenType =
            when (value) {
                "bearer" -> Bearer
                "mac" -> Mac
                else -> Extension(value)
            }
    }
}

/** Basic access token error types. */
public sealed class BasicErrorResponseType : ErrorResponseType {
    /** Client authentication failed. */
    public data object InvalidClient : BasicErrorResponseType()

    /** The provided authorization grant or refresh token is invalid. */
    public data object InvalidGrant : BasicErrorResponseType()

    /** The request is missing a required parameter or is otherwise malformed. */
    public data object InvalidRequest : BasicErrorResponseType()

    /** The requested scope is invalid, unknown, malformed, or exceeds the granted scope. */
    public data object InvalidScope : BasicErrorResponseType()

    /** The authenticated client is not authorized to use this authorization grant type. */
    public data object UnauthorizedClient : BasicErrorResponseType()

    /** The authorization grant type is not supported by the authorization server. */
    public data object UnsupportedGrantType : BasicErrorResponseType()

    /** An extension not defined by RFC 6749. */
    public data class Extension(
        public val value: String,
    ) : BasicErrorResponseType()

    override val code: String
        get() =
            when (this) {
                InvalidClient -> "invalid_client"
                InvalidGrant -> "invalid_grant"
                InvalidRequest -> "invalid_request"
                InvalidScope -> "invalid_scope"
                UnauthorizedClient -> "unauthorized_client"
                UnsupportedGrantType -> "unsupported_grant_type"
                is Extension -> value
            }

    override fun toString(): String = code

    public companion object {
        public fun fromString(value: String): BasicErrorResponseType =
            when (value) {
                "invalid_client" -> InvalidClient
                "invalid_grant" -> InvalidGrant
                "invalid_request" -> InvalidRequest
                "invalid_scope" -> InvalidScope
                "unauthorized_client" -> UnauthorizedClient
                "unsupported_grant_type" -> UnsupportedGrantType
                else -> Extension(value)
            }
    }
}

/** Error response specialization for basic OAuth2 implementation. */
@HiddenFromObjC
public class BasicErrorResponse(
    error: BasicErrorResponseType,
    errorDescription: String? = null,
    errorUri: String? = null,
) : StandardErrorResponse<BasicErrorResponseType>(error, errorDescription, errorUri) {
    /** Encodes this error response using the RFC 6749 JSON field names. */
    public fun toJsonString(): String =
        buildString {
            append("{\"error\":")
            appendJsonString(error.code)
            if (errorDescription != null) {
                append(",\"error_description\":")
                appendJsonString(errorDescription)
            }
            if (errorUri != null) {
                append(",\"error_uri\":")
                appendJsonString(errorUri)
            }
            append("}")
        }

    public companion object {
        /** Decodes an RFC 6749 JSON error response object. */
        public fun fromJsonString(value: String): BasicErrorResponse {
            val fields = parseJsonStringObject(value)
            return BasicErrorResponse(
                BasicErrorResponseType.fromString(requireNotNull(fields["error"]) { "Missing required error field" }),
                fields["error_description"],
                fields["error_uri"],
            )
        }
    }
}

/** Token error specialization for basic OAuth2 implementation. */
public typealias BasicRequestTokenError<RequestError> = RequestTokenError<RequestError, BasicErrorResponse>

private fun StringBuilder.appendJsonString(value: String) {
    append('"')
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else ->
                if (character < ' ') {
                    append("\\u")
                    append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
        }
    }
    append('"')
}

private fun parseJsonStringObject(value: String): Map<String, String> {
    val parser = JsonStringObjectParser(value)
    return parser.parse()
}

private class JsonStringObjectParser(
    private val value: String,
) {
    private var index = 0

    fun parse(): Map<String, String> {
        val fields = mutableMapOf<String, String>()
        skipWhitespace()
        requireNext('{')
        skipWhitespace()
        if (consumeIf('}')) return fields
        while (true) {
            skipWhitespace()
            val name = parseString()
            skipWhitespace()
            requireNext(':')
            skipWhitespace()
            fields[name] = parseString()
            skipWhitespace()
            if (consumeIf('}')) return fields
            requireNext(',')
        }
    }

    private fun parseString(): String {
        requireNext('"')
        return buildString {
            while (index < value.length) {
                when (val character = value[index++]) {
                    '"' -> return@buildString
                    '\\' -> append(parseEscape())
                    else -> append(character)
                }
            }
            error("Unterminated JSON string")
        }
    }

    private fun parseEscape(): Char {
        require(index < value.length) { "Unterminated JSON escape" }
        return when (val character = value[index++]) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> parseUnicodeEscape()
            else -> error("Invalid JSON escape: $character")
        }
    }

    private fun parseUnicodeEscape(): Char {
        require(index + 4 <= value.length) { "Incomplete JSON unicode escape" }
        val code = value.substring(index, index + 4).toInt(16)
        index += 4
        return code.toChar()
    }

    private fun skipWhitespace() {
        while (index < value.length && value[index].isWhitespace()) {
            index += 1
        }
    }

    private fun consumeIf(expected: Char): Boolean {
        if (index < value.length && value[index] == expected) {
            index += 1
            skipWhitespace()
            require(index == value.length || expected != '}') { "Unexpected trailing JSON content" }
            return true
        }
        return false
    }

    private fun requireNext(expected: Char) {
        require(index < value.length && value[index] == expected) { "Expected '$expected' at JSON offset $index" }
        index += 1
    }
}
