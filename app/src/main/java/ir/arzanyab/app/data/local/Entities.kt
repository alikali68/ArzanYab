package ir.arzanyab.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_products")
data class TrackedProductEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetThresholdPrice: Long?,
    val initialPrice: Long,
    val latestPrice: Long,
    val sourceId: String,
    val url: String,
    val imageUrl: String?,
    val trackedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "price_history",
    foreignKeys = [
        ForeignKey(
            entity = TrackedProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productId"])]
)
data class PricePointEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val productId: String,
    val price: Long,
    val trustScore: Int,
    val timestamp: Long = System.currentTimeMillis()
)
