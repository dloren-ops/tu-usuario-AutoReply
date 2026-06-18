package com.autoreply.bot.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface KnownGroupDao {

    @Upsert
    suspend fun upsert(entity: KnownGroupEntity)

    @Query("SELECT * FROM known_groups ORDER BY lastSeenAt DESC")
    fun observeAll(): Flow<List<KnownGroupEntity>>

    @Query("SELECT * FROM known_groups WHERE conversationKey = :key LIMIT 1")
    suspend fun getByConversationKey(key: String): KnownGroupEntity?

    @Query("DELETE FROM known_groups WHERE lastSeenAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
