package com.example.todoaccesible.data.remote

import com.example.todoaccesible.data.remote.dto.ActivoRequest
import com.example.todoaccesible.data.remote.dto.ActualizarEstadoRequest
import com.example.todoaccesible.data.remote.dto.AdminDiagnosticosResponseDto
import com.example.todoaccesible.data.remote.dto.AdminUsuarioDto
import com.example.todoaccesible.data.remote.dto.AprobarRequest
import com.example.todoaccesible.data.remote.dto.AuthResponseDto
import com.example.todoaccesible.data.remote.dto.CuestionarioAsignadoRequest
import com.example.todoaccesible.data.remote.dto.DiagnosticoAdminDetailDto
import com.example.todoaccesible.data.remote.dto.DiagnosticoClienteDetailDto
import com.example.todoaccesible.data.remote.dto.DiagnosticoPendienteDto
import com.example.todoaccesible.data.remote.dto.EstadoDiagnosticoDto
import com.example.todoaccesible.data.remote.dto.EvaluacionesRequest
import com.example.todoaccesible.data.remote.dto.FinalizarResponseDto
import com.example.todoaccesible.data.remote.dto.GuardarRespuestasRequest
import com.example.todoaccesible.data.remote.dto.GuardarRespuestasResponseDto
import com.example.todoaccesible.data.remote.dto.HeartbeatResponseDto
import com.example.todoaccesible.data.remote.dto.IniciarDiagnosticoRequest
import com.example.todoaccesible.data.remote.dto.IniciarDiagnosticoResponseDto
import com.example.todoaccesible.data.remote.dto.LimiteCuestionariosResponseDto
import com.example.todoaccesible.data.remote.dto.ListarDiagnosticosResponseDto
import com.example.todoaccesible.data.remote.dto.LoginRequest
import com.example.todoaccesible.data.remote.dto.MensajeEstadoResponseDto
import com.example.todoaccesible.data.remote.dto.NotificacionesResponseDto
import com.example.todoaccesible.data.remote.dto.NuevaContrasenaRequest
import com.example.todoaccesible.data.remote.dto.NuevaContrasenaResponseDto
import com.example.todoaccesible.data.remote.dto.OkDto
import com.example.todoaccesible.data.remote.dto.OrdenRequest
import com.example.todoaccesible.data.remote.dto.PreguntaDto
import com.example.todoaccesible.data.remote.dto.PreguntaRequest
import com.example.todoaccesible.data.remote.dto.ProyectoDto
import com.example.todoaccesible.data.remote.dto.ProyectoRequest
import com.example.todoaccesible.data.remote.dto.RecuperarRequest
import com.example.todoaccesible.data.remote.dto.RecuperarResponseDto
import com.example.todoaccesible.data.remote.dto.RechazarRequest
import com.example.todoaccesible.data.remote.dto.RefreshRequest
import com.example.todoaccesible.data.remote.dto.RegisterRequest
import com.example.todoaccesible.data.remote.dto.SeccionDto
import com.example.todoaccesible.data.remote.dto.SeccionRequest
import com.example.todoaccesible.data.remote.dto.SolicitarInfoRequest
import com.example.todoaccesible.data.remote.dto.TipoInmuebleRequest
import com.example.todoaccesible.data.remote.dto.VerificarCodigoRequest
import com.example.todoaccesible.data.remote.dto.VerificarCodigoResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface AuthApiService {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponseDto

    @POST("auth/heartbeat")
    suspend fun heartbeat(): HeartbeatResponseDto

    @POST("auth/recuperar")
    suspend fun recuperar(@Body request: RecuperarRequest): RecuperarResponseDto

    @POST("auth/verificar-codigo")
    suspend fun verificarCodigo(@Body request: VerificarCodigoRequest): VerificarCodigoResponseDto

    @POST("auth/nueva-contrasena")
    suspend fun nuevaContrasena(@Body request: NuevaContrasenaRequest): NuevaContrasenaResponseDto
}

interface ProyectoApiService {
    @POST("proyectos")
    suspend fun crear(@Body request: ProyectoRequest): ProyectoDto

    @GET("proyectos")
    suspend fun listar(): List<ProyectoDto>

    @GET("proyectos/{id}")
    suspend fun obtener(@Path("id") id: Long): ProyectoDto

    @PUT("proyectos/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body request: ProyectoRequest): ProyectoDto

    @DELETE("proyectos/{id}")
    suspend fun eliminar(@Path("id") id: Long): OkDto
}

interface DiagnosticoApiService {
    @POST("diagnosticos/iniciar")
    suspend fun iniciar(@Body request: IniciarDiagnosticoRequest): IniciarDiagnosticoResponseDto

    @GET("diagnosticos")
    suspend fun listar(@Query("page") page: Int = 1, @Query("limit") limit: Int = 50): ListarDiagnosticosResponseDto

    @GET("diagnosticos/{id}")
    suspend fun obtenerParaCliente(@Path("id") id: Long): DiagnosticoClienteDetailDto

    @GET("diagnosticos/{id}")
    suspend fun obtenerParaAdmin(@Path("id") id: Long): DiagnosticoAdminDetailDto

    @POST("diagnosticos/{id}/respuestas")
    suspend fun guardarRespuestas(@Path("id") id: Long, @Body request: GuardarRespuestasRequest): GuardarRespuestasResponseDto

    @POST("diagnosticos/{id}/finalizar")
    suspend fun finalizar(@Path("id") id: Long): FinalizarResponseDto

    @GET("diagnosticos/{id}/estado")
    suspend fun consultarEstado(@Path("id") id: Long): EstadoDiagnosticoDto

    @POST("diagnosticos/{id}/evaluaciones")
    suspend fun guardarEvaluaciones(@Path("id") id: Long, @Body request: EvaluacionesRequest): MensajeEstadoResponseDto

    @POST("diagnosticos/{id}/aprobar")
    suspend fun aprobar(@Path("id") id: Long, @Body request: AprobarRequest): MensajeEstadoResponseDto

    @POST("diagnosticos/{id}/rechazar")
    suspend fun rechazar(@Path("id") id: Long, @Body request: RechazarRequest): MensajeEstadoResponseDto

    @POST("diagnosticos/{id}/solicitar-info")
    suspend fun solicitarInfo(@Path("id") id: Long, @Body request: SolicitarInfoRequest): MensajeEstadoResponseDto

    @POST("diagnosticos/{id}/estado")
    suspend fun actualizarEstado(@Path("id") id: Long, @Body request: ActualizarEstadoRequest): MensajeEstadoResponseDto
}

interface CategoriaApiService {
    @GET("categorias")
    suspend fun listar(@Query("tipo") tipo: String): List<SeccionDto>

    @POST("categorias")
    suspend fun crearSeccion(@Query("tipo") tipo: String, @Body request: SeccionRequest): SeccionDto

    @PUT("categorias/orden")
    suspend fun ordenSecciones(@Query("tipo") tipo: String, @Body request: OrdenRequest): OkDto

    @PUT("categorias/{id}")
    suspend fun actualizarSeccion(@Path("id") id: Long, @Body request: SeccionRequest): OkDto

    @DELETE("categorias/{id}")
    suspend fun eliminarSeccion(@Path("id") id: Long): OkDto

    @POST("categorias/{id}/preguntas")
    suspend fun crearPregunta(@Path("id") seccionId: Long, @Body request: PreguntaRequest): PreguntaDto

    @PUT("categorias/{id}/preguntas/orden")
    suspend fun ordenPreguntas(@Path("id") seccionId: Long, @Body request: OrdenRequest): OkDto

    @PUT("categorias/{id}/preguntas/{preguntaId}")
    suspend fun actualizarPregunta(@Path("id") seccionId: Long, @Path("preguntaId") preguntaId: Long, @Body request: PreguntaRequest): OkDto

    @DELETE("categorias/{id}/preguntas/{preguntaId}")
    suspend fun eliminarPregunta(@Path("id") seccionId: Long, @Path("preguntaId") preguntaId: Long): OkDto
}

interface TipoInmuebleApiService {
    @GET("tipos-inmueble")
    suspend fun listar(): List<String>

    @POST("tipos-inmueble")
    suspend fun agregar(@Body request: TipoInmuebleRequest): List<String>

    @DELETE("tipos-inmueble/{nombre}")
    suspend fun eliminar(@Path("nombre") nombre: String): List<String>
}

interface NotificacionApiService {
    @GET("notificaciones")
    suspend fun listar(@Query("page") page: Int = 1, @Query("limit") limit: Int = 50): NotificacionesResponseDto

    @POST("notificaciones/{id}/leer")
    suspend fun marcarLeida(@Path("id") id: Long): OkDto

    @POST("notificaciones/leer-todas")
    suspend fun marcarTodasLeidas(): OkDto
}

interface AdminApiService {
    @GET("admin/usuarios")
    suspend fun listarUsuarios(): List<AdminUsuarioDto>

    @PATCH("admin/usuarios/{id}/activo")
    suspend fun setActivo(@Path("id") id: Long, @Body request: ActivoRequest): OkDto

    @PATCH("admin/usuarios/{id}/cuestionario-asignado")
    suspend fun setCuestionarioAsignado(@Path("id") id: Long, @Body request: CuestionarioAsignadoRequest): OkDto

    @POST("admin/usuarios/{id}/limite-cuestionarios")
    suspend fun incrementarLimite(@Path("id") id: Long): LimiteCuestionariosResponseDto

    @DELETE("admin/usuarios/{id}/limite-cuestionarios")
    suspend fun decrementarLimite(@Path("id") id: Long): LimiteCuestionariosResponseDto

    @DELETE("admin/usuarios/{id}")
    suspend fun eliminarUsuario(@Path("id") id: Long): OkDto

    @GET("admin/diagnosticos-pendientes")
    suspend fun diagnosticosPendientes(@Query("filtro") filtro: String? = null): List<DiagnosticoPendienteDto>

    @GET("admin/diagnosticos")
    suspend fun listarDiagnosticos(@QueryMap params: Map<String, String>): AdminDiagnosticosResponseDto
}

interface EvidenciaApiService {
    @DELETE("evidencias/{id}")
    suspend fun eliminar(@Path("id") id: Long): OkDto
}
