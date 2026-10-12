package ir.arzanyab.app.domain.engine

import ir.arzanyab.app.data.local.ProductDao
import ir.arzanyab.app.data.local.VisualCatalogEntity
import kotlin.math.sqrt

sealed interface VisualMatchOutcome {
    data class HighConfidence(val topMatch: VisualCatalogEntity, val similarity: Float) : VisualMatchOutcome
    data class ModerateConfidence(val suggestions: List<VisualCatalogEntity>) : VisualMatchOutcome
    data class LowConfidence(val categoryGuess: String?) : VisualMatchOutcome
}

/** تطبیق امبدینگ عکس کاربر با کاتالوگ بصری محلی (شباهت کسینوسی) */
class VisualMatcher(private val productDao: ProductDao) {

    suspend fun match(queryEmbedding: FloatArray): VisualMatchOutcome {
        val catalog = productDao.getAllVisualCatalog()
        if (catalog.isEmpty()) {
            return VisualMatchOutcome.LowConfidence(null)
        }

        val scored = catalog.map { entity ->
            entity to cosineSimilarity(queryEmbedding, entity.toEmbeddingArray())
        }.sortedByDescending { it.second }

        val (topEntity, topScore) = scored.first()

        return when {
            topScore >= 0.75f -> VisualMatchOutcome.HighConfidence(topEntity, topScore)
            topScore >= 0.55f -> VisualMatchOutcome.ModerateConfidence(scored.take(3).map { it.first })
            else -> VisualMatchOutcome.LowConfidence(topEntity.category)
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0) 0f else (dot / denom).toFloat()
    }
}
