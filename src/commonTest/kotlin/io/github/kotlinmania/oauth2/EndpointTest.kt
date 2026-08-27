// port-lint: tests endpoint.rs
package io.github.kotlinmania.oauth2

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals

class EndpointTest {
    private fun newClient(): BasicClient =
        BasicClientFactory.new(ClientId.new("aaa"))
            .setAuthUri(AuthUrl.new("https://example.com/auth"))
            .setTokenUri(TokenUrl.new("https://example.com/token"))
            .setClientSecret(ClientSecret.new("bbb"))

    private fun runBlockingTest(block: suspend () -> Unit) {
        var completed = false
        var error: Throwable? = null
        block.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                completed = true
                error = result.exceptionOrNull()
            }
        })
        if (error != null) throw error!!
        if (!completed) error("Coroutine did not complete synchronously")
    }

    @Test
    fun testAsyncClientClosure() = runBlockingTest {
        val client = newClient()
        val httpResponse = HttpResponse(
            status = 200,
            headers = mapOf("content-type" to "application/json"),
            body = "{\"access_token\": \"12/34\", \"token_type\": \"BEARER\"}".encodeToByteArray(),
        )

        val asyncClient = AsyncHttpClient { _ ->
            httpResponse
        }

        val token = client
            .exchangeCode(AuthorizationCode.new("ccc"))
            .requestAsync(asyncClient)

        assertEquals("12/34", token.accessToken().secret())
    }
}
