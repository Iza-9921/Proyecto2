package com.example.todoaccesible

import android.content.Context
import com.example.todoaccesible.data.local.entities.UserEntity
import com.example.todoaccesible.data.local.memory.InMemoryTable
import com.example.todoaccesible.data.preferences.ActiveSessionRegistry
import com.example.todoaccesible.data.preferences.DiagnosticQuotaStore
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.DiagnosticHistoryRepository
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.NotificationRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.data.repository.QuestionReviewRepository
import com.example.todoaccesible.data.repository.UserRepository
import com.example.todoaccesible.data.repository.impl.AuthRepositoryImpl
import com.example.todoaccesible.data.repository.impl.DiagnosticHistoryRepositoryImpl
import com.example.todoaccesible.data.repository.impl.DiagnosticRepositoryImpl
import com.example.todoaccesible.data.repository.impl.NotificationRepositoryImpl
import com.example.todoaccesible.data.repository.impl.QuestionCatalogRepositoryImpl
import com.example.todoaccesible.data.repository.impl.QuestionReviewRepositoryImpl
import com.example.todoaccesible.data.repository.impl.UserRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Contenedor de dependencias manual (sin Hilt/Koin) — la app es lo bastante
 * chica para que un grafo explícito sea más fácil de seguir. No hay base de
 * datos ni backend: cada repositorio guarda su información en memoria
 * (`InMemoryTable`), sembrada con valores por defecto (catálogo de preguntas,
 * cuenta admin) al construirse. Todo se reinicia al cerrar la app.
 */
class AppContainer(context: Context) {
    val sessionManager = SessionManager(context.applicationContext)
    val activeSessionRegistry = ActiveSessionRegistry()

    private val usersTable = InMemoryTable<UserEntity>(UserRepositoryImpl.defaultUsers())
    private val diagnosticQuotaStore = DiagnosticQuotaStore(context.applicationContext)

    val questionCatalogRepository: QuestionCatalogRepository = QuestionCatalogRepositoryImpl()

    val notificationRepository: NotificationRepository = NotificationRepositoryImpl()

    val userRepository: UserRepository = UserRepositoryImpl(usersTable, diagnosticQuotaStore)

    val authRepository: AuthRepository = AuthRepositoryImpl(usersTable, sessionManager, activeSessionRegistry)

    val diagnosticHistoryRepository: DiagnosticHistoryRepository = DiagnosticHistoryRepositoryImpl()

    val questionReviewRepository: QuestionReviewRepository = QuestionReviewRepositoryImpl(
        InMemoryTable(DiagnosticRepositoryImpl.demoQuestionReviews)
    )

    val diagnosticRepository: DiagnosticRepository = DiagnosticRepositoryImpl(
        questionCatalogRepository = questionCatalogRepository,
        notificationRepository = notificationRepository,
        diagnosticHistoryRepository = diagnosticHistoryRepository,
        userRepository = userRepository,
        questionReviewRepository = questionReviewRepository
    )

    init {
        // Cupo inicial de la cuenta cliente demo; no pisa una asignación ya guardada del admin.
        CoroutineScope(Dispatchers.IO).launch {
            diagnosticQuotaStore.seedIfAbsent(UserRepositoryImpl.DEMO_CLIENT_ID, 1)
        }
    }
}
