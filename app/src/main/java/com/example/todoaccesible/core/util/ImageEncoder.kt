package com.example.todoaccesible.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

private const val MAX_DIMENSION = 1600
private const val JPEG_QUALITY = 80

/**
 * Abre [uri] (content:// o file://), la redimensiona (borde largo ~1600px)
 * y la comprime a JPEG calidad ~80 antes de mandarla como base64 al backend
 * (`POST /diagnosticos/:id/respuestas`, campo `fotos`). Sin esto, una foto
 * de cámara moderna (varios MB) tardaría mucho o fallaría por el límite de
 * tamaño del body en el servidor.
 */
fun Context.encodeImageToDataUri(uri: Uri): String {
    val bitmap = decodeSampledBitmap(uri) ?: throw IllegalArgumentException("No se pudo leer la imagen: $uri")
    val resized = resizeIfNeeded(bitmap)
    val bytes = ByteArrayOutputStream().use { stream ->
        resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        stream.toByteArray()
    }
    if (resized !== bitmap) bitmap.recycle()
    resized.recycle()
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:image/jpeg;base64,$base64"
}

private fun Context.decodeSampledBitmap(uri: Uri): Bitmap? {
    // Primer paso: solo leer dimensiones para calcular el inSampleSize sin cargar todo a memoria.
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

    var sampleSize = 1
    var (width, height) = boundsOptions.outWidth to boundsOptions.outHeight
    while (width / (sampleSize * 2) >= MAX_DIMENSION || height / (sampleSize * 2) >= MAX_DIMENSION) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
}

private fun resizeIfNeeded(bitmap: Bitmap): Bitmap {
    val longSide = maxOf(bitmap.width, bitmap.height)
    if (longSide <= MAX_DIMENSION) return bitmap
    val scale = MAX_DIMENSION.toFloat() / longSide
    val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}
