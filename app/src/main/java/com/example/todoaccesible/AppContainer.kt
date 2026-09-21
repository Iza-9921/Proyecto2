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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val questionReviewRepositoryImpl =
        QuestionReviewRepositoryImpl(networkModule.diagnosticoApi, sessionManager, toastController)
    val questionReviewRepository: QuestionReviewRepository = questionReviewRepositoryImpl

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
        questionReviewRepositoryImpl.resolveDiagnosticId = diagnosticRepositoryImpl::resolveId

        // Arranca/para heartbeat + socket según haya o no sesión activa (incluida la
        // sesión persistida al reabrir la app), foreground-only. Se combina con `tokens`
        // (no solo `session`) para que el socket se reconecte con el access token vigente
        // cada vez que el heartbeat/TokenAuthenticator lo rota: el cliente de socket.io no
        // reevalúa el token de un socket ya creado, así que sin esto el socket seguía usando
        // el token original y, tras la primera reconexión pasados ~15 min, el servidor podía
        // rechazarlo silenciosamente (notificaciones/eventos en vivo dejaban de llegar).
        combine(sessionManager.session, sessionManager.tokens) { session, tokens ->
            if (session != null && tokens != null) Pair(session.userId, tokens.accessToken) else null
        }
            .distinctUntilChanged()
            .onEach { active ->
                if (active != null) {
                    val (userId, accessToken) = active
                    heartbeatManager.start(appScope)
                    socketManager.connect(userId, accessToken)
                } else {
                    heartbeatManager.stop()
                    socketManager.disconnect()
                    // Sin esto, si un admin cierra sesión y otro usuario entra después en el mismo
                    // proceso, la lista de usuarios (`GET /admin/usuarios`) del admin anterior se
                    // quedaba cacheada: el refresh para el nuevo usuario falla en silencio (403 si
                    // no es admin) y conserva "la última conocida" en vez de vaciarse.
                    userRepository.clearCache()
                    // Mismo problema con los alias de diagnóstico local->real (ver clearCache en
                    // DiagnosticRepository): sin esto sobreviven al cambio de cuenta y pueden
                    // reutilizarse para el diagnóstico de otra persona.
                    diagnosticRepositoryImpl.clearCache()
                }
            }
            .launchIn(appScope)
    }
}
