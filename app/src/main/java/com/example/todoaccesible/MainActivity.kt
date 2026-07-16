package com.example.todoaccesible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.todoaccesible.core.theme.TodoAccesibleTheme
import com.example.todoaccesible.data.model.Role
import com.example.todoaccesible.navigation.Routes
import com.example.todoaccesible.navigation.TodoAccesibleNavGraph
import kotlinx.coroutines.flow.map

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
            TodoAccesibleTheme {
                val state by sessionState.collectAsState(initial = SessionState.Loading)
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
            }
        }
    }
}
