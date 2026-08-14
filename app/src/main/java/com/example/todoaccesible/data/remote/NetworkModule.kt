package com.example.todoaccesible.data.remote

import com.example.todoaccesible.BuildConfig
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.dto.AuthResponseDto
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Construye los clientes Retrofit/OkHttp de la app. Hay dos Retrofit
 * distintos a propósito:
 *  - [plainRetrofit]: sin [AuthInterceptor] ni [TokenAuthenticator], usado
 *    solo para login/register/refresh (evita que [TokenAuthenticator] se
 *    recurse a sí mismo al refrescar).
 *  - [retrofit]: con ambos, usado por el resto de la API (incluido el
 *    heartbeat, que si encuentra un token ya vencido puede disparar un
 *    refresh igual que cualquier otra llamada).
 */
class NetworkModule(
    private val sessionManager: SessionManager
) {
    /**
     * `var` (no parámetro de constructor) para romper el ciclo con
     * `AuthRepositoryImpl`, que a su vez necesita `authApiPlain` de este
     * mismo módulo para construirse. `AppContainer` lo asigna después de
     * crear ambos; [TokenAuthenticator] lee esta propiedad en cada refresh
     * silencioso (no la captura una sola vez).
     */
    var onAuthRefreshed: (AuthResponseDto) -> Unit = {}

    /**
     * MySQL guarda booleanos como `TINYINT(1)`; varios endpoints devuelven la
     * fila cruda tal cual (`SELECT *`, p.ej. `proyectos.activo`,
     * `diagnosticos.calificacion_notificada`, `evidencias.is_sensitive`) sin
     * convertirla a `true`/`false` en JSON, así que llega como `0`/`1`. El
     * `Boolean` adapter estricto de Gson truena con eso
     * (`IllegalStateException: Expected a boolean but was NUMBER`), así que
     * se registra un deserializador tolerante en vez de tocar el backend.
     */
    private val gson: Gson by lazy {
        val lenientBoolean = JsonDeserializer { json, _, _ ->
            val primitive = json.asJsonPrimitive
            when {
                primitive.isBoolean -> primitive.asBoolean
                primitive.isNumber -> primitive.asInt != 0
                else -> primitive.asString == "true" || primitive.asString == "1"
            }
        }
        GsonBuilder()
            .registerTypeAdapter(Boolean::class.javaPrimitiveType, lenientBoolean)
            .registerTypeAdapter(Boolean::class.javaObjectType, lenientBoolean)
            .create()
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
    }

    private val plainClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .apply { if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor) }
            .build()
    }

    private val plainRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(plainClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /** Solo para login/register/refresh; también lo usa [TokenAuthenticator] internamente. */
    val authApiPlain: AuthApiService by lazy { plainRetrofit.create(AuthApiService::class.java) }

    private val authenticatedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(TokenAuthenticator(sessionManager, authApiPlain) { onAuthRefreshed(it) })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            // Las fotos van en base64 dentro del body: pueden tardar más en subir.
            .writeTimeout(30, TimeUnit.SECONDS)
            .apply { if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor) }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(authenticatedClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val authApi: AuthApiService by lazy { retrofit.create(AuthApiService::class.java) }
    val proyectoApi: ProyectoApiService by lazy { retrofit.create(ProyectoApiService::class.java) }
    val diagnosticoApi: DiagnosticoApiService by lazy { retrofit.create(DiagnosticoApiService::class.java) }
    val categoriaApi: CategoriaApiService by lazy { retrofit.create(CategoriaApiService::class.java) }
    val tipoInmuebleApi: TipoInmuebleApiService by lazy { retrofit.create(TipoInmuebleApiService::class.java) }
    val notificacionApi: NotificacionApiService by lazy { retrofit.create(NotificacionApiService::class.java) }
    val adminApi: AdminApiService by lazy { retrofit.create(AdminApiService::class.java) }
    val evidenciaApi: EvidenciaApiService by lazy { retrofit.create(EvidenciaApiService::class.java) }
}
