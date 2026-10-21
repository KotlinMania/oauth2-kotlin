// port-lint: source devicecode.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import io.github.kotlinmania.http.StatusCode
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.IgnoredAny
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.fromSlice
import io.github.kotlinmania.serdejson.fromStr
import io.github.kotlinmania.serdejson.toStr
import kotlin.native.HiddenFromObjC
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * The minimum amount of time in seconds that the client SHOULD wait
 * between polling requests to the token endpoint. If no value is
 * provided, clients MUST use 5 as the default.
 */
public fun defaultDevicecodeInterval(): ULong = 5uL

public fun <D : Deserializer> deserializeDevicecodeInterval(deserializer: D): SerdeResult<ULong> {
    class NumOrNull : Visitor<ULong> {
        override fun expecting(): String = "non-negative integer or null"

        override fun visitU64(v: ULong): SerdeResult<ULong> = SerdeResult.success(v)

        override fun visitI64(v: Long): SerdeResult<ULong> =
            if (v >= 0) SerdeResult.success(v.toULong()) else SerdeResult.failure(SerdeError.custom("negative integer"))

        override fun visitUnit(): SerdeResult<ULong> = SerdeResult.success(defaultDevicecodeInterval())

        override fun visitNone(): SerdeResult<ULong> = SerdeResult.success(defaultDevicecodeInterval())
    }
    return deserializer.deserializeAny(NumOrNull())
}

/** Trait for adding extra fields to the DeviceAuthorizationResponse. */
public interface ExtraDeviceAuthorizationFields : Serialize {
    public fun <Ok> appendFields(struct: SerializeStruct<Ok>) {}
}

/** Empty (default) extra token fields. */
public class EmptyExtraDeviceAuthorizationFields : ExtraDeviceAuthorizationFields {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        val s = serializer.serializeStruct("EmptyExtraDeviceAuthorizationFields", 0).getOrThrow()
        return s.end()
    }

    override fun equals(other: Any?): Boolean = other is EmptyExtraDeviceAuthorizationFields

    override fun hashCode(): Int = 0

    override fun toString(): String = "EmptyExtraDeviceAuthorizationFields"

    public companion object : Deserialize<EmptyExtraDeviceAuthorizationFields> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<EmptyExtraDeviceAuthorizationFields> =
            SerdeResult.success(EmptyExtraDeviceAuthorizationFields())
    }
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
) : Serialize {
    public fun deviceCode(): DeviceCode = deviceCode

    public fun userCode(): UserCode = userCode

    public fun verificationUri(): EndUserVerificationUrl = verificationUri

    public fun verificationUriComplete(): VerificationUriComplete? = verificationUriComplete

    public fun expiresIn(): Duration = expiresInSeconds.seconds

    public fun interval(): Duration = intervalSeconds.seconds

    public fun extraFields(): EF = extraFields

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        var count = 4
        if (verificationUriComplete != null) count++
        val struct = serializer.serializeStruct("DeviceAuthorizationResponse", count).getOrThrow()
        struct.serializeField("device_code", deviceCode.secret()).getOrThrow()
        struct.serializeField("user_code", userCode.secret()).getOrThrow()
        struct.serializeField("verification_uri", verificationUri.url()).getOrThrow()
        if (verificationUriComplete != null) {
            struct.serializeField("verification_uri_complete", verificationUriComplete.secret()).getOrThrow()
        }
        struct.serializeField("expires_in", expiresInSeconds).getOrThrow()
        struct.serializeField("interval", intervalSeconds).getOrThrow()
        extraFields.appendFields(struct)
        return struct.end()
    }

    public fun toJsonString(): String = toStr(this).getOrThrow()

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

    public class DeserializerImpl<EF : ExtraDeviceAuthorizationFields>(
        private val extraFieldsDeserialize: Deserialize<EF>,
    ) : Deserialize<DeviceAuthorizationResponse<EF>> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DeviceAuthorizationResponse<EF>> {
            class VisitorImpl : Visitor<DeviceAuthorizationResponse<EF>> {
                override fun expecting(): String = "struct DeviceAuthorizationResponse"

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<DeviceAuthorizationResponse<EF>> {
                    var deviceCode: DeviceCode? = null
                    var userCode: UserCode? = null
                    var verificationUri: EndUserVerificationUrl? = null
                    var verificationUriComplete: VerificationUriComplete? = null
                    var expiresIn: Long? = null
                    var interval: Long = 5L

                    while (true) {
                        val key = access.nextKey(StringDeserialize).getOrThrow() ?: break
                        when (key) {
                            "device_code" -> {
                                val s = access.nextValue(StringDeserialize).getOrThrow()
                                deviceCode = DeviceCode.new(s)
                            }

                            "user_code" -> {
                                val s = access.nextValue(StringDeserialize).getOrThrow()
                                userCode = UserCode.new(s)
                            }

                            "verification_uri", "verification_url" -> {
                                val s = access.nextValue(StringDeserialize).getOrThrow()
                                verificationUri = EndUserVerificationUrl.new(s)
                            }

                            "verification_uri_complete" -> {
                                val s = access.nextValue(StringDeserialize).getOrThrow()
                                verificationUriComplete = VerificationUriComplete.new(s)
                            }

                            "expires_in" -> {
                                expiresIn = access.nextValue(I64Deserialize).getOrThrow()
                            }

                            "interval" -> {
                                val u =
                                    access
                                        .nextValue(
                                            object : Deserialize<ULong> {
                                                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<ULong> =
                                                    deserializeDevicecodeInterval(deserializer)
                                            },
                                        ).getOrThrow()
                                interval = u.toLong()
                            }

                            else -> {
                                access.nextValue(IgnoredAny).getOrThrow()
                            }
                        }
                    }
                    val dc = deviceCode ?: return SerdeResult.failure(SerdeError.missingField("device_code"))
                    val uc = userCode ?: return SerdeResult.failure(SerdeError.missingField("user_code"))
                    val vu = verificationUri ?: return SerdeResult.failure(SerdeError.missingField("verification_uri"))
                    val exp = expiresIn ?: return SerdeResult.failure(SerdeError.missingField("expires_in"))
                    val ef = extraFieldsDeserialize.deserialize(deserializer).getOrThrow()
                    return SerdeResult.success(
                        DeviceAuthorizationResponse(
                            deviceCode = dc,
                            userCode = uc,
                            verificationUri = vu,
                            verificationUriComplete = verificationUriComplete,
                            expiresInSeconds = exp,
                            intervalSeconds = interval,
                            extraFields = ef,
                        ),
                    )
                }
            }
            return deserializer.deserializeStruct(
                "DeviceAuthorizationResponse",
                listOf("device_code", "user_code", "verification_uri", "verification_uri_complete", "expires_in", "interval"),
                VisitorImpl(),
            )
        }
    }

    public companion object {
        public fun <EF : ExtraDeviceAuthorizationFields> deserializer(
            extraFieldsDeserialize: Deserialize<EF>,
        ): Deserialize<DeviceAuthorizationResponse<EF>> = DeserializerImpl(extraFieldsDeserialize)

        public fun fromJsonString(
            jsonString: String,
        ): DeviceAuthorizationResponse<EmptyExtraDeviceAuthorizationFields> =
            fromStr(jsonString, deserializer(EmptyExtraDeviceAuthorizationFields)).getOrThrow()
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
        override val value: String get() = error.asRef()

        override fun toString(): String = value
    }

    public fun asRef(): String =
        when (this) {
            AuthorizationPending -> "authorization_pending"
            SlowDown -> "slow_down"
            AccessDenied -> "access_denied"
            ExpiredToken -> "expired_token"
            is Basic -> error.asRef()
        }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStr(asRef())

    override fun toString(): String = asRef()

    public companion object : Deserialize<DeviceCodeErrorResponseType> {
        public fun fromStr(s: String): DeviceCodeErrorResponseType =
            when (s) {
                "authorization_pending" -> AuthorizationPending
                "slow_down" -> SlowDown
                "access_denied" -> AccessDenied
                "expired_token" -> ExpiredToken
                else -> Basic(BasicErrorResponseType.fromStr(s))
            }

        public fun fromString(s: String): DeviceCodeErrorResponseType = fromStr(s)

        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<DeviceCodeErrorResponseType> {
            val variantStr = StringDeserialize.deserialize(deserializer).getOrThrow()
            return SerdeResult.success(fromStr(variantStr))
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
        val response = invokeSyncHttpClient(httpClient, request)
        return endpointResponse(response, deserializer, errorDeserializer)
    }

    public suspend fun <EF : ExtraDeviceAuthorizationFields> requestAsync(
        httpClient: AsyncHttpClient,
        deserializer: (ByteArray) -> DeviceAuthorizationResponse<EF>,
    ): DeviceAuthorizationResponse<EF> {
        val request = prepareRequest()
        val response = invokeAsyncHttpClient(httpClient, request)
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
    private val baseTimeMark = TimeSource.Monotonic.markNow()
    private var timeFn: () -> Duration = { baseTimeMark.elapsedNow() }

    public fun addExtraParam(name: String, value: String): DeviceAccessTokenRequest<TR, EF> =
        apply {
            extraParams.add(name to value)
        }

    public fun setMaxBackoffInterval(interval: Duration): DeviceAccessTokenRequest<TR, EF> =
        apply {
            this.maxBackoffInterval = interval
        }

    public fun setTimeFn(timeFn: () -> Duration): DeviceAccessTokenRequest<TR, EF> =
        apply {
            this.timeFn = timeFn
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
        val timeoutDur = timeout ?: devAuthResp.expiresIn()
        val timeoutDt = timeFn() + timeoutDur
        var interval = devAuthResp.interval()
        while (true) {
            val now = timeFn()
            if (now > timeoutDt) {
                throw RequestTokenError.ServerResponse(
                    DeviceCodeErrorResponse.new(
                        DeviceCodeErrorResponseType.ExpiredToken,
                        "This device code has expired.",
                        null,
                    ),
                )
            }

            val response =
                try {
                    httpClient.call(prepareRequest())
                } catch (e: AssertionError) {
                    throw e
                } catch (e: Throwable) {
                    val maxBackoff = maxBackoffInterval ?: 10.seconds
                    val newInterval = minOf(interval * 2, maxBackoff)
                    interval = newInterval
                    sleepFn(interval)
                    continue
                }

            if (!StatusCode.OK.eq(response.status)) {
                val errorResp =
                    try {
                        fromSlice(
                            response.body,
                            DeviceCodeErrorResponse.deserializer(DeviceCodeErrorResponseType),
                        ).getOrThrow()
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

                    else -> {
                        throw RequestTokenError.ServerResponse(errorResp)
                    }
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
        val timeoutDur = timeout ?: devAuthResp.expiresIn()
        val timeoutDt = timeFn() + timeoutDur
        var interval = devAuthResp.interval()
        while (true) {
            val now = timeFn()
            if (now > timeoutDt) {
                throw RequestTokenError.ServerResponse(
                    DeviceCodeErrorResponse.new(
                        DeviceCodeErrorResponseType.ExpiredToken,
                        "This device code has expired.",
                        null,
                    ),
                )
            }

            val response =
                try {
                    httpClient.call(prepareRequest())
                } catch (e: AssertionError) {
                    throw e
                } catch (e: Throwable) {
                    val maxBackoff = maxBackoffInterval ?: 10.seconds
                    val newInterval = minOf(interval * 2, maxBackoff)
                    interval = newInterval
                    sleepFn(interval)
                    continue
                }

            if (!StatusCode.OK.eq(response.status)) {
                val errorResp =
                    try {
                        fromSlice(
                            response.body,
                            DeviceCodeErrorResponse.deserializer(DeviceCodeErrorResponseType),
                        ).getOrThrow()
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

                    else -> {
                        throw RequestTokenError.ServerResponse(errorResp)
                    }
                }
            } else {
                return endpointResponse(response, responseDeserializer)
            }
        }
    }
}
