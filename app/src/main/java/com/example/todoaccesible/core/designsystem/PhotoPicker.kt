package com.example.todoaccesible.core.designsystem

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

data class PhotoItem(val id: Long, val uri: Uri)

private fun createCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * Fila de fotos de una pregunta: miniaturas + botones para tomar/elegir foto.
 * `onRequestDelete` debe abrir el ConfirmDialog correspondiente antes de
 * borrar de verdad (confirmación obligatoria del spec).
 */
@Composable
fun PhotoPickerRow(
    photos: List<PhotoItem>,
    onPhotoAdded: (Uri) -> Unit,
    onRequestDelete: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCaptureUri?.let(onPhotoAdded)
        pendingCaptureUri = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onPhotoAdded) }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos, key = { it.id }) { photo ->
                Box(modifier = Modifier.size(72.dp)) {
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = "Foto de evidencia",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { onRequestDelete(photo) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Eliminar foto", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        IconButton(onClick = {
            val uri = createCaptureUri(context)
            pendingCaptureUri = uri
            cameraLauncher.launch(uri)
        }) {
            Icon(Icons.Filled.CameraAlt, contentDescription = "Tomar foto")
        }
        IconButton(onClick = {
            galleryLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = "Elegir de la galería")
        }
    }
}
