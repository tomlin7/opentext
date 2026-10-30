package com.tom7.opentext

import com.tom7.opentext.data.normalizeServerUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerUrlTest {
    @Test
    fun addsHttpWhenSchemeIsMissing() {
        assertEquals("http://192.168.1.10:4096", normalizeServerUrl("192.168.1.10:4096"))
    }

    @Test
    fun dropsTrailingSlash() {
        assertEquals("http://10.0.2.2:4097", normalizeServerUrl("http://10.0.2.2:4097/"))
    }

    @Test
    fun keepsHttps() {
        assertEquals("https://tunnel.example.dev", normalizeServerUrl(" https://tunnel.example.dev "))
    }

    @Test
    fun rejectsBlankInput() {
        assertNull(normalizeServerUrl("   "))
    }

    @Test
    fun rejectsEmbeddedCredentials() {
        assertNull(normalizeServerUrl("http://user:secret@example.dev:4096"))
    }

    @Test
    fun rejectsUnsupportedScheme() {
        assertNull(normalizeServerUrl("ftp://example.dev"))
    }

    @Test
    fun rejectsQueryAndFragment() {
        assertNull(normalizeServerUrl("http://example.dev:4096/?a=1"))
        assertNull(normalizeServerUrl("http://example.dev:4096/#x"))
    }
}
