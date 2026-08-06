package com.example.todoaccesible.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.example.todoaccesible.core.theme.LocalThemeController

/**
 * Botón flotante para alternar claro/oscuro, visible en cualquier pantalla
 * (equivalente a que `ThemeToggle.jsx` esté en el header de cada página en
 * la web, pero como overlay único montado en `MainActivity` en vez de
 * repetirlo en cada `TopAppBar`).
 */
@Composable
fun ThemeToggleButton(modifier: Modifier = Modifier) {
    val controller = LocalThemeController.current
    IconButton(
        onClick = controller.toggle,
        modifier = modifier.shadow(elevation = 4.dp, shape = CircleShape),
        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Icon(
            imageVector = if (controller.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = if (controller.isDark) "Cambiar a modo claro" else "Cambiar a modo oscuro",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
