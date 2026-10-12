package ir.arzanyab.app.domain.model

enum class SortOption(val title: String) {
    CHEAPEST_REAL("ارزان‌ترین واقعی"),
    MOST_TRUSTED("بیشترین اعتماد"),
    HIGHEST_PRICE("گران‌ترین")
}

data class SearchFilterState(
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val minTrustScore: Int = 40,
    val selectedSources: Set<String> = setOf("digikala", "torob", "divar", "emalls", "snappmarket", "okala"),
    val sortOption: SortOption = SortOption.CHEAPEST_REAL
)
