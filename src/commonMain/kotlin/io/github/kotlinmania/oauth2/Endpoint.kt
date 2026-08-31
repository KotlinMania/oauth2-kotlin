// port-lint: source oauth2/src/endpoint.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

/** An HTTP request. */
public data class HttpRequest(
    public val url: String,
    public val method: String = "POST",
    public val headers: Map<String, String> = emptyMap(),
    public val body: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpRequest) return false
        if (url != other.url) return false
        if (method != other.method) return false
        if (headers != other.headers) return false
        if (!body.contentEquals(other.body)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }

    override fun toString(): String =
        "HttpRequest(url=$url, method=$method, headers=$headers, bodyLength=${body.size})"
}

/** An HTTP response. */
public data class HttpResponse(
    public val status: Int,
    public val headers: Map<String, String> = emptyMap(),
    public val body: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpResponse) return false
        if (status != other.status) return false
        if (headers != other.headers) return false
        if (!body.contentEquals(other.body)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = status
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }

    override fun toString(): String =
        "HttpResponse(status=$status, headers=$headers, bodyLength=${body.size})"
}

/** An asynchronous HTTP client. */
public fun interface AsyncHttpClient {
    /** Perform a single HTTP request asynchronously. */
    public suspend fun call(request: HttpRequest): HttpResponse
}

/** A synchronous (blocking) HTTP client. */
public fun interface SyncHttpClient {
    /** Perform a single HTTP request synchronously. */
    public fun call(request: HttpRequest): HttpResponse
}

internal fun endpointRequest(
    authType: AuthType,
    clientId: ClientId,
    clientSecret: ClientSecret?,
    extraParams: List<Pair<String, String>>,
    redirectUrl: RedirectUrl?,
    scopes: List<Scope>?,
    url: String,
    params: List<Pair<String, String>>,
): HttpRequest {
    val headers =
        mutableMapOf(
            "accept" to CONTENT_TYPE_JSON,
            "content-type" to CONTENT_TYPE_FORMENCODED,
        )

    val paramList = mutableListOf<Pair<String, String>>()
    paramList.addAll(params)

    if (!scopes.isNullOrEmpty()) {
        paramList.add("scope" to scopes.joinToString(" ") { it.value })
    }

    when {
        authType == AuthType.BasicAuth && clientSecret != null -> {
            val encodedId = formUrlEncode(clientId.value)
            val encodedSecret = formUrlEncode(clientSecret.secret())
            val rawCredentials = "$encodedId:$encodedSecret".encodeToByteArray()
            val b64 = base64Encode(rawCredentials)
            headers["authorization"] = "Basic $b64"
        }
        else -> {
            paramList.add("client_id" to clientId.value)
            if (clientSecret != null) {
                paramList.add("client_secret" to clientSecret.secret())
            }
        }
    }

    if (redirectUrl != null) {
        paramList.add("redirect_uri" to redirectUrl.value)
    }

    paramList.addAll(extraParams)

    val bodyString =
        paramList.joinToString("&") { (key, value) ->
            "${formUrlEncode(key)}=${formUrlEncode(value)}"
        }
    val bodyBytes = bodyString.encodeToByteArray()

    return HttpRequest(
        url = url,
        method = "POST",
        headers = headers,
        body = bodyBytes,
    )
}

internal fun <DO> endpointResponse(
    httpResponse: HttpResponse,
    deserialize: (ByteArray) -> DO,
    errorDeserialize: (ByteArray) -> ErrorResponse = { bytes ->
        StandardErrorResponse.fromJsonString(bytes.decodeToString())
    },
): DO {
    checkResponseStatus(httpResponse, errorDeserialize)
    checkResponseBody(httpResponse)
    return deserialize(httpResponse.body)
}

internal fun endpointResponseStatusOnly(
    httpResponse: HttpResponse,
    errorDeserialize: (ByteArray) -> ErrorResponse = { bytes ->
        StandardErrorResponse.fromJsonString(bytes.decodeToString())
    },
) {
    checkResponseStatus(httpResponse, errorDeserialize)
}

internal fun checkResponseStatus(
    httpResponse: HttpResponse,
    errorDeserialize: (ByteArray) -> ErrorResponse = { bytes ->
        StandardErrorResponse.fromJsonString(bytes.decodeToString())
    },
) {
    if (httpResponse.status != 200) {
        val reason = httpResponse.body
        if (reason.isEmpty()) {
            throw RequestTokenError.Other("server returned empty error response")
        } else {
            try {
                val error = errorDeserialize(reason)
                throw RequestTokenError.ServerResponse(error)
            } catch (e: RequestTokenError) {
                throw e
            } catch (e: Throwable) {
                throw RequestTokenError.Parse(e.message ?: "Failed to parse error response", reason)
            }
        }
    }
}

internal fun checkResponseBody(httpResponse: HttpResponse) {
    val contentType =
        httpResponse.headers.entries
            .firstOrNull {
                it.key.equals("content-type", ignoreCase = true)
            }?.value

    if (contentType != null && !contentType.lowercase().startsWith(CONTENT_TYPE_JSON)) {
        throw RequestTokenError.Other(
            "unexpected response Content-Type: \"$contentType\", should be `$CONTENT_TYPE_JSON`",
        )
    }

    if (httpResponse.body.isEmpty()) {
        throw RequestTokenError.Other("server returned empty response body")
    }
}

internal fun formUrlEncode(value: String): String =
    buildString {
        val bytes = value.encodeToByteArray()
        for (b in bytes) {
            val byteVal = b.toInt() and 0xFF
            when (val ch = byteVal.toChar()) {
                in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.', '*' -> append(ch)
                ' ' -> append('+')
                else -> {
                    append('%')
                    val hex = byteVal.toString(16).uppercase()
                    if (hex.length < 2) append('0')
                    append(hex)
                }
            }
        }
    }

private fun base64Encode(bytes: ByteArray): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val sb = StringBuilder()
    var i = 0
    while (i < bytes.size) {
        val b0 = bytes[i++].toInt() and 0xFF
        val b1 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1
        val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1

        val out0 = b0 ushr 2
        val out1 = ((b0 and 0x03) shl 4) or if (b1 >= 0) (b1 ushr 4) else 0
        val out2 = if (b1 >= 0) (((b1 and 0x0F) shl 2) or if (b2 >= 0) (b2 ushr 6) else 0) else -1
        val out3 = if (b2 >= 0) (b2 and 0x3F) else -1

        sb.append(chars[out0])
        sb.append(chars[out1])
        sb.append(if (out2 >= 0) chars[out2] else '=')
        sb.append(if (out3 >= 0) chars[out3] else '=')
    }
    return sb.toString()
}
