package com.example.todoaccesible.navigation

import androidx.compose.runtime.Composable
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
import com.example.todoaccesible.ui.cliente.notifications.NotificationsScreen
import com.example.todoaccesible.ui.cliente.notifications.NotificationsViewModel
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
                factory = viewModelFactory { initializer { RegisterViewModel(container.authRepository) } }
            )
            RegisterScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Routes.ClienteDashboard.route) { popUpTo(0) }
                }
            )
        }

        composable(Routes.ClienteDashboard.route) {
            val clienteId = session?.userId ?: return@composable
            val viewModel: DashboardViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { DashboardViewModel(container.diagnosticRepository, container.notificationRepository, clienteId) }
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
                factory = viewModelFactory { initializer { ProjectInfoViewModel(container.diagnosticRepository, clienteId) } }
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
                onSubmitted = { id ->
                    navController.navigate(Routes.DiagnosticDetail.build(id)) {
                        popUpTo(Routes.ClienteDashboard.route)
                    }
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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Notifications.route) {
            val userId = session?.userId ?: return@composable
            val viewModel: NotificationsViewModel = viewModel(
                factory = viewModelFactory { initializer { NotificationsViewModel(container.notificationRepository, userId) } }
            )
            NotificationsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.AdminDashboard.route) {
            val viewModel: AdminDashboardViewModel = viewModel(
                factory = viewModelFactory { initializer { AdminDashboardViewModel(container.diagnosticRepository) } }
            )
            AdminShell(navController, currentRoute) {
                AdminDashboardScreen(viewModel = viewModel, onLogout = ::logout)
            }
        }

        composable(Routes.AdminUsers.route) {
            val viewModel: UserManagementViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { UserManagementViewModel(container.userRepository, container.activeSessionRegistry) }
                }
            )
            AdminShell(navController, currentRoute) {
                UserManagementScreen(viewModel = viewModel)
            }
        }

        composable(Routes.AdminQuestions.route) {
            val viewModel: QuestionCatalogViewModel = viewModel(
                factory = viewModelFactory { initializer { QuestionCatalogViewModel(container.questionCatalogRepository) } }
            )
            AdminShell(navController, currentRoute) {
                QuestionCatalogScreen(viewModel = viewModel)
            }
        }

        composable(Routes.AdminPending.route) {
            val viewModel: AdminPendingViewModel = viewModel(
                factory = viewModelFactory { initializer { AdminPendingViewModel(container.diagnosticRepository, container.userRepository) } }
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
                            container.userRepository
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
