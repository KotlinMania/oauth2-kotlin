// port-lint: source oauth2/src/client.rs
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package io.github.kotlinmania.oauth2

import kotlin.native.HiddenFromObjC

/** Typestate base interface indicating whether an endpoint has been configured. */
public interface EndpointState

/** Typestate indicating that an endpoint has not been set. */
public class EndpointNotSet : EndpointState

/** Typestate indicating that an endpoint has been set and is ready to be used. */
public class EndpointSet : EndpointState

/** Typestate indicating that an endpoint may have been set. */
public class EndpointMaybeSet : EndpointState

/**
 * Stores the configuration for an OAuth2 client.
 */
@HiddenFromObjC
public class Client<
    TE : ErrorResponse,
    TR : TokenResponse,
    TIR : TokenIntrospectionResponse,
    RT : RevocableToken,
    TRE : ErrorResponse,
>(
    public val clientId: ClientId,
    public var clientSecret: ClientSecret? = null,
    public var authUrl: AuthUrl? = null,
    public var authType: AuthType = AuthType.BasicAuth,
    public var tokenUrl: TokenUrl? = null,
    public var redirectUrl: RedirectUrl? = null,
    public var introspectionUrl: IntrospectionUrl? = null,
    public var revocationUrl: RevocationUrl? = null,
    public var deviceAuthorizationUrl: DeviceAuthorizationUrl? = null,
    private val tokenResponseDeserializer: (ByteArray) -> TR,
    private val tokenErrorDeserializer: (ByteArray) -> TE,
    private val introspectionResponseDeserializer: (ByteArray) -> TIR,
    private val revocationErrorDeserializer: (ByteArray) -> TRE,
) {
    public fun clientId(): ClientId = clientId

    public fun clientSecret(): ClientSecret? = clientSecret

    public fun authType(): AuthType = authType

    public fun redirectUri(): RedirectUrl? = redirectUrl

    public fun redirectUrl(): RedirectUrl? = redirectUrl

    public fun authUri(): AuthUrl? = authUrl

    public fun authUrl(): AuthUrl? = authUrl

    public fun tokenUri(): TokenUrl? = tokenUrl

    public fun tokenUrl(): TokenUrl? = tokenUrl

    public fun introspectionUri(): IntrospectionUrl? = introspectionUrl

    public fun introspectionUrl(): IntrospectionUrl? = introspectionUrl

    public fun revocationUri(): RevocationUrl? = revocationUrl

    public fun revocationUrl(): RevocationUrl? = revocationUrl

    public fun deviceAuthorizationUri(): DeviceAuthorizationUrl? = deviceAuthorizationUrl

    public fun deviceAuthorizationUrl(): DeviceAuthorizationUrl? = deviceAuthorizationUrl

    public fun setAuthType(authType: AuthType): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.authType = authType
        }

    public fun setAuthUri(authUrl: AuthUrl): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.authUrl = authUrl
        }

    public fun setAuthUriOption(authUrl: AuthUrl?): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.authUrl = authUrl
        }

    public fun setClientSecret(clientSecret: ClientSecret): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.clientSecret = clientSecret
        }

    public fun setDeviceAuthorizationUrl(deviceAuthorizationUrl: DeviceAuthorizationUrl): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.deviceAuthorizationUrl = deviceAuthorizationUrl
        }

    public fun setDeviceAuthorizationUrlOption(deviceAuthorizationUrl: DeviceAuthorizationUrl?): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.deviceAuthorizationUrl = deviceAuthorizationUrl
        }

    public fun setIntrospectionUrl(introspectionUrl: IntrospectionUrl): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.introspectionUrl = introspectionUrl
        }

    public fun setIntrospectionUrlOption(introspectionUrl: IntrospectionUrl?): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.introspectionUrl = introspectionUrl
        }

    public fun setRedirectUri(redirectUrl: RedirectUrl): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.redirectUrl = redirectUrl
        }

    public fun setRevocationUrl(revocationUrl: RevocationUrl): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.revocationUrl = revocationUrl
        }

    public fun setRevocationUrlOption(revocationUrl: RevocationUrl?): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.revocationUrl = revocationUrl
        }

    public fun setTokenUri(tokenUrl: TokenUrl): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.tokenUrl = tokenUrl
        }

    public fun setTokenUriOption(tokenUrl: TokenUrl?): Client<TE, TR, TIR, RT, TRE> =
        apply {
            this.tokenUrl = tokenUrl
        }

    public fun authorizeUrl(stateFn: () -> CsrfToken = { CsrfToken.newRandom() }): AuthorizationRequest {
        val authUri = requireNotNull(authUrl) { "No authorization endpoint URL specified" }
        val req = AuthorizationRequest(authUri, clientId, stateFn())
        redirectUrl?.let { req.setRedirectUri(it) }
        return req
    }

    public fun exchangeClientCredentials(): ClientCredentialsTokenRequest<TE, TR> {
        val tokUri = requireNotNull(tokenUrl) { "No token endpoint URL specified" }
        return ClientCredentialsTokenRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            tokenUrl = tokUri,
            responseDeserializer = tokenResponseDeserializer,
            errorDeserializer = tokenErrorDeserializer,
        )
    }

    public fun exchangeCode(code: AuthorizationCode): CodeTokenRequest<TE, TR> {
        val tokUri = requireNotNull(tokenUrl) { "No token endpoint URL specified" }
        val req =
            CodeTokenRequest(
                authType = authType,
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                tokenUrl = tokUri,
                redirectUrl = redirectUrl,
                responseDeserializer = tokenResponseDeserializer,
                errorDeserializer = tokenErrorDeserializer,
            )
        return req
    }

    public fun exchangePassword(
        username: ResourceOwnerUsername,
        password: ResourceOwnerPassword,
    ): PasswordTokenRequest<TE, TR> {
        val tokUri = requireNotNull(tokenUrl) { "No token endpoint URL specified" }
        return PasswordTokenRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            username = username,
            password = password,
            tokenUrl = tokUri,
            responseDeserializer = tokenResponseDeserializer,
            errorDeserializer = tokenErrorDeserializer,
        )
    }

    public fun exchangeRefreshToken(refreshToken: RefreshToken): RefreshTokenRequest<TE, TR> {
        val tokUri = requireNotNull(tokenUrl) { "No token endpoint URL specified" }
        return RefreshTokenRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            refreshToken = refreshToken,
            tokenUrl = tokUri,
            responseDeserializer = tokenResponseDeserializer,
            errorDeserializer = tokenErrorDeserializer,
        )
    }

    public fun introspect(token: AccessToken): IntrospectionRequest<TE, TIR> {
        val introUri = requireNotNull(introspectionUrl) { "No introspection endpoint URL specified" }
        return IntrospectionRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            introspectionUrl = introUri,
            token = token,
            responseDeserializer = introspectionResponseDeserializer,
            errorDeserializer = tokenErrorDeserializer,
        )
    }

    public fun revokeToken(token: RT): RevocationRequest<RT, TRE> {
        val revUri = requireNotNull(revocationUrl) { "No revocation endpoint URL specified" }
        if (!revUri.url().startsWith("https://")) {
            throw ConfigurationError.InsecureUrl("revocation")
        }
        return RevocationRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            revocationUrl = revUri,
            token = token,
            errorDeserializer = revocationErrorDeserializer,
        )
    }

    public fun exchangeDeviceCode(): DeviceAuthorizationRequest<TE> {
        val devAuthUri = requireNotNull(deviceAuthorizationUrl) { "No device authorization endpoint URL specified" }
        return DeviceAuthorizationRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            deviceAuthorizationUrl = devAuthUri,
            errorDeserializer = tokenErrorDeserializer,
        )
    }

    public fun <EF : ExtraDeviceAuthorizationFields> exchangeDeviceAccessToken(
        authResponse: DeviceAuthorizationResponse<EF>,
    ): DeviceAccessTokenRequest<TR, EF> {
        val tokUri = requireNotNull(tokenUrl) { "No token endpoint URL specified" }
        return DeviceAccessTokenRequest(
            authType = authType,
            clientId = clientId,
            clientSecret = clientSecret,
            tokenUrl = tokUri,
            devAuthResp = authResponse,
            responseDeserializer = tokenResponseDeserializer,
        )
    }
}
