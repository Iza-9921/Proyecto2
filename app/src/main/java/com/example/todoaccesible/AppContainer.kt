package com.example.todoaccesible

import android.content.Context
import androidx.room.Room
import com.example.todoaccesible.data.local.AppDatabase
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.NotificationRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.data.repository.UserRepository
import com.example.todoaccesible.data.repository.impl.AuthRepositoryImpl
import com.example.todoaccesible.data.repository.impl.DiagnosticRepositoryImpl
import com.example.todoaccesible.data.repository.impl.NotificationRepositoryImpl
import com.example.todoaccesible.data.repository.impl.QuestionCatalogRepositoryImpl
import com.example.todoaccesible.data.repository.impl.UserRepositoryImpl

/**
 * Contenedor de dependencias manual (sin Hilt/Koin) — la app es lo bastante
 * chica para que un grafo explícito sea más fácil de seguir. Todos los
 * repositorios son interfaces con una única implementación Room-backed; el
 * día que exista backend, se agrega una fuente remota y se cambia solo el
 * constructor de cada `impl`, sin tocar ViewModels ni pantallas.
 */
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        AppDatabase.NAME
    ).fallbackToDestructiveMigration().build()

    val sessionManager = SessionManager(context.applicationContext)

    val questionCatalogRepository: QuestionCatalogRepository =
        QuestionCatalogRepositoryImpl(database.questionCatalogDao())

    val notificationRepository: NotificationRepository =
        NotificationRepositoryImpl(database.notificationDao())

    val userRepository: UserRepository = UserRepositoryImpl(database.userDao())

    val authRepository: AuthRepository = AuthRepositoryImpl(database.userDao(), sessionManager)

    val diagnosticRepository: DiagnosticRepository = DiagnosticRepositoryImpl(
        diagnosticDao = database.diagnosticDao(),
        answerDao = database.answerDao(),
        photoDao = database.photoDao(),
        questionCatalogRepository = questionCatalogRepository,
        notificationRepository = notificationRepository
    )
}
