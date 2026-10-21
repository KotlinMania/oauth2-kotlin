@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import io.github.kotlinmania.thiserror.StdError
import kotlin.native.HiddenFromObjC

/** Error configuring a client. */
@HiddenFromObjC
public sealed class ConfigurationError(
    message: String,
) : Exception(message),
    StdError {
    /** Endpoint URL not specified. */
    public class MissingUrl(
        public val urlType: String,
    ) : ConfigurationError("No $urlType endpoint URL specified")

    /** Insecure (non-HTTPS) URL provided. */
    public class InsecureUrl(
        public val urlType: String,
    ) : ConfigurationError("Scheme for $urlType endpoint URL must be HTTPS")
}
