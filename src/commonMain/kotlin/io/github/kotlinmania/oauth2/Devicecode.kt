// port-lint: source devicecode.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.native.HiddenFromObjC
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Trait for adding extra fields to the DeviceAuthorizationResponse. */
public interface ExtraDeviceAuthorizationFields

/** Empty (default) extra token fields. */
public class EmptyExtraDeviceAuthorizationFields : ExtraDeviceAuthorizationFields {
    override fun equals(other: Any?): Boolean = other is EmptyExtraDeviceAuthorizationFields

    override fun hashCode(): Int = 0

    override fun toString(): String = "EmptyExtraDeviceAuthorizationFields"
}

/** Standard OAuth2 device authorization response. */
@HiddenFromObjC
public class DeviceAuthorizationResponse<EF : ExtraDeviceAuthorizationFields>(
    private val deviceCode: DeviceCode,
    private val userCode: UserCode,
    private val verificationUri: EndUserVerificationUrl,
    private val verificationUriComplete: VerificationUriComplete? = null,
    private val expiresInSeconds: Long,
    private val intervalSeconds: Long = 5,
    private val extraFields: EF,
) {
    public fun deviceCode(): DeviceCode = deviceCode

    public fun userCode(): UserCode = userCode

    public fun verificationUri(): EndUserVerificationUrl = verificationUri

    public fun verificationUriComplete(): VerificationUriComplete? = verificationUriComplete

    public fun expiresIn(): Duration = expiresInSeconds.seconds

    public fun interval(): Duration = intervalSeconds.seconds

    public fun extraFields(): EF = extraFields

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeviceAuthorizationResponse<*>) return false
        if (deviceCode != other.deviceCode) return false
        if (userCode != other.userCode) return false
        if (verificationUri != other.verificationUri) return false
        if (verificationUriComplete != other.verificationUriComplete) return false
        if (expiresInSeconds != other.expiresInSeconds) return false
        if (intervalSeconds != other.intervalSeconds) return false
        if (extraFields != other.extraFields) return false
        return true
    }

    override fun hashCode(): Int {
        var result = deviceCode.hashCode()
        result = 31 * result + userCode.hashCode()
        result = 31 * result + verificationUri.hashCode()
        result = 31 * result + (verificationUriComplete?.hashCode() ?: 0)
        result = 31 * result + expiresInSeconds.hashCode()
        result = 31 * result + intervalSeconds.hashCode()
        result = 31 * result + extraFields.hashCode()
        return result
    }

    public companion object {
        public fun fromJsonString(
            jsonString: String,
        ): DeviceAuthorizationResponse<EmptyExtraDeviceAuthorizationFields> {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val deviceCode =
                DeviceCode.new(
                    requireNotNull(json["device_code"]?.jsonPrimitive?.contentOrNull) { "Missing device_code" },
                )
            val userCode =
                UserCode.new(
                    requireNotNull(json["user_code"]?.jsonPrimitive?.contentOrNull) { "Missing user_code" },
                )
            val uriStr =
                json["verification_uri"]?.jsonPrimitive?.contentOrNull
                    ?: json["verification_url"]?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing verification_uri / verification_url")
            val verificationUri = EndUserVerificationUrl.new(uriStr)
            val verificationUriComplete =
                json["verification_uri_complete"]?.jsonPrimitive?.contentOrNull?.let {
                    VerificationUriComplete.new(it)
                }
            val expiresIn = requireNotNull(json["expires_in"]?.jsonPrimitive?.longOrNull) { "Missing expires_in" }
            val interval = json["interval"]?.jsonPrimitive?.longOrNull ?: 5L

            return DeviceAuthorizationResponse(
                deviceCode = deviceCode,
                userCode = userCode,
                verificationUri = verificationUri,
                verificationUriComplete = verificationUriComplete,
                expiresInSeconds = expiresIn,
                intervalSeconds = interval,
                extraFields = EmptyExtraDeviceAuthorizationFields(),
            )
        }
    }
}

/** Standard implementation of DeviceAuthorizationResponse which throws away extra fields. */
public typealias StandardDeviceAuthorizationResponse =
    DeviceAuthorizationResponse<EmptyExtraDeviceAuthorizationFields>

/** Basic access token error types for device code OAuth2 flow. */
public sealed class DeviceCodeErrorResponseType : ErrorResponseType {
    public data object AuthorizationPending : DeviceCodeErrorResponseType() {
        override val value: String get() = "authorization_pending"

        override fun toString(): String = value
    }

    public data object SlowDown : DeviceCodeErrorResponseType() {
        override val value: String get() = "slow_down"

        override fun toString(): String = value
    }

    public data object AccessDenied : DeviceCodeErrorResponseType() {
        override val value: String get() = "access_denied"

        override fun toString(): String = value
    }

    public data object ExpiredToken : DeviceCodeErrorResponseType() {
        override val value: String get() = "expired_token"

        override fun toString(): String = value
    }

    public data class Basic(
        public val error: BasicErrorResponseType,
    ) : DeviceCodeErrorResponseType() {
        override val value: String get() = error.value

        override fun toString(): String = value
    }

    public companion object {
        public fun fromString(s: String): DeviceCodeErrorResponseType =
            when (s) {
                "authorization_pending" -> AuthorizationPending
                "slow_down" -> SlowDown
                "access_denied" -> AccessDenied
                "expired_token" -> ExpiredToken
                else -> Basic(BasicErrorResponseType.fromString(s))
            }
    }
}

/** Error response specialization for device code OAuth2 implementation. */
public typealias DeviceCodeErrorResponse = StandardErrorResponse<DeviceCodeErrorResponseType>

/** The request for a set of verification codes from the authorization server. */
@HiddenFromObjC
public class DeviceAuthorizationRequest<TE : ErrorResponse>(
    private val authType: AuthType,
    private val clientId: ClientId,
    private val clientSecret: ClientSecret?,
    private val deviceAuthorizationUrl: DeviceAuthorizationUrl,
    private val errorDeserializer: (ByteArray) -> TE,
) {
    private val extraParams = mutableListOf<Pair<String, String>>()
    private val scopes = mutableListOf<Scope>()

    public fun addExtraParam(name: String, value: String): DeviceAuthorizationRequest<TE> =
        apply {
            extraParams.add(name to value)
        }

    public fun addScope(scope: Scope): DeviceAuthorizationRequest<TE> =
        apply {
            scopes.add(scope)
        }

    public fun addScopes(scopes: Iterable<Scope>): DeviceAuthorizationRequest<TE> =
        apply {
            this.scopes.addAll(scopes)
        }

    public fun prepareRequest(): HttpRequest =
        endpointRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            extraParams = extraParams,
            redirectUrl = null,
            scopes = if (scopes.isNotEmpty()) scopes else null,
            url = deviceAuthorizationUrl.value,
            params = emptyList(),
        )

    public fun <EF : ExtraDeviceAuthorizationFields> request(
        httpClient: SyncHttpClient,
        deserializer: (ByteArray) -> DeviceAuthorizationResponse<EF>,
    ): DeviceAuthorizationResponse<EF> {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, deserializer, errorDeserializer)
    }

    public suspend fun <EF : ExtraDeviceAuthorizationFields> requestAsync(
        httpClient: AsyncHttpClient,
        deserializer: (ByteArray) -> DeviceAuthorizationResponse<EF>,
    ): DeviceAuthorizationResponse<EF> {
        val request = prepareRequest()
        val response = httpClient.call(request)
        return endpointResponse(response, deserializer, errorDeserializer)
    }
}

internal sealed class DeviceAccessTokenPollResult<out TR> {
    data class ContinueWithNewPollInterval(
        val newInterval: Duration,
    ) : DeviceAccessTokenPollResult<Nothing>()

    data class Done<TR>(
        val result: TR,
    ) : DeviceAccessTokenPollResult<TR>()
}

/** The request for a device access token from the authorization server. */
@HiddenFromObjC
public class DeviceAccessTokenRequest<TR : TokenResponse, EF : ExtraDeviceAuthorizationFields>(
    private val authType: AuthType,
    private val clientId: ClientId,
    private val clientSecret: ClientSecret?,
    private val tokenUrl: TokenUrl,
    private val devAuthResp: DeviceAuthorizationResponse<EF>,
    private val responseDeserializer: (ByteArray) -> TR,
) {
    private val extraParams = mutableListOf<Pair<String, String>>()
    private var maxBackoffInterval: Duration? = null

    public fun addExtraParam(name: String, value: String): DeviceAccessTokenRequest<TR, EF> =
        apply {
            extraParams.add(name to value)
        }

    public fun setMaxBackoffInterval(interval: Duration): DeviceAccessTokenRequest<TR, EF> =
        apply {
            this.maxBackoffInterval = interval
        }

    public fun prepareRequest(): HttpRequest =
        endpointRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            extraParams = extraParams,
            redirectUrl = null,
            scopes = null,
            url = tokenUrl.value,
            params =
                listOf(
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    "device_code" to devAuthResp.deviceCode().secret(),
                ),
        )

    public fun request(
        httpClient: SyncHttpClient,
        sleepFn: (Duration) -> Unit,
        timeout: Duration? = null,
    ): TR {
        var interval = devAuthResp.interval()
        while (true) {
            val response =
                try {
                    httpClient.call(prepareRequest())
                } catch (e: Throwable) {
                    val maxBackoff = maxBackoffInterval ?: 10.seconds
                    val newInterval = minOf(interval * 2, maxBackoff)
                    interval = newInterval
                    sleepFn(interval)
                    continue
                }

            if (response.status != 200) {
                val errorResp =
                    try {
                        val json = Json.parseToJsonElement(response.body.decodeToString()).jsonObject
                        val errCode = json["error"]?.jsonPrimitive?.contentOrNull ?: ""
                        val errDesc = json["error_description"]?.jsonPrimitive?.contentOrNull
                        val errUri = json["error_uri"]?.jsonPrimitive?.contentOrNull
                        DeviceCodeErrorResponse.new(
                            DeviceCodeErrorResponseType.fromString(errCode),
                            errDesc,
                            errUri,
                        )
                    } catch (e: Throwable) {
                        throw RequestTokenError.Parse(e.message ?: "Failed to parse error", response.body)
                    }

                when (errorResp.error()) {
                    is DeviceCodeErrorResponseType.AuthorizationPending -> {
                        sleepFn(interval)
                        continue
                    }
                    is DeviceCodeErrorResponseType.SlowDown -> {
                        interval += 5.seconds
                        sleepFn(interval)
                        continue
                    }
                    else -> throw RequestTokenError.ServerResponse(errorResp)
                }
            } else {
                return endpointResponse(response, responseDeserializer)
            }
        }
    }

    public suspend fun requestAsync(
        httpClient: AsyncHttpClient,
        sleepFn: suspend (Duration) -> Unit,
        timeout: Duration? = null,
    ): TR {
        var interval = devAuthResp.interval()
        while (true) {
            val response =
                try {
                    httpClient.call(prepareRequest())
                } catch (e: Throwable) {
                    val maxBackoff = maxBackoffInterval ?: 10.seconds
                    val newInterval = minOf(interval * 2, maxBackoff)
                    interval = newInterval
                    sleepFn(interval)
                    continue
                }

            if (response.status != 200) {
                val errorResp =
                    try {
                        val json = Json.parseToJsonElement(response.body.decodeToString()).jsonObject
                        val errCode = json["error"]?.jsonPrimitive?.contentOrNull ?: ""
                        val errDesc = json["error_description"]?.jsonPrimitive?.contentOrNull
                        val errUri = json["error_uri"]?.jsonPrimitive?.contentOrNull
                        DeviceCodeErrorResponse.new(
                            DeviceCodeErrorResponseType.fromString(errCode),
                            errDesc,
                            errUri,
                        )
                    } catch (e: Throwable) {
                        throw RequestTokenError.Parse(e.message ?: "Failed to parse error", response.body)
                    }

                when (errorResp.error()) {
                    is DeviceCodeErrorResponseType.AuthorizationPending -> {
                        sleepFn(interval)
                        continue
                    }
                    is DeviceCodeErrorResponseType.SlowDown -> {
                        interval += 5.seconds
                        sleepFn(interval)
                        continue
                    }
                    else -> throw RequestTokenError.ServerResponse(errorResp)
                }
            } else {
                return endpointResponse(response, responseDeserializer)
            }
        }
    }
}
