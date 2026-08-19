package org.snkt.partylauncher.minecraft

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class JavaRuntimeTest {

    @Test
    fun testRequiredJavaVersion() {
        assertEquals(21, JavaRuntime.getRequiredJavaVersion("1.21.4"))
        assertEquals(21, JavaRuntime.getRequiredJavaVersion("1.21.8"))
        assertEquals(21, JavaRuntime.getRequiredJavaVersion("1.20.5"))
        assertEquals(17, JavaRuntime.getRequiredJavaVersion("1.20.4"))
        assertEquals(17, JavaRuntime.getRequiredJavaVersion("1.18.2"))
        assertEquals(16, JavaRuntime.getRequiredJavaVersion("1.17.1"))
        assertEquals(8, JavaRuntime.getRequiredJavaVersion("1.16.5"))
    }

    @Test
    fun testParseMajorVersion() {
        assertEquals(8, JavaRuntime.parseMajorVersion("1.8.0_352"))
        assertEquals(17, JavaRuntime.parseMajorVersion("17.0.9"))
        assertEquals(21, JavaRuntime.parseMajorVersion("21.0.2"))
        assertEquals(26, JavaRuntime.parseMajorVersion("26.0.2"))
    }

    @Test
    fun testParseJavaVersionOutput() {
        val dummyPath = Paths.get("/dummy/java")
        val openJdkOutput = """
            openjdk version "21.0.2" 2024-01-16
            OpenJDK Runtime Environment (build 21.0.2+13-58)
            OpenJDK 64-Bit Server VM (build 21.0.2+13-58, mixed mode, sharing)
        """.trimIndent()

        val parsed = JavaRuntime.parseJavaVersion(openJdkOutput, dummyPath)
        assertNotNull(parsed)
        assertEquals(21, parsed?.majorVersion)

        val oracleOutput = """
            java version "1.8.0_202"
            Java(TM) SE Runtime Environment (build 1.8.0_202-b08)
            Java HotSpot(TM) 64-Bit Server VM (build 25.202-b08, mixed mode)
        """.trimIndent()

        val parsedLegacy = JavaRuntime.parseJavaVersion(oracleOutput, dummyPath)
        assertNotNull(parsedLegacy)
        assertEquals(8, parsedLegacy?.majorVersion)
    }
}
