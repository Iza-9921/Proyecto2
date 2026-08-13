package com.example.todoaccesible

import android.content.Context
import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.voice.VoiceGuideController
import com.example.todoaccesible.data.preferences.LocalDiagnosticMetadataStore
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.preferences.ThemePreferenceStore
import com.example.todoaccesible.data.remote.HeartbeatManager
import com.example.todoaccesible.data.remote.NetworkModule
import com.example.todoaccesible.data.remote.SocketManager
import com.example.todoaccesible.data.repository.AuthRepository
import com.example.todoaccesible.data.repository.DiagnosticHistoryRepository
import com.example.todoaccesible.data.repository.DiagnosticRepository
import com.example.todoaccesible.data.repository.NotificationRepository
import com.example.todoaccesible.data.repository.PresenceRepository
import com.example.todoaccesible.data.repository.QuestionCatalogRepository
import com.example.todoaccesible.data.repository.QuestionReviewRepository
import com.example.todoaccesible.data.repository.TipoCuestionarioRepository
import com.example.todoaccesible.data.repository.UserRepository
import com.example.todoaccesible.data.repository.impl.AuthRepositoryImpl
import com.example.todoaccesible.data.repository.impl.DiagnosticHistoryRepositoryImpl
import com.example.todoaccesible.data.repository.impl.DiagnosticRepositoryImpl
import com.example.todoaccesible.data.repository.impl.NotificationRepositoryImpl
import com.example.todoaccesible.data.repository.impl.PresenceRepositoryImpl
import com.example.todoaccesible.data.repository.impl.QuestionCatalogRepositoryImpl
import com.example.todoaccesible.data.repository.impl.QuestionReviewRepositoryImpl
import com.example.todoaccesible.data.repository.impl.TipoCuestionarioRepositoryImpl
import com.example.todoaccesible.data.repository.impl.UserRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Contenedor de dependencias manual (sin Hilt/Koin) — la app está conectada
 * al backend real: Retrofit/OkHttp ([networkModule]), Socket.IO
 * ([socketManager]) y un heartbeat periódico ([heartbeatManager]), todos
 * arrancados/parados según haya o no sesión activa (ver bloque `init`,
 * alcance foreground-only, sin WorkManager).
 *
 * [presenceRepository] es la única excepción: no tiene ningún equivalente en
 * el backend (ni socket ni REST) y se deja tal cual, local/no-op.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(Dispatchers.IO)

    val sessionManager = SessionManager(appContext)
    val themePreferenceStore = ThemePreferenceStore(appContext)
    val voiceGuideController = VoiceGuideController(appContext)
    val toastController = ToastController()
    val presenceRepository: PresenceRepository = PresenceRepositoryImpl()

    private val localMetaStore = LocalDiagnosticMetadataStore(appContext)

    val networkModule = NetworkModule(sessionManager)
    val heartbeatManager = HeartbeatManager(networkModule.authApi, sessionManager)

    private val authRepositoryImpl = AuthRepositoryImpl(networkModule.authApiPlain, sessionManager)
    val authRepository: AuthRepository = authRepositoryImpl

    val tipoCuestionarioRepository: TipoCuestionarioRepository =
        TipoCuestionarioRepositoryImpl(networkModule.tipoInmuebleApi, sessionManager, toastController)

    val questionCatalogRepository: QuestionCatalogRepository =
        QuestionCatalogRepositoryImpl(networkModule.categoriaApi, sessionManager, toastController)

    private val notificationRepositoryImpl =
        NotificationRepositoryImpl(networkModule.notificacionApi, sessionManager, toastController)
    val notificationRepository: NotificationRepository = notificationRepositoryImpl

    val userRepository: UserRepository =
        UserRepositoryImpl(networkModule.adminApi, sessionManager, authRepositoryImpl.cachedUserFlow, toastController)

    val diagnosticHistoryRepository: DiagnosticHistoryRepository =
        DiagnosticHistoryRepositoryImpl(networkModule.diagnosticoApi, sessionManager)

    val questionReviewRepository: QuestionReviewRepository =
        QuestionReviewRepositoryImpl(networkModule.diagnosticoApi, sessionManager, toastController)

    private val diagnosticRepositoryImpl = DiagnosticRepositoryImpl(
        proyectoApi = networkModule.proyectoApi,
        diagnosticoApi = networkModule.diagnosticoApi,
        evidenciaApi = networkModule.evidenciaApi,
        adminApi = networkModule.adminApi,
        questionCatalogRepository = questionCatalogRepository,
        questionReviewRepository = questionReviewRepository,
        userRepository = userRepository,
        sessionManager = sessionManager,
        localMetaStore = localMetaStore,
        toastController = toastController,
        appContext = appContext
    )
    val diagnosticRepository: DiagnosticRepository = diagnosticRepositoryImpl

    val socketManager = SocketManager(
        onNotificacion = { notificationRepositoryImpl.onSocketNotification(it) },
        onDiagnosticoEvent = { diagnosticRepositoryImpl.onSocketDiagnosticEvent(it) }
    )

    init {
        networkModule.onAuthRefreshed = { authRepositoryImpl.onAuthResponse(it) }

        // Arranca/para heartbeat + socket según haya o no sesión activa (incluida la
        // sesión persistida al reabrir la app), foreground-only.
        sessionManager.session.distinctUntilChanged()
            .onEach { session ->
                if (session != null) {
                    heartbeatManager.start(appScope)
                    val token = sessionManager.tokens.first()?.accessToken
                    if (token != null) socketManager.connect(session.userId, token)
                } else {
                    heartbeatManager.stop()
                    socketManager.disconnect()
                }
            }
            .launchIn(appScope)
    }
}
