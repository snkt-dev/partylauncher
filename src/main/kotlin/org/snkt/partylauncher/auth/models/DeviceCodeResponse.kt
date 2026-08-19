package org.snkt.partylauncher.auth.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceCodeResponse(
    @SerialName("user_code") val userCode: String,
    @SerialName("device_code") val deviceCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Long,
    val interval: Long = 5L,
    val message: String? = null
)

@Serializable
data class MicrosoftTokenResponse(
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val scope: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
)
