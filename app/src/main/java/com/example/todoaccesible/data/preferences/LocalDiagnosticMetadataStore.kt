package com.example.todoaccesible.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.localMetaDataStore: DataStore<Preferences> by preferencesDataStore(name = "diagnostico_local_meta_prefs")

/**
 * Campos del formulario "datos del proyecto" que hay que poder mostrar de
 * vuelta en la UI, pero que o bien no tienen columna propia en el backend
 * (`responsable`, `telefono`, `clienteNombre`, `revision`, `logoEmpresaUri`)
 * o sí la tienen en `Proyecto` pero el shape de `GET /diagnosticos/:id` para
 * rol cliente no los re-expone (solo manda `nombre_proyecto`, ver
 * `diagnostico.service.ts` `obtenerParaCliente` confirmado en el código
 * fuente) — así que para que sobrevivan a que se reinicie el proceso
 * también se cachean aquí, no solo los que genuinamente no existen en el
 * backend.
 */
data class LocalDiagnosticMetadata(
    val projectName: String = "",
    val ubicacion: String = "",
    val entidadFederativa: String = "",
    val ciudad: String = "",
    val tipoInmueble: String = "",
    val fechaEvaluacion: Long? = null,
    val responsable: String = "",
    val telefono: String = "",
    val clienteNombre: String = "",
    val revision: String = "1",
    val logoEmpresaUri: String? = null
)

/**
 * Guarda, indexado por el id REAL del diagnóstico en el backend (o por el id
 * local negativo mientras el borrador todavía no se crea en el backend), los
 * campos de [LocalDiagnosticMetadata]. Mismo patrón DataStore que
 * campo con nombres compuestos por id. Se pierde solo si se reinstala la app o se borran
 * sus datos (aceptable: son campos del formulario, no el cuestionario en sí).
 */
class LocalDiagnosticMetadataStore(private val context: Context) {
    private fun key(diagnosticId: Long, field: String) = stringPreferencesKey("meta_${diagnosticId}_$field")
    private fun longKey(diagnosticId: Long, field: String) = longPreferencesKey("meta_${diagnosticId}_$field")

    suspend fun get(diagnosticId: Long): LocalDiagnosticMetadata {
        val prefs = context.localMetaDataStore.data.first()
        return LocalDiagnosticMetadata(
            projectName = prefs[key(diagnosticId, "projectName")].orEmpty(),
            ubicacion = prefs[key(diagnosticId, "ubicacion")].orEmpty(),
            entidadFederativa = prefs[key(diagnosticId, "entidadFederativa")].orEmpty(),
            ciudad = prefs[key(diagnosticId, "ciudad")].orEmpty(),
            tipoInmueble = prefs[key(diagnosticId, "tipoInmueble")].orEmpty(),
            fechaEvaluacion = prefs[longKey(diagnosticId, "fechaEvaluacion")]?.takeIf { it > 0 },
            responsable = prefs[key(diagnosticId, "responsable")].orEmpty(),
            telefono = prefs[key(diagnosticId, "telefono")].orEmpty(),
            clienteNombre = prefs[key(diagnosticId, "clienteNombre")].orEmpty(),
            revision = prefs[key(diagnosticId, "revision")] ?: "1",
            logoEmpresaUri = prefs[key(diagnosticId, "logoEmpresaUri")]
        )
    }

    suspend fun set(diagnosticId: Long, metadata: LocalDiagnosticMetadata) {
        context.localMetaDataStore.edit { prefs ->
            prefs[key(diagnosticId, "projectName")] = metadata.projectName
            prefs[key(diagnosticId, "ubicacion")] = metadata.ubicacion
            prefs[key(diagnosticId, "entidadFederativa")] = metadata.entidadFederativa
            prefs[key(diagnosticId, "ciudad")] = metadata.ciudad
            prefs[key(diagnosticId, "tipoInmueble")] = metadata.tipoInmueble
            prefs[longKey(diagnosticId, "fechaEvaluacion")] = metadata.fechaEvaluacion ?: -1L
            prefs[key(diagnosticId, "responsable")] = metadata.responsable
            prefs[key(diagnosticId, "telefono")] = metadata.telefono
            prefs[key(diagnosticId, "clienteNombre")] = metadata.clienteNombre
            prefs[key(diagnosticId, "revision")] = metadata.revision
            if (metadata.logoEmpresaUri != null) {
                prefs[key(diagnosticId, "logoEmpresaUri")] = metadata.logoEmpresaUri
            } else {
                prefs.remove(key(diagnosticId, "logoEmpresaUri"))
            }
        }
    }

    /** Cuando un borrador se promueve de id local (negativo) a id real del backend: copia y borra el original. */
    suspend fun migrate(fromId: Long, toId: Long) {
        val meta = get(fromId)
        set(toId, meta)
        clear(fromId)
    }

    suspend fun clear(diagnosticId: Long) {
        context.localMetaDataStore.edit { prefs ->
            prefs.remove(key(diagnosticId, "projectName"))
            prefs.remove(key(diagnosticId, "ubicacion"))
            prefs.remove(key(diagnosticId, "entidadFederativa"))
            prefs.remove(key(diagnosticId, "ciudad"))
            prefs.remove(key(diagnosticId, "tipoInmueble"))
            prefs.remove(longKey(diagnosticId, "fechaEvaluacion"))
            prefs.remove(key(diagnosticId, "responsable"))
            prefs.remove(key(diagnosticId, "telefono"))
            prefs.remove(key(diagnosticId, "clienteNombre"))
            prefs.remove(key(diagnosticId, "revision"))
            prefs.remove(key(diagnosticId, "logoEmpresaUri"))
        }
    }
}
