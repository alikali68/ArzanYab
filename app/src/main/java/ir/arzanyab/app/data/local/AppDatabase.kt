package ir.arzanyab.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ir.arzanyab.app.data.security.SecurityKeyManager
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [TrackedProductEntity::class, PricePointEntity::class, VisualCatalogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = SecurityKeyManager.getDatabasePassphrase(context.applicationContext)
                val factory = SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arzanyab_secure.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        @Synchronized
        fun closeDatabaseInstance() {
            INSTANCE?.let { db -> if (db.isOpen) db.close() }
            INSTANCE = null
        }
    }
}
