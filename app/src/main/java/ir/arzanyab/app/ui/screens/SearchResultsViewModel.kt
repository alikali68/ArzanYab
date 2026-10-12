package ir.arzanyab.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.arzanyab.app.data.adapter.DigikalaAdapter
import ir.arzanyab.app.data.adapter.ProductQuery
import ir.arzanyab.app.data.adapter.TorobAdapter
import ir.arzanyab.app.domain.engine.EvaluatedPriceItem
import ir.arzanyab.app.domain.engine.PriceTrustEngine
import ir.arzanyab.app.domain.model.SearchFilterState
import ir.arzanyab.app.domain.model.SortOption
import ir.arzanyab.app.domain.normalizer.PersianNormalizer
import ir.arzanyab.app.domain.normalizer.PriceParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Loading : SearchUiState
    data class Success(val data: SearchResultsData) : SearchUiState
    data class Empty(val message: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class SearchResultsData(
    val trustedListings: List<EvaluatedPriceItem>,
    val suspiciousListings: List<EvaluatedPriceItem>,
    val availableSources: List<Pair<String, String>>
)

/** جست‌وجوی متنی ساده (مسیر کلاسیک) — مسیر اصلی اپ از FusedSearchViewModel می‌گذرد */
class SearchResultsViewModel : ViewModel() {

    private val digikalaAdapter = DigikalaAdapter()
    private val torobAdapter = TorobAdapter()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(SearchFilterState())
    val filterState: StateFlow<SearchFilterState> = _filterState.asStateFlow()

    private var allEvaluatedItems: List<EvaluatedPriceItem> = emptyList()

    fun executeSearch(rawQuery: String) {
        val normalizedQuery = PersianNormalizer.normalize(rawQuery)
        if (normalizedQuery.isBlank()) {
            _uiState.value = SearchUiState.Empty("عبارت جست‌وجو خالی است.")
            return
        }
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val query = ProductQuery(queryText = normalizedQuery)
                val digikalaResults = digikalaAdapter.search(query)
                val torobResults = torobAdapter.search(query)
                val rawListings = digikalaResults + torobResults

                if (rawListings.isEmpty()) {
                    _uiState.value = SearchUiState.Empty("نتیجه‌ای برای «$normalizedQuery» پیدا نشد.")
                    return@launch
                }

                val inputItems = rawListings.map { raw ->
                    val parsed = PriceParser.parse(raw.priceText)
                    PriceTrustEngine.RawInputItem(
                        title = raw.title,
                        rawPriceText = raw.priceText,
                        priceInToman = parsed.priceInToman,
                        sellerName = raw.sellerName ?: "فروشگاه آنلاین",
                        isVerifiedSeller = raw.sourceId == "digikala",
                        sourceId = raw.sourceId,
                        sourceName = if (raw.sourceId == "digikala") "دیجی‌کالا" else "ترب",
                        url = raw.url,
                        imageUrl = raw.imageUrl
                    )
                }

                allEvaluatedItems = PriceTrustEngine.evaluateListings(normalizedQuery, inputItems)
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error("خطا در جست‌وجو: ${e.message}")
            }
        }
    }

    fun updateFilters(newState: SearchFilterState) {
        _filterState.value = newState
        applyFilters()
    }

    private fun applyFilters() {
        val filters = _filterState.value

        val trusted = allEvaluatedItems.filter { !it.isBait }
        val suspicious = allEvaluatedItems.filter { it.isBait }

        val filteredTrusted = trusted
            .filter { item ->
                val price = item.priceInToman
                (filters.minPrice == null || (price != null && price >= filters.minPrice)) &&
                (filters.maxPrice == null || (price != null && price <= filters.maxPrice)) &&
                item.trustScore >= filters.minTrustScore &&
                item.sourceId in filters.selectedSources
            }
            .let { list ->
                when (filters.sortOption) {
                    SortOption.CHEAPEST_REAL -> list.sortedBy { it.priceInToman ?: Long.MAX_VALUE }
                    SortOption.MOST_TRUSTED -> list.sortedByDescending { it.trustScore }
                    SortOption.HIGHEST_PRICE -> list.sortedByDescending { it.priceInToman ?: 0L }
                }
            }

        _uiState.value = SearchUiState.Success(
            SearchResultsData(
                trustedListings = filteredTrusted,
                suspiciousListings = suspicious,
                availableSources = listOf("digikala" to "دیجی‌کالا", "torob" to "ترب")
            )
        )
    }
}
