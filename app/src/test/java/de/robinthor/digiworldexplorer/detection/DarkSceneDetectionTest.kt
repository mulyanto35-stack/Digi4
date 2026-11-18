package de.robinthor.digiworldexplorer.detection

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

/**
 * Second real device frame (Samsung SM-G998B, 1080x2400) with a dark night scene and an opened
 * reward panel. The geometry is identical to the bright frame, but the line confidence drops
 * from ~0.84 to ~0.74. This test pins down that detection stays geometrically exact - so
 * calibration must not rely on a high contrast threshold.
 */
class DarkSceneDetectionTest {

    private fun frame(name: String): Triple<Int, Int, IntArray> {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(name)) { "missing $name" }
        val image = stream.use { ImageIO.read(it) }
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        return Triple(image.width, image.height, pixels)
    }

    @Test
    fun detectsGridOnDarkSceneFrame() {
        val (width, height, pixels) = frame("samsung_reward_panel.png")
        val detection = GridDetector.detect(width, height, pixels)
        assertNotNull("kein Raster auf dunklem Frame erkannt", detection)
        detection!!

        val b = detection.bounds
        assertTrue("linke Kante ${b.left}", b.left in 15..40)
        assertTrue("obere Kante ${b.top}", b.top in 835..860)
        assertTrue("rechte Kante ${b.right}", b.right in 1005..1035)
        assertTrue("untere Kante ${b.bottom}", b.bottom in 1650..1680)
        assertTrue("Konfidenz ${detection.confidence} unter Kandidatenschwelle", detection.confidence >= .55)
    }
}
