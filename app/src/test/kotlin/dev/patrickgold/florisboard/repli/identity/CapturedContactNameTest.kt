package dev.patrickgold.florisboard.repli.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CapturedContactNameTest {
    @Test fun `normalizes a person title and keeps phone number identities`() {
        assertEquals("Abdulhakeem", CapturedContactName.prepare("  Abdulhakeem  "))
        assertEquals("+234 801 234 5678", CapturedContactName.prepare("+234  801 234 5678"))
    }

    @Test fun `rejects missing generic group and oversized titles`() {
        listOf(null, "", "WhatsApp", "online", "last seen today", "18 participants",
            "10:48 00:00", "10:48", "00:00", "10 48 00 00", "5G", "x".repeat(81))
            .forEach { assertNull(CapturedContactName.prepare(it)) }
    }

    @Test fun `uses a chat title from the locally read header`() {
        assertEquals("Abdulhakeem", CapturedContactName.fromHeaderLines(listOf("Abdulhakeem", "online")))
        assertEquals("Alex", CapturedContactName.fromHeaderLines(listOf("Alex · practice chat")))
        assertEquals("Abdulhakeem", CapturedContactName.fromHeaderLines(
            listOf("10:48 00:00", "Abdulhakeem", "online")))
        assertNull(CapturedContactName.fromHeaderLines(listOf("WhatsApp")))
        assertNull(CapturedContactName.fromHeaderLines(listOf("Weekend plans", "Ada, Ben, Chika")))
    }
}
