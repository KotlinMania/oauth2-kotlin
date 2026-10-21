@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import io.github.kotlinmania.thiserror.StdError
import kotlin.native.HiddenFromObjC

/** Error returned by an HTTP client. */
@HiddenFromObjC
public sealed class HttpClientError(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause),
    StdError {
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
