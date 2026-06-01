// port-lint: tests oauth2/src/error.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorTest {
    @Test
    fun test_error_response_serializer() {
        assertEquals(
            "{\"error\":\"unauthorized_client\"}",
            BasicErrorResponse(BasicErrorResponseType.UnauthorizedClient).toJsonString(),
        )

        assertEquals(
            "{\"error\":\"invalid_client\",\"error_description\":\"Invalid client_id\",\"error_uri\":\"https://example.com/errors/invalid_client\"}",
            BasicErrorResponse(
                BasicErrorResponseType.InvalidClient,
                "Invalid client_id",
                "https://example.com/errors/invalid_client",
            ).toJsonString(),
        )

        assertEquals(
            BasicErrorResponse(
                BasicErrorResponseType.InvalidClient,
                "Invalid client_id",
                "https://example.com/errors/invalid_client",
            ),
            BasicErrorResponse.fromJsonString(
                "{\"error\":\"invalid_client\",\"error_description\":\"Invalid client_id\",\"error_uri\":\"https://example.com/errors/invalid_client\"}",
            ),
        )
    }

    @Test
    fun error_response_decoder_ignores_extension_fields() {
        assertEquals(
            BasicErrorResponse(BasicErrorResponseType.InvalidGrant),
            BasicErrorResponse.fromJsonString(
                "{\"error\":\"invalid_grant\",\"error_codes\":[70000],\"status\":400,\"metadata\":{\"retry\":false}}",
            ),
        )
    }

    @Test
    fun error_response_equality_uses_values() {
        assertEquals(
            RequestTokenError.ServerResponse(BasicErrorResponse(BasicErrorResponseType.InvalidClient, "Invalid client")),
            RequestTokenError.ServerResponse(BasicErrorResponse(BasicErrorResponseType.InvalidClient, "Invalid client")),
        )
    }
}
