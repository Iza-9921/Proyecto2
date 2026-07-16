package com.example.todoaccesible.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.todoaccesible.data.local.entities.DiagnosticEntity
import com.example.todoaccesible.data.model.DiagnosticStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticDao {
    @Insert
    suspend fun insert(diagnostic: DiagnosticEntity): Long

    @Update
    suspend fun update(diagnostic: DiagnosticEntity)

    @Query("DELETE FROM diagnostics WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM diagnostics WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): DiagnosticEntity?

    @Query("SELECT * FROM diagnostics WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<DiagnosticEntity?>

    @Query("SELECT * FROM diagnostics WHERE clienteId = :clienteId ORDER BY fechaCreacion DESC")
    fun observeForCliente(clienteId: Long): Flow<List<DiagnosticEntity>>

    @Query("SELECT * FROM diagnostics WHERE estado != :borrador ORDER BY fechaEnvio DESC")
    fun observeAllSubmitted(borrador: DiagnosticStatus = DiagnosticStatus.BORRADOR): Flow<List<DiagnosticEntity>>

    @Query("SELECT * FROM diagnostics WHERE clienteId = :clienteId AND estado = :borrador LIMIT 1")
    suspend fun findDraftForCliente(clienteId: Long, borrador: DiagnosticStatus = DiagnosticStatus.BORRADOR): DiagnosticEntity?
}
