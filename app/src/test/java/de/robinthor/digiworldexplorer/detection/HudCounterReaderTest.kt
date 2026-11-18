package de.robinthor.digiworldexplorer.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

/**
 * Real device frame with paws 197, claws 1 and Dash 2. The digits are outline glyphs, the
 * templates come from exactly this image - so the test mainly ensures that band position,
 * segmentation and normalisation stay stable when the recognition is being tweaked.
 */
class HudCounterReaderTest {

    private fun frame(name: String = "samsung_hud.png"): Triple<Int, Int, IntArray> {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(name))
        val image = stream.use { ImageIO.read(it) }
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        return Triple(image.width, image.height, pixels)
    }

    private val bounds = GridBounds(27, 847, 1021, 1664)

    @Test
    fun readsClawsAndDash() {
        val (w, h, px) = frame()
        val hud = HudCounterReader.read(w, h, px, bounds)
        assertEquals(1, hud.claws)
        assertEquals(2, hud.dash)
    }

    /**
     * Second device frame, captured after a claw had regrown. Here the overlay is already in the
     * image - so the box around the number must not cover it.
     */
    @Test
    fun readsRegeneratedClawCount() {
        val (w, h, px) = frame("samsung_hud_three.png")
        val hud = HudCounterReader.read(w, h, px, bounds)
        assertEquals(3, hud.claws)
        assertEquals(2, hud.dash)
        assertTrue("keine unbekannten Formen erwartet: ${hud.unknown}", hud.unknown.isEmpty())
    }

    @Test
    fun reportsBoxesForOverlay() {
        val (w, h, px) = frame()
        val hud = HudCounterReader.read(w, h, px, bounds)
        val claws = assertNotNull(hud.clawsBox).let { hud.clawsBox!! }
        val dash = assertNotNull(hud.dashBox).let { hud.dashBox!! }
        // Gemessen: Krallenzahl y=2142..2171, Dashzahl y=2227..2257, beide ab x=289.
        assertTrue("Krallenkasten $claws", claws.top in 2135..2150 && claws.bottom in 2165..2180)
        assertTrue("Dashkasten $dash", dash.top in 2220..2235 && dash.bottom in 2250..2265)
        assertTrue("Kaesten duerfen sich nicht ueberlappen", claws.bottom < dash.top)
    }

    @Test
    fun rejectsUnknownGlyphsInsteadOfGuessing() {
        val (w, h, px) = frame()
        // Das Pfotenband traegt eine dreistellige Zahl, deren Ziffern sich beruehren. Genau so ein
        // Fall darf keine erfundene Zahl liefern.
        val paws = HudCounterReader.readBand(w, h, px, bounds, HudCounterReader.PAWS_BAND)
        assertNotNull(paws)
        assertEquals(null, paws!!.first)
    }
}
