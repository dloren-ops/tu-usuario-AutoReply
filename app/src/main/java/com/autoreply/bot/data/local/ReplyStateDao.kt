package com.autoreply.bot.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReplyStateDao {

    @Query("SELECT lastReplyAt FROM reply_state WHERE `key` = :key")
    suspend fun getLastReplyAt(key: String): Long?

    @Query("SELECT * FROM reply_state")
    suspend fun getAll(): List<ReplyStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ReplyStateEntity)

    @Query("DELETE FROM reply_state")
    suspend fun clear()

    /** Borra el estado de frecuencia de una regla concreta (claves "rule:<id>|..."). */
    @Query("DELETE FROM reply_state WHERE `key` LIKE :prefix || '%'")
    suspend fun clearForRulePrefix(prefix: String)
}
