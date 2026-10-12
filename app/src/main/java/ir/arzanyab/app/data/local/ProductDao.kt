package ir.arzanyab.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM tracked_products ORDER BY trackedAt DESC")
    fun getAllTrackedProducts(): Flow<List<TrackedProductEntity>>

    @Query("SELECT * FROM tracked_products WHERE id = :id")
    suspend fun getTrackedProductById(id: String): TrackedProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedProduct(product: TrackedProductEntity)

    @Query("DELETE FROM tracked_products WHERE id = :id")
    suspend fun deleteTrackedProduct(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPricePoint(point: PricePointEntity)

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp ASC")
    fun getPriceHistory(productId: String): Flow<List<PricePointEntity>>

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp ASC")
    suspend fun getPriceHistorySync(productId: String): List<PricePointEntity>

    @Query("UPDATE tracked_products SET latestPrice = :latestPrice WHERE id = :id")
    suspend fun updateLatestPrice(id: String, latestPrice: Long)

    @Transaction
    suspend fun recordNewPrice(productId: String, newPrice: Long, trustScore: Int) {
        updateLatestPrice(productId, newPrice)
        insertPricePoint(PricePointEntity(productId = productId, price = newPrice, trustScore = trustScore))
    }

    // کاتالوگ بصری (جست‌وجوی عکسی)
    @Query("SELECT * FROM visual_catalog")
    suspend fun getAllVisualCatalog(): List<VisualCatalogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisualItems(items: List<VisualCatalogEntity>)
}
