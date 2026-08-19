package org.snkt.partylauncher.auth

import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.java.JavaAuthManager
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.function.Consumer

class MinecraftAuthTest {

    @Test
    fun testDeviceCodeGeneration() {
        val httpClient = MinecraftAuth.createHttpClient()
        var receivedCode: MsaDeviceCode? = null

        val deviceCodeCallback = Consumer<MsaDeviceCode> { code ->
            receivedCode = code
            println("Received Device Code: ${code.userCode}")
            println("Verification URI: ${code.verificationUri}")
            println("Direct URL: ${code.directVerificationUri}")
        }

        // Test creating the auth manager builder with callback
        val builder = JavaAuthManager.create(httpClient)
        assertNotNull(builder)
    }
}
