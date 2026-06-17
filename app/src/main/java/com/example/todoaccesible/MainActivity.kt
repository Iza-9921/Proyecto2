package com.example.todoaccesible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.room.Room
import com.example.todoaccesible.data.local.AppDatabase
import com.example.todoaccesible.data.model.UserRole
import com.example.todoaccesible.data.preferences.TokenManager
import com.example.todoaccesible.ui.dashboard.DashboardScreen
import com.example.todoaccesible.ui.dashboard.DashboardViewModel
import com.example.todoaccesible.ui.evaluation.*
import com.example.todoaccesible.ui.forgotpassword.ForgotPasswordScreen
import com.example.todoaccesible.ui.forgotpassword.ForgotPasswordViewModel
import com.example.todoaccesible.ui.login.LoginScreen
import com.example.todoaccesible.ui.login.LoginViewModel
import com.example.todoaccesible.ui.newproject.NewProjectScreen
import com.example.todoaccesible.ui.newproject.NewProjectViewModel
import com.example.todoaccesible.ui.quotation.QuotationScreen
import com.example.todoaccesible.ui.quotation.QuotationViewModel
import com.example.todoaccesible.ui.register.RegisterScreen
import com.example.todoaccesible.ui.register.RegisterViewModel
import com.example.todoaccesible.ui.theme.TodoAccesibleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicialización de la base de datos
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "todo-accesible-db"
        ).fallbackToDestructiveMigration().build()

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
                        // --- PANTALLA DE LOGIN ---
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
                                onLoginSuccess = { role ->
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- PANTALLA DE REGISTRO ---
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

                        // --- RECUPERAR CONTRASEÑA ---
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

                        // --- DASHBOARD ---
                        composable("dashboard") {
                            val dashboardViewModel: DashboardViewModel = viewModel(
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        @Suppress("UNCHECKED_CAST")
                                        return DashboardViewModel(db.projectDao()) as T
                                    }
                                }
                            )
                            
                            val role by tokenManager.getRole.collectAsState(initial = UserRole.CLIENTE)
                            LaunchedEffect(role) {
                                dashboardViewModel.setRole(role)
                            }

                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToNewProject = { 
                                    if (role == UserRole.CLIENTE) navController.navigate("new_project") 
                                },
                                onNavigateToProjectDetail = { projectId ->
                                    if (role == UserRole.CLIENTE) navController.navigate("evaluation/$projectId")
                                }
                            )
                        }

                        // --- NUEVO PROYECTO ---
                        composable("new_project") {
                            val dashboardEntry = remember(it) { navController.getBackStackEntry("dashboard") }
                            val dashboardViewModel: DashboardViewModel = viewModel(dashboardEntry)
                            val newProjectViewModel: NewProjectViewModel = viewModel()

                            NewProjectScreen(
                                viewModel = newProjectViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onProjectCreated = { name, address, responsible, desc ->
                                    dashboardViewModel.addProject(name, address, responsible, desc)
                                    navController.popBackStack()
                                }
                            )
                        }

                        // --- EVALUACIÓN (ENCUESTA) ---
                        composable(
                            route = "evaluation/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                            val evaluationViewModel: EvaluationViewModel = viewModel(
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        @Suppress("UNCHECKED_CAST")
                                        return EvaluationViewModel(db.answerDao()) as T
                                    }
                                }
                            )
                            EvaluationScreen(
                                projectId = projectId,
                                viewModel = evaluationViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onTakePhoto = { questionIndex ->
                                    navController.navigate("camera_capture/$questionIndex")
                                },
                                onEvaluationFinished = { id ->
                                    // Ahora navegamos a la pantalla de resultados
                                    navController.navigate("evaluation_results/$id")
                                }
                            )
                        }

                        // --- RESULTADOS DE ACCESIBILIDAD ---
                        composable(
                            route = "evaluation_results/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                            // Recuperamos el ViewModel de la evaluación (compartido)
                            val evaluationEntry = remember(backStackEntry) {
                                navController.getBackStackEntry("evaluation/{projectId}")
                            }
                            val evaluationViewModel: EvaluationViewModel = viewModel(evaluationEntry)
                            
                            EvaluationResultsScreen(
                                result = evaluationViewModel.calculateResult(),
                                onContinue = {
                                    navController.navigate("diagnosis_confirmation/$projectId")
                                }
                            )
                        }

                        // --- CAPTURA DE CÁMARA ---
                        composable(
                            route = "camera_capture/{questionIndex}",
                            arguments = listOf(navArgument("questionIndex") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val questionIndex = backStackEntry.arguments?.getInt("questionIndex") ?: 0
                            val evaluationEntry = remember(backStackEntry) {
                                try {
                                    navController.getBackStackEntry("evaluation/{projectId}")
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            if (evaluationEntry != null) {
                                val evaluationViewModel: EvaluationViewModel = viewModel(
                                    viewModelStoreOwner = evaluationEntry,
                                    factory = object : ViewModelProvider.Factory {
                                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                            @Suppress("UNCHECKED_CAST")
                                            return EvaluationViewModel(db.answerDao()) as T
                                        }
                                    }
                                )
                                CameraCaptureScreen(
                                    onImageCaptured = { uri ->
                                        evaluationViewModel.onPhotoCaptured(questionIndex, uri)
                                        navController.popBackStack()
                                    },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
                        }

                        // --- CONFIRMACIÓN FINAL ---
                        composable(
                            route = "diagnosis_confirmation/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            DiagnosisConfirmationScreen(
                                projectName = "Instalación",
                                onNavigateToQuotation = { navController.navigate("quotation") },
                                onFinish = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // --- SOLICITUD DE COTIZACIÓN ---
                        composable("quotation") {
                            val quotationViewModel: QuotationViewModel = viewModel(
                                factory = object : ViewModelProvider.Factory {
                                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                        @Suppress("UNCHECKED_CAST")
                                        return QuotationViewModel(db.quotationDao()) as T
                                    }
                                }
                            )
                            QuotationScreen(
                                viewModel = quotationViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onQuotationSent = {
                                    navController.navigate("dashboard") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
