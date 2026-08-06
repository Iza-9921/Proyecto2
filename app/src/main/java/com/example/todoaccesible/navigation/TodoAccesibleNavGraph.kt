package com.example.todoaccesible.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.todoaccesible.AppContainer
import com.example.todoaccesible.ui.admin.compare.CompareDiagnosticsScreen
import com.example.todoaccesible.ui.admin.compare.CompareDiagnosticsViewModel
import com.example.todoaccesible.ui.admin.dashboard.AdminDashboardScreen
import com.example.todoaccesible.ui.admin.dashboard.AdminDashboardViewModel
import com.example.todoaccesible.ui.admin.pending.AdminPendingScreen
import com.example.todoaccesible.ui.admin.pending.AdminPendingViewModel
import com.example.todoaccesible.ui.admin.questions.QuestionCatalogScreen
import com.example.todoaccesible.ui.admin.questions.QuestionCatalogViewModel
import com.example.todoaccesible.ui.admin.review.AdminReviewScreen
import com.example.todoaccesible.ui.admin.review.AdminReviewViewModel
import com.example.todoaccesible.ui.admin.users.UserManagementScreen
import com.example.todoaccesible.ui.admin.users.UserManagementViewModel
import com.example.todoaccesible.ui.auth.login.LoginScreen
import com.example.todoaccesible.ui.auth.login.LoginViewModel
import com.example.todoaccesible.ui.auth.register.RegisterScreen
import com.example.todoaccesible.ui.auth.register.RegisterViewModel
import com.example.todoaccesible.ui.cliente.dashboard.DashboardScreen
import com.example.todoaccesible.ui.cliente.dashboard.DashboardViewModel
import com.example.todoaccesible.ui.cliente.diagnostic.detail.DiagnosticDetailScreen
import com.example.todoaccesible.ui.cliente.diagnostic.detail.DiagnosticDetailViewModel
import com.example.todoaccesible.ui.cliente.diagnostic.new.ProjectInfoScreen
import com.example.todoaccesible.ui.cliente.diagnostic.new.ProjectInfoViewModel
import com.example.todoaccesible.ui.cliente.diagnostic.new.QuestionnaireScreen
import com.example.todoaccesible.ui.cliente.diagnostic.new.QuestionnaireViewModel
import com.example.todoaccesible.ui.cliente.diagnostic.result.DiagnosticResultScreen
import com.example.todoaccesible.ui.cliente.diagnostic.result.DiagnosticResultViewModel
import com.example.todoaccesible.ui.cliente.diagnostic.responder.ResponderInfoAdicionalScreen
import com.example.todoaccesible.ui.cliente.diagnostic.responder.ResponderInfoAdicionalViewModel
import com.example.todoaccesible.ui.cliente.notifications.NotificationsScreen
import com.example.todoaccesible.ui.cliente.notifications.NotificationsViewModel
import com.example.todoaccesible.core.voice.VoiceInstructions
import kotlinx.coroutines.launch

@Composable
fun TodoAccesibleNavGraph(
    navController: NavHostController,
    container: AppContainer,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val session by container.sessionManager.session.collectAsState(initial = null)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Al cambiar de ruta: corta cualquier lectura en curso (para no arrastrar el
    // audio de la pantalla anterior) y carga el texto por defecto de la nueva
    // pantalla. Las pantallas con pasos internos lo sobreescriben después vía
    // `container.voiceGuideController.setInstructions(...)` según su estado.
    LaunchedEffect(currentRoute) {
        container.voiceGuideController.stop()
        container.voiceGuideController.setInstructions(VoiceInstructions.forRoute(currentRoute))
    }

    fun logout() {
        scope.launch {
            container.authRepository.logout()
            navController.navigate(Routes.Login.route) {
                popUpTo(0)
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Routes.Login.route) {
            val viewModel: LoginViewModel = viewModel(
                factory = viewModelFactory { initializer { LoginViewModel(container.authRepository) } }
            )
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate(Routes.Register.route) },
                onLoginSuccess = { rol ->
                    val destination = if (rol == com.example.todoaccesible.data.model.Role.ADMIN) {
                        Routes.AdminDashboard.route
                    } else {
                        Routes.ClienteDashboard.route
                    }
                    navController.navigate(destination) { popUpTo(0) }
                }
            )
        }

        composable(Routes.Register.route) {
            val viewModel: RegisterViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { RegisterViewModel(container.authRepository, container.diagnosticRepository, container.userRepository) }
                }
            )
            RegisterScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { diagnosticId ->
                    navController.navigate(Routes.Questionnaire.build(diagnosticId)) { popUpTo(0) }
                },
                onQuotaBlockedAcknowledged = {
                    navController.navigate(Routes.ClienteDashboard.route) { popUpTo(0) }
                }
            )
        }

        composable(Routes.ClienteDashboard.route) {
            val clienteId = session?.userId ?: return@composable
            val viewModel: DashboardViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { DashboardViewModel(container.diagnosticRepository, container.notificationRepository, container.userRepository, clienteId) }
                }
            )
            ClienteShell(navController, currentRoute) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToNewDiagnostic = { navController.navigate(Routes.ProjectInfo.route) },
                    onNavigateToDetail = { id -> navController.navigate(Routes.DiagnosticDetail.build(id)) },
                    onNavigateToNotifications = { navController.navigate(Routes.Notifications.route) },
                    onLogout = ::logout
                )
            }
        }

        composable(Routes.ProjectInfo.route) {
            val clienteId = session?.userId ?: return@composable
            val viewModel: ProjectInfoViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ProjectInfoViewModel(
                            container.diagnosticRepository,
                            container.userRepository,
                            container.tipoCuestionarioRepository,
                            clienteId
                        )
                    }
                }
            )
            ProjectInfoScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onContinue = { id -> navController.navigate(Routes.Questionnaire.build(id)) }
            )
        }

        composable(
            route = Routes.Questionnaire.route,
            arguments = listOf(navArgument(Routes.ARG_DIAGNOSTIC_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val diagnosticId = backStackEntry.arguments?.getLong(Routes.ARG_DIAGNOSTIC_ID) ?: return@composable
            val viewModel: QuestionnaireViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        QuestionnaireViewModel(diagnosticId, container.diagnosticRepository, container.questionCatalogRepository)
                    }
                }
            )
            QuestionnaireScreen(
                viewModel = viewModel,
                diagnosticId = diagnosticId,
                onNavigateBack = {
                    navController.navigate(Routes.ClienteDashboard.route) { popUpTo(Routes.ClienteDashboard.route) { inclusive = true } }
                },
                onGoToSummary = { id ->
                    // Sin popUpTo: el cuestionario se queda en el backstack para
                    // que "Anterior" en el resumen pueda volver a él.
                    navController.navigate(Routes.DiagnosticResult.build(id))
                }
            )
        }

        composable(
            route = Routes.DiagnosticResult.route,
            arguments = listOf(navArgument(Routes.ARG_DIAGNOSTIC_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val diagnosticId = backStackEntry.arguments?.getLong(Routes.ARG_DIAGNOSTIC_ID) ?: return@composable
            val viewModel: DiagnosticResultViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        DiagnosticResultViewModel(diagnosticId, container.diagnosticRepository, container.questionCatalogRepository)
                    }
                }
            )
            DiagnosticResultScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSubmitted = {
                    navController.navigate(Routes.ClienteDashboard.route) { popUpTo(0) }
                },
                onDiscarded = {
                    navController.navigate(Routes.ClienteDashboard.route) { popUpTo(0) }
                }
            )
        }

        composable(
            route = Routes.DiagnosticDetail.route,
            arguments = listOf(navArgument(Routes.ARG_DIAGNOSTIC_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val diagnosticId = backStackEntry.arguments?.getLong(Routes.ARG_DIAGNOSTIC_ID) ?: return@composable
            val viewModel: DiagnosticDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        DiagnosticDetailViewModel(
                            diagnosticId,
                            container.diagnosticRepository,
                            container.diagnosticHistoryRepository,
                            container.userRepository
                        )
                    }
                }
            )
            DiagnosticDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onResponderInfoAdicional = { navController.navigate(Routes.ResponderInfoAdicional.build(diagnosticId)) }
            )
        }

        composable(
            route = Routes.ResponderInfoAdicional.route,
            arguments = listOf(navArgument(Routes.ARG_DIAGNOSTIC_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val diagnosticId = backStackEntry.arguments?.getLong(Routes.ARG_DIAGNOSTIC_ID) ?: return@composable
            val viewModel: ResponderInfoAdicionalViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ResponderInfoAdicionalViewModel(
                            diagnosticId,
                            container.diagnosticRepository,
                            container.questionCatalogRepository,
                            container.questionReviewRepository
                        )
                    }
                }
            )
            ResponderInfoAdicionalScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNotAllowed = { navController.popBackStack() },
                onReenviado = { navController.popBackStack() }
            )
        }

        composable(Routes.Notifications.route) {
            val userId = session?.userId ?: return@composable
            val viewModel: NotificationsViewModel = viewModel(
                factory = viewModelFactory { initializer { NotificationsViewModel(container.notificationRepository, userId) } }
            )
            NotificationsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenDiagnostic = { id ->
                    if (session?.rol == com.example.todoaccesible.data.model.Role.ADMIN) {
                        navController.navigate(Routes.AdminReview.build(id))
                    } else {
                        navController.navigate(Routes.DiagnosticDetail.build(id))
                    }
                }
            )
        }

        composable(Routes.AdminDashboard.route) {
            val viewModel: AdminDashboardViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AdminDashboardViewModel(container.diagnosticRepository, container.panelCollapseStore) }
                }
            )
            AdminShell(navController, currentRoute) {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onLogout = ::logout,
                    onOpenDiagnostic = { id -> navController.navigate(Routes.AdminReview.build(id)) }
                )
            }
        }

        composable(Routes.AdminUsers.route) {
            val viewModel: UserManagementViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        UserManagementViewModel(
                            container.userRepository,
                            container.activeSessionRegistry,
                            container.diagnosticRepository,
                            container.tipoCuestionarioRepository,
                            container.notificationRepository,
                            container.toastController
                        )
                    }
                }
            )
            AdminShell(navController, currentRoute) {
                UserManagementScreen(viewModel = viewModel)
            }
        }

        composable(Routes.AdminQuestions.route) {
            val viewModel: QuestionCatalogViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { QuestionCatalogViewModel(container.questionCatalogRepository, container.tipoCuestionarioRepository) }
                }
            )
            AdminShell(navController, currentRoute) {
                QuestionCatalogScreen(viewModel = viewModel)
            }
        }

        composable(Routes.AdminPending.route) {
            val reviewerId = session?.userId ?: return@composable
            val viewModel: AdminPendingViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        AdminPendingViewModel(container.diagnosticRepository, container.userRepository, reviewerId, container.toastController)
                    }
                }
            )
            AdminShell(navController, currentRoute) {
                AdminPendingScreen(
                    viewModel = viewModel,
                    onOpenReview = { id -> navController.navigate(Routes.AdminReview.build(id)) }
                )
            }
        }

        composable(
            route = Routes.AdminReview.route,
            arguments = listOf(navArgument(Routes.ARG_DIAGNOSTIC_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val diagnosticId = backStackEntry.arguments?.getLong(Routes.ARG_DIAGNOSTIC_ID) ?: return@composable
            val reviewerId = session?.userId ?: return@composable
            val viewModel: AdminReviewViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        AdminReviewViewModel(
                            diagnosticId,
                            reviewerId,
                            container.diagnosticRepository,
                            container.questionCatalogRepository,
                            container.diagnosticHistoryRepository,
                            container.userRepository,
                            container.questionReviewRepository,
                            container.presenceRepository
                        )
                    }
                }
            )
            AdminReviewScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.AdminCompare.route) {
            val viewModel: CompareDiagnosticsViewModel = viewModel(
                factory = viewModelFactory { initializer { CompareDiagnosticsViewModel(container.diagnosticRepository) } }
            )
            AdminShell(navController, currentRoute) {
                CompareDiagnosticsScreen(viewModel = viewModel)
            }
        }
    }
}
