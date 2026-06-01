package com.example.todoaccesible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.todoaccesible.data.preferences.TokenManager
import com.example.todoaccesible.ui.forgotpassword.ForgotPasswordScreen
import com.example.todoaccesible.ui.forgotpassword.ForgotPasswordViewModel
import com.example.todoaccesible.ui.login.LoginScreen
import com.example.todoaccesible.ui.login.LoginViewModel
import com.example.todoaccesible.ui.register.RegisterScreen
import com.example.todoaccesible.ui.register.RegisterViewModel
import com.example.todoaccesible.ui.theme.TodoAccesibleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val tokenManager = TokenManager(applicationContext)

        setContent {
            TodoAccesibleTheme {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            val loginViewModel: LoginViewModel = viewModel(
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        @Suppress("UNCHECKED_CAST")
                                        return LoginViewModel(tokenManager) as T
                                    }
                                }
                            )
                            LoginScreen(
                                viewModel = loginViewModel,
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                                onLoginSuccess = { 
                                    // Aquí navegarías a la pantalla principal (Home)
                                }
                            )
                        }
                        
                        composable("register") {
                            val registerViewModel: RegisterViewModel = viewModel()
                            RegisterScreen(
                                viewModel = registerViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onRegisterSuccess = {
                                    // Al registrarse, volvemos al login para que inicie sesión
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("forgot_password") {
                            val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()
                            ForgotPasswordScreen(
                                viewModel = forgotPasswordViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onResetSuccess = {
                                    // Al recuperar contraseña con éxito, volvemos al login
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
