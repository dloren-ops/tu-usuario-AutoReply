package com.autoreply.bot.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RuleEntity::class, LogEntity::class, ReplyStateEntity::class, KnownGroupEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ruleDao(): RuleDao
    abstract fun logDao(): LogDao
    abstract fun replyStateDao(): ReplyStateDao
    abstract fun knownGroupDao(): KnownGroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migracion v1 -> v2: agrega los campos de alcance y frecuencia por regla.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rules ADD COLUMN scope TEXT NOT NULL DEFAULT 'ALL'")
                db.execSQL("ALTER TABLE rules ADD COLUMN frequency TEXT NOT NULL DEFAULT 'ALWAYS'")
                db.execSQL("ALTER TABLE rules ADD COLUMN everyHours INTEGER NOT NULL DEFAULT 24")
            }
        }

        // Migracion v2 -> v3: tabla para recordar respuestas por conversacion.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS reply_state (" +
                        "`key` TEXT NOT NULL PRIMARY KEY, " +
                        "lastReplyAt INTEGER NOT NULL)"
                )
            }
        }

        // Migracion v3 -> v4: agrega el titulo/nombre de la regla.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rules ADD COLUMN title TEXT NOT NULL DEFAULT ''")
            }
        }

        // Migracion v4 -> v5: tabla de grupos conocidos + campo allowedGroupIds en rules.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS known_groups (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "packageName TEXT NOT NULL, " +
                        "groupName TEXT NOT NULL, " +
                        "conversationKey TEXT NOT NULL, " +
                        "lastSeenAt INTEGER NOT NULL, " +
                        "communityParent TEXT)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_known_groups_conversationKey " +
                        "ON known_groups (conversationKey)"
                )
                db.execSQL("ALTER TABLE rules ADD COLUMN allowedGroupIds TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autoreply.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
