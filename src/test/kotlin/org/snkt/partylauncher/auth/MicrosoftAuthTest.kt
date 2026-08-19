package org.snkt.partylauncher.auth

import net.raphimc.minecraftauth.MinecraftAuth
import net.raphimc.minecraftauth.java.JavaAuthManager
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class MicrosoftAuthTest {

    @Test
    fun testJavaAuthManagerInitialization() {
        val httpClient = MinecraftAuth.createHttpClient()
        val builder = JavaAuthManager.create(httpClient)
        assertNotNull(builder)
        val authService = MicrosoftAuthService()
        assertNotNull(authService)
    }
}
