package com.example.todoaccesible.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.example.todoaccesible.data.local.entities.PhotoEntity

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun delete(photoId: Long)

    @Query("SELECT * FROM photos WHERE answerId = :answerId")
    fun observeForAnswer(answerId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE answerId IN (:answerIds)")
    suspend fun getForAnswers(answerIds: List<Long>): List<PhotoEntity>
}
