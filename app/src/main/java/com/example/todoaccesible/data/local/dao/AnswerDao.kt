package com.example.todoaccesible.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.todoaccesible.data.local.entities.AnswerEntity

@Dao
interface AnswerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<AnswerEntity>)

    @Query("SELECT * FROM answers WHERE projectId = :projectId")
    suspend fun getAnswersByProject(projectId: String): List<AnswerEntity>
}
