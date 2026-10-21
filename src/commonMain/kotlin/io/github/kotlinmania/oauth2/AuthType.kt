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
