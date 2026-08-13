package com.example.todoaccesible.data.repository.impl

import com.example.todoaccesible.core.designsystem.ToastController
import com.example.todoaccesible.core.designsystem.ToastTipo
import com.example.todoaccesible.data.preferences.SessionManager
import com.example.todoaccesible.data.remote.ApiErrorMapper
import com.example.todoaccesible.data.remote.TipoInmuebleApiService
import com.example.todoaccesible.data.remote.dto.TipoInmuebleRequest
import com.example.todoaccesible.data.repository.TipoCuestionarioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.atomic.AtomicBoolean

class TipoCuestionarioRepositoryImpl(
    private val tipoInmuebleApi: TipoInmuebleApiService,
    private val sessionManager: SessionManager,
    private val toastController: ToastController
) : TipoCuestionarioRepository {

    private val _tipos = MutableStateFlow<List<String>>(emptyList())
    private val loaded = AtomicBoolean(false)

    override fun observeTipos(): Flow<List<String>> = _tipos.onStart { if (loaded.compareAndSet(false, true)) refresh() }

    override suspend fun getTipos(): List<String> {
        if (loaded.compareAndSet(false, true)) refresh()
        return _tipos.value
    }

    private suspend fun refresh() {
        try {
            _tipos.value = tipoInmuebleApi.listar()
        } catch (e: Exception) {
            loaded.set(false)
            reportError(e)
        }
    }

    override suspend fun addTipo(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty() || _tipos.value.contains(limpio)) return
        try {
            _tipos.value = tipoInmuebleApi.agregar(TipoInmuebleRequest(limpio))
        } catch (e: Exception) {
            reportError(e)
        }
    }

    override suspend fun deleteTipo(nombre: String) {
        if (_tipos.value.size <= 1) return
        try {
            _tipos.value = tipoInmuebleApi.eliminar(nombre)
        } catch (e: Exception) {
            reportError(e)
        }
    }

    private suspend fun reportError(e: Exception) {
        val mapped = ApiErrorMapper.handle(e, sessionManager)
        toastController.show(mapped.message, ToastTipo.ERROR)
    }
}
