package com.example.todoaccesible.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.voice.VoiceGuideController

/**
 * Botón flotante de guía de voz, visible en cualquier pantalla mientras el
 * dispositivo tenga español disponible en su motor de texto a voz.
 * Equivalente a `VoiceHelpButton.jsx` en la web.
 */
@Composable
fun VoiceHelpButton(controller: VoiceGuideController, modifier: Modifier = Modifier) {
    val supported by controller.supported.collectAsState()
    val speaking by controller.speaking.collectAsState()
    if (!supported) return

    IconButton(
        onClick = controller::toggle,
        modifier = modifier.shadow(elevation = 4.dp, shape = CircleShape),
        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(
            imageVector = if (speaking) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = if (speaking) "Detener la lectura de instrucciones" else "Escuchar instrucciones de esta pantalla",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}
