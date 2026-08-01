package com.example.todoaccesible.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

private data class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val clienteItems = listOf(
    BottomItem(Routes.ClienteDashboard.route, "Diagnósticos", Icons.Filled.Dashboard),
    BottomItem(Routes.Notifications.route, "Notificaciones", Icons.Filled.Notifications)
)

private val adminItems = listOf(
    BottomItem(Routes.AdminDashboard.route, "Panel", Icons.Filled.Dashboard),
    BottomItem(Routes.AdminPending.route, "Diagnósticos", Icons.Filled.Assignment),
    BottomItem(Routes.AdminUsers.route, "Usuarios", Icons.Filled.Group),
    BottomItem(Routes.AdminQuestions.route, "Preguntas", Icons.Filled.Help),
    BottomItem(Routes.AdminCompare.route, "Comparar", Icons.Filled.Compare),
    BottomItem(Routes.Notifications.route, "Notificaciones", Icons.Filled.Notifications)
)

@Composable
private fun RoleBottomBar(navController: NavController, currentRoute: String?, items: List<BottomItem>) {
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
fun ClienteShell(navController: NavController, currentRoute: String?, content: @Composable () -> Unit) {
    Scaffold(bottomBar = { RoleBottomBar(navController, currentRoute, clienteItems) }) { padding ->
        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun AdminShell(navController: NavController, currentRoute: String?, content: @Composable () -> Unit) {
    Scaffold(bottomBar = { RoleBottomBar(navController, currentRoute, adminItems) }) { padding ->
        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.padding(padding)) {
            content()
        }
    }
}
