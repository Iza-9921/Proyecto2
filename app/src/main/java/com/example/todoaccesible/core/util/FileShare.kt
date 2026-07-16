package com.example.todoaccesible.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object FileShare {
    /** Abre el selector de "compartir/guardar" del sistema para el archivo exportado (PDF/Excel). */
    fun share(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir ${file.name}"))
    }
}
