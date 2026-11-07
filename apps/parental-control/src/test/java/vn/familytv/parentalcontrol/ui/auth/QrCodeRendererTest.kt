package vn.familytv.parentalcontrol.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeRendererTest {
    @Test
    fun testRenderQrMatrixProducesSquare() {
        val matrix = QrCodeRenderer.renderQrMatrix("otpauth://totp/Test?secret=JBSWY3DPEHPK3PXP", 100)
        assertEquals(100, matrix.size)
        assertEquals(100, matrix[0].size)
        var blackCount = 0
        for (row in matrix) {
            for (pixel in row) {
                if (pixel) blackCount++
            }
        }
        assertTrue(blackCount > 0)
    }
}
