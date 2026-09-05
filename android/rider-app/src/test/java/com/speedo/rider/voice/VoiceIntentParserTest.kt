package com.speedo.rider.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceIntentParserTest {

    @Test
    fun testHindiAutoBooking() {
        val input = "Bhaiya, mujhe Ranchi Railway station jana hai auto se."
        val result = VoiceIntentParser.parse(input)
        assertEquals("auto", result.vehicleType)
        assertTrue(
            "Expected 'Ranchi Railway station', got '${result.cleanDestination}'",
            result.cleanDestination.equals("Ranchi Railway station", ignoreCase = true)
        )
    }

    @Test
    fun testEnglishBikeBooking() {
        val input = "Kolkata airport, bike ride."
        val result = VoiceIntentParser.parse(input)
        assertEquals("bike", result.vehicleType)
        assertTrue(
            "Expected 'Kolkata airport', got '${result.cleanDestination}'",
            result.cleanDestination.equals("Kolkata airport", ignoreCase = true)
        )
    }

    @Test
    fun testCabBooking() {
        val input = "Main road jana hai cab se"
        val result = VoiceIntentParser.parse(input)
        assertEquals("car", result.vehicleType)
        assertTrue(
            "Expected 'Main road', got '${result.cleanDestination}'",
            result.cleanDestination.equals("Main road", ignoreCase = true)
        )
    }

    @Test
    fun testSimpleDestinationWithoutVehicle() {
        val input = "Lalpur Chowk"
        val result = VoiceIntentParser.parse(input)
        assertEquals(null, result.vehicleType)
        assertEquals("Lalpur Chowk", result.cleanDestination)
    }

    @Test
    fun testEnglishPrefixTakeMeTo() {
        val input = "Please take me to HSR Layout by bike"
        val result = VoiceIntentParser.parse(input)
        assertEquals("bike", result.vehicleType)
        assertEquals("HSR Layout", result.cleanDestination)
    }
}
