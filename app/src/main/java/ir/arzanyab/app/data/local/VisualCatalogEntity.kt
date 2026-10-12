package ir.arzanyab.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** بردار امبدینگ ۵۱۲ بُعدی به‌صورت BLOB فشرده (۲۰۴۸ بایت) */
@Entity(tableName = "visual_catalog")
data class VisualCatalogEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val embeddingBlob: ByteArray
) {
    fun toEmbeddingArray(): FloatArray {
        val buffer = ByteBuffer.wrap(embeddingBlob).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(embeddingBlob.size / 4)
        buffer.asFloatBuffer().get(floats)
        return floats
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VisualCatalogEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun fromFloatArray(
            id: String,
            title: String,
            category: String,
            imageUrl: String,
            embedding: FloatArray
        ): VisualCatalogEntity {
            val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            buffer.asFloatBuffer().put(embedding)
            return VisualCatalogEntity(id, title, category, imageUrl, buffer.array())
        }
    }
}
