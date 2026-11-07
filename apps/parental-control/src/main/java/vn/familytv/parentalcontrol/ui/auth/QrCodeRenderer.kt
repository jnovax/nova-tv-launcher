package vn.familytv.parentalcontrol.ui.auth

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeRenderer {
    fun renderQrMatrix(content: String, size: Int = 300): Array<BooleanArray> {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val result = Array(size) { BooleanArray(size) }
        for (y in 0 until size) {
            for (x in 0 until size) {
                result[y][x] = bitMatrix.get(x, y)
            }
        }
        return result
    }

    fun renderQrBitmap(content: String, size: Int = 300): Bitmap {
        val matrix = renderQrMatrix(content, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) {
            for (x in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[y][x]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
