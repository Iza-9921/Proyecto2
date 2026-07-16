package com.example.todoaccesible.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.todoaccesible.data.local.entities.QuestionEntity
import com.example.todoaccesible.data.local.entities.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionCatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<SectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Query("SELECT COUNT(*) FROM sections")
    suspend fun sectionCount(): Int

    @Query("SELECT * FROM sections ORDER BY orden ASC")
    fun observeSections(): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections ORDER BY orden ASC")
    suspend fun getAllSections(): List<SectionEntity>

    @Query("SELECT * FROM questions ORDER BY orden ASC")
    fun observeQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY orden ASC")
    suspend fun getAllQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE seccionId = :sectionId ORDER BY orden ASC")
    suspend fun getQuestionsForSection(sectionId: String): List<QuestionEntity>
}
