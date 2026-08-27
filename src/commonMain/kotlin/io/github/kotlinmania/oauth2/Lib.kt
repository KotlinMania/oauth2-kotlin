// port-lint: source oauth2/src/lib.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlin.native.HiddenFromObjC

public const val CONTENT_TYPE_JSON: String = "application/json"
public const val CONTENT_TYPE_FORMENCODED: String = "application/x-www-form-urlencoded"

/**
 * Method of sending the client ID and client secret to the authorization server.
 *
 * Defaults to `AuthType.BasicAuth`.
 */
@HiddenFromObjC
public enum class AuthType {
    /**
     * Client ID and client secret are sent in the `Authorization` header via HTTP Basic Auth.
     *
     * This is the default and recommended authentication mechanism.
     */
    BasicAuth,

    /** Client ID and client secret are sent in the request body. */
    RequestBody,
}

/** Error configuring a client. */
@HiddenFromObjC
public sealed class ConfigurationError(message: String) : Exception(message) {
    /** Insecure (non-HTTPS) URL provided. */
    public class InsecureUrl(
        public val urlType: String,
    ) : ConfigurationError("Scheme for $urlType endpoint URL must be HTTPS")
}

/** Error returned by an HTTP client. */
@HiddenFromObjC
public sealed class HttpClientError(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** HTTP client error. */
    public class Client(
        public val clientCause: Throwable,
    ) : HttpClientError("HTTP client error", clientCause)

    /** HTTP protocol error. */
    public class Http(
        public val httpCause: Throwable,
    ) : HttpClientError("HTTP error", httpCause)

    /** Other error. */
    public class Other(
        public val otherMessage: String,
    ) : HttpClientError("Other error: $otherMessage")
}
