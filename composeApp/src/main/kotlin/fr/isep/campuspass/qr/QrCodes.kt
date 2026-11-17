package fr.isep.campuspass.qr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Generates and reads QR codes with ZXing. */
object QrCodes {

    /** Draws [content] as a square QR code bitmap of [sizePx] pixels. */
    fun generate(content: String, sizePx: Int = 720): Bitmap {
        // TODO(Exercise 1): draw the QR code of [content] with ZXing.
        //  1. Create the hints: error correction level M and a margin of 1.
        //  2. val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        //  3. Build an IntArray of matrix.width * matrix.height pixels: Color.BLACK if matrix[x, y] is true, else Color.WHITE.
        //  4. return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        // Placeholder: a grey square, so that the app still runs.
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.LTGRAY) }
    }

    /** Reads a QR code from a picture (e.g. a screenshot of a ticket). Returns null if none is found. */
    fun decode(bitmap: Bitmap): String? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        return try {
            QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source)), mapOf(DecodeHintType.TRY_HARDER to true)).text
        } catch (e: Exception) {
            null
        }
    }

    fun decode(context: Context, uri: Uri): String? {
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        return decode(bitmap)
    }
}
