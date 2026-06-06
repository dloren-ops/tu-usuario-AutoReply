package com.autoreply.bot.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Query("SELECT * FROM reply_logs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<LogEntity>>

    @Insert
    suspend fun insert(log: LogEntity): Long

    @Query("DELETE FROM reply_logs")
    suspend fun clear()

    /** Limpia registros antiguos dejando solo los mas recientes. */
    @Query(
        "DELETE FROM reply_logs WHERE id NOT IN " +
            "(SELECT id FROM reply_logs ORDER BY timestamp DESC LIMIT :keep)"
    )
    suspend fun trim(keep: Int = 500)
}
