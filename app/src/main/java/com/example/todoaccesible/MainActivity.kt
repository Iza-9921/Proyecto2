package com.example.todoaccesible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.todoaccesible.core.designsystem.ThemeToggleButton
import com.example.todoaccesible.core.designsystem.ToastTipo
import com.example.todoaccesible.core.designsystem.VoiceHelpButton
import com.example.todoaccesible.core.theme.LocalThemeController
import com.example.todoaccesible.core.theme.ThemeController
import com.example.todoaccesible.core.theme.TodoAccesibleTheme
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.data.preferences.ThemePreference
import com.example.todoaccesible.navigation.Routes
import com.example.todoaccesible.navigation.TodoAccesibleNavGraph
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private sealed class SessionState {
    object Loading : SessionState()
    object LoggedOut : SessionState()
    data class LoggedIn(val rol: Role) : SessionState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as TodoAccesibleApp).container
        val sessionState = container.sessionManager.session.map { session ->
            if (session == null) SessionState.LoggedOut else SessionState.LoggedIn(session.rol)
        }

        setContent {
            val themePreference by container.themePreferenceStore.preference.collectAsState(initial = ThemePreference.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> systemDark
            }
            val scope = rememberCoroutineScope()
            val themeController = ThemeController(isDark = isDark) {
                scope.launch {
                    container.themePreferenceStore.set(if (isDark) ThemePreference.LIGHT else ThemePreference.DARK)
                }
            }

            CompositionLocalProvider(LocalThemeController provides themeController) {
                TodoAccesibleTheme(darkTheme = isDark) {
                    val state by sessionState.collectAsState(initial = SessionState.Loading)
                    val navController = rememberNavController()

                    val snackbarHostState = remember { SnackbarHostState() }
                    var toastTipo by remember { androidx.compose.runtime.mutableStateOf(ToastTipo.INFO) }
                    LaunchedEffect(Unit) {
                        container.toastController.messages.collect { toast ->
                            toastTipo = toast.tipo
                            snackbarHostState.showSnackbar(toast.texto)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            snackbarHost = {
                                SnackbarHost(snackbarHostState) { data ->
                                    val containerColor = when (toastTipo) {
                                        ToastTipo.EXITO -> Color(0xFF2E7D32)
                                        ToastTipo.ERROR -> Color(0xFFC62828)
                                        ToastTipo.ALERTA -> Color(0xFFB8860B)
                                        ToastTipo.INFO -> MaterialTheme.colorScheme.inverseSurface
                                    }
                                    Snackbar(containerColor = containerColor, contentColor = Color.White) {
                                        Text(data.visuals.message)
                                    }
                                }
                            }
                        ) { innerPadding ->
                            when (val current = state) {
                                is SessionState.Loading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                                else -> {
                                    val startDestination = when (current) {
                                        is SessionState.LoggedIn -> if (current.rol == Role.ADMIN) {
                                            Routes.AdminDashboard.route
                                        } else {
                                            Routes.ClienteDashboard.route
                                        }
                                        else -> Routes.Login.route
                                    }
                                    TodoAccesibleNavGraph(
                                        navController = navController,
                                        container = container,
                                        startDestination = startDestination,
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                            }
                        }
                        ThemeToggleButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 48.dp, end = 12.dp)
                        )
                        VoiceHelpButton(
                            controller = container.voiceGuideController,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 88.dp, end = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
