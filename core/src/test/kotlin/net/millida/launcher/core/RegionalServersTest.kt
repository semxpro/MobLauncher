package net.millida.launcher.core

import net.millida.launcher.core.servers.RegionalServers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalServersTest {
    @Test
    fun `suggestions cover Bangladesh and the United States with valid addresses`() {
        val suggestions = RegionalServers.suggestions

        assertEquals(5, suggestions.size)
        assertTrue(suggestions.all { it.suggested && it.joinAddress.isNotBlank() })
        assertEquals(
            suggestions.size,
            suggestions.map { it.joinAddress.lowercase() }.distinct().size,
        )
        assertEquals(3, suggestions.count { it.shortDesc.startsWith("Bangladesh") })
        assertEquals(2, suggestions.count { it.shortDesc.startsWith("United States") })
        assertEquals("pe.bdzonemc.com:19132", suggestions.first().bedrockAddress)
    }
}
