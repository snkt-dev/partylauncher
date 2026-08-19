package org.snkt.partylauncher.auth.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XboxLiveAuthRequest(
    @SerialName("Properties") val properties: XboxLiveProperties,
    @SerialName("RelyingParty") val relyingParty: String = "http://auth.xboxlive.com",
    @SerialName("TokenType") val tokenType: String = "JWT"
)

@Serializable
data class XboxLiveProperties(
    @SerialName("AuthMethod") val authMethod: String = "RPS",
    @SerialName("SiteName") val siteName: String = "user.auth.xboxlive.com",
    @SerialName("RpsTicket") val rpsTicket: String
)

@Serializable
data class XstsAuthRequest(
    @SerialName("Properties") val properties: XstsProperties,
    @SerialName("RelyingParty") val relyingParty: String = "rp://api.minecraftservices.com/",
    @SerialName("TokenType") val tokenType: String = "JWT"
)

@Serializable
data class XstsProperties(
    @SerialName("SandboxId") val sandboxId: String = "RETAIL",
    @SerialName("UserTokens") val userTokens: List<String>
)

@Serializable
data class XboxAuthResponse(
    @SerialName("IssueInstant") val issueInstant: String? = null,
    @SerialName("NotAfter") val notAfter: String? = null,
    @SerialName("Token") val token: String? = null,
    @SerialName("DisplayClaims") val displayClaims: DisplayClaims? = null,
    @SerialName("XErr") val xErr: Long? = null,
    @SerialName("Message") val message: String? = null
)

@Serializable
data class DisplayClaims(
    @SerialName("xui") val xui: List<XuiClaim> = emptyList()
)

@Serializable
data class XuiClaim(
    val uhs: String? = null,
    val xid: String? = null,
    val gtg: String? = null
)

@Serializable
data class MinecraftLoginRequest(
    val identityToken: String
)

@Serializable
data class MinecraftLoginResponse(
    val username: String? = null,
    val roles: List<String> = emptyList(),
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val error: String? = null,
    val errorMessage: String? = null
)
