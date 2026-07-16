package com.example.todoaccesible.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoaccesible.data.local.entities.AnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(answer: AnswerEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(answers: List<AnswerEntity>)

    @Update
    suspend fun update(answer: AnswerEntity)

    @Query("SELECT * FROM answers WHERE diagnosticId = :diagnosticId")
    fun observeForDiagnostic(diagnosticId: Long): Flow<List<AnswerEntity>>

    @Query("SELECT * FROM answers WHERE diagnosticId = :diagnosticId")
    suspend fun getForDiagnostic(diagnosticId: Long): List<AnswerEntity>

    @Query("SELECT * FROM answers WHERE diagnosticId = :diagnosticId AND questionCodigo = :codigo LIMIT 1")
    suspend fun findAnswer(diagnosticId: Long, codigo: String): AnswerEntity?
}
