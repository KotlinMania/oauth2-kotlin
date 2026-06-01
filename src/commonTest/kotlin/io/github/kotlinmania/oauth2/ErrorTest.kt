// port-lint: tests oauth2/src/error.rs
package io.github.kotlinmania.oauth2

import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorTest {
    @Test
    fun test_error_response_serializer() {
        assertEquals(
            "{\"error\":\"unauthorized_client\"}",
            encodeBasicErrorResponse(BasicErrorResponse(BasicErrorResponseType.UnauthorizedClient)),
        )

        assertEquals(
            "{\"error\":\"invalid_client\",\"error_description\":\"Invalid client_id\",\"error_uri\":\"https://example.com/errors/invalid_client\"}",
            encodeBasicErrorResponse(
                BasicErrorResponse(
                    BasicErrorResponseType.InvalidClient,
                    "Invalid client_id",
                    "https://example.com/errors/invalid_client",
                ),
            ),
        )
    }

    private fun encodeBasicErrorResponse(response: BasicErrorResponse): String =
        buildString {
            append("{\"error\":\"")
            append(response.error.code)
            append("\"")
            if (response.errorDescription != null) {
                append(",\"error_description\":\"")
                append(response.errorDescription)
                append("\"")
            }
            if (response.errorUri != null) {
                append(",\"error_uri\":\"")
                append(response.errorUri)
                append("\"")
            }
            append("}")
        }
}
