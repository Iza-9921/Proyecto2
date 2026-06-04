package com.example.todoaccesible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.todoaccesible.data.preferences.TokenManager
import com.example.todoaccesible.ui.dashboard.DashboardScreen
import com.example.todoaccesible.ui.dashboard.DashboardViewModel
import com.example.todoaccesible.ui.evaluation.CameraCaptureScreen
import com.example.todoaccesible.ui.evaluation.EvaluationScreen
import com.example.todoaccesible.ui.evaluation.EvaluationViewModel
import com.example.todoaccesible.ui.forgotpassword.ForgotPasswordScreen
import com.example.todoaccesible.ui.forgotpassword.ForgotPasswordViewModel
import com.example.todoaccesible.ui.login.LoginScreen
import com.example.todoaccesible.ui.login.LoginViewModel
import com.example.todoaccesible.ui.newproject.NewProjectScreen
import com.example.todoaccesible.ui.newproject.NewProjectViewModel
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
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable("register") {
                            val registerViewModel: RegisterViewModel = viewModel()
                            RegisterScreen(
                                viewModel = registerViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onRegisterSuccess = {
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
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("dashboard") {
                            val dashboardViewModel: DashboardViewModel = viewModel()
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToNewProject = { navController.navigate("new_project") }
                            )
                        }

                        composable("new_project") {
                            val newProjectViewModel: NewProjectViewModel = viewModel()
                            NewProjectScreen(
                                viewModel = newProjectViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onStartEvaluation = {
                                    navController.navigate("evaluation")
                                }
                            )
                        }

                        composable("evaluation") {
                            val evaluationViewModel: EvaluationViewModel = viewModel()
                            EvaluationScreen(
                                viewModel = evaluationViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onTakePhoto = { questionIndex ->
                                    navController.navigate("camera_capture/$questionIndex")
                                }
                            )
                        }

                        composable(
                            route = "camera_capture/{questionIndex}",
                            arguments = listOf(navArgument("questionIndex") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val questionIndex = backStackEntry.arguments?.getInt("questionIndex") ?: 0
                            val evaluationEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("evaluation")
                            }
                            val evaluationViewModel: EvaluationViewModel = viewModel(evaluationEntry)
                            CameraCaptureScreen(
                                onImageCaptured = { uri ->
                                    evaluationViewModel.onPhotoCaptured(questionIndex, uri)
                                    navController.popBackStack()
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
