package com.example.ubuntuonandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkEnvironmentConfiguratorTest {

    @Test
    fun renderResolvConfUsesOnlyProvidedAndroidDnsServers() {
        val result = NetworkEnvironmentConfigurator.renderResolvConf(
            listOf("192.0.2.53", "2001:db8::53")
        )

        assertTrue(result.contains("nameserver 192.0.2.53"))
        assertTrue(result.contains("nameserver 2001:db8::53"))
        assertFalse(result.contains("8.8.8.8"))
        assertFalse(result.contains("1.1.1.1"))
    }

    @Test
    fun renderResolvConfDoesNotAddFallbackWhenOffline() {
        assertEquals(
            "# Managed by UbuntuOnAndroid from Android's active network.\n" +
                "# No DNS server is currently available.\n",
            NetworkEnvironmentConfigurator.renderResolvConf(emptyList())
        )
    }
}
