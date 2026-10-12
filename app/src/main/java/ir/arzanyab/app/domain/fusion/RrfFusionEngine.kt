package ir.arzanyab.app.domain.fusion

import ir.arzanyab.app.data.adapter.RawListing

data class RankedItem(val listing: RawListing, val sourceModality: ModalityType)

enum class ModalityType(val weight: Double) {
    BARCODE(2.0),
    TEXT(1.2),
    IMAGE(0.8)
}

data class FusedListing(
    val listing: RawListing,
    val rrfScore: Double,
    val matchedModalities: Set<ModalityType>
)

/**
 * تلفیق رتبه‌ها با روش Reciprocal Rank Fusion:
 * RRF_score(d) = Σ weight_i / (k + rank_i(d))  ;  k = 60
 */
object RrfFusionEngine {
    private const val K = 60

    fun fuseRankings(rankedLists: Map<ModalityType, List<RawListing>>): List<FusedListing> {
        val scoreMap = mutableMapOf<String, Double>()
        val listingMap = mutableMapOf<String, RawListing>()
        val modalityMatches = mutableMapOf<String, MutableSet<ModalityType>>()

        for ((modality, list) in rankedLists) {
            val weight = modality.weight
            list.forEachIndexed { index, listing ->
                val rank = index + 1
                val contribution = weight * (1.0 / (K + rank))
                val key = listing.url.ifBlank { listing.title.trim().lowercase() }
                scoreMap[key] = (scoreMap[key] ?: 0.0) + contribution
                // نگه‌داشتن بهترین لیستینگ (اولین دیده‌شده = رتبه بهتر در مودالیتی خودش)
                if (key !in listingMap) listingMap[key] = listing
                modalityMatches.getOrPut(key) { mutableSetOf() }.add(modality)
            }
        }

        return scoreMap.entries
            .map { (key, score) ->
                FusedListing(
                    listing = listingMap.getValue(key),
                    rrfScore = score,
                    matchedModalities = modalityMatches[key] ?: emptySet()
                )
            }
            .sortedByDescending { it.rrfScore }
    }
}
