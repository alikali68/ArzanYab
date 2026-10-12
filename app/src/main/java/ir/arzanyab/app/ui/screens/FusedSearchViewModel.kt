package ir.arzanyab.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.arzanyab.app.data.local.AppDatabase
import ir.arzanyab.app.domain.engine.EvaluatedPriceItem
import ir.arzanyab.app.domain.engine.MultiModalSearchRequest
import ir.arzanyab.app.domain.engine.SearchOrchestrator
import ir.arzanyab.app.domain.engine.SmartQueryParser
import ir.arzanyab.app.domain.model.SearchFilterState
import ir.arzanyab.app.domain.model.SortOption
import ir.arzanyab.app.ml.VisualEmbeddingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** اجرای جست‌وجوی چندحالته (متن/بارکد/عکس) با تلفیق RRF */
class FusedSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).productDao()
    private val orchestrator = SearchOrchestrator(
        context = application,
        productDao = dao,
        embeddingEngine = VisualEmbeddingEngine(application)
    )

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(SearchFilterState())
    val filterState: StateFlow<SearchFilterState> = _filterState.asStateFlow()

    private var allEvaluatedItems: List<EvaluatedPriceItem> = emptyList()

    fun execute(request: MultiModalSearchRequest) {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                // فهم هوشمند پرس‌وجو: قید قیمت و مرتب‌سازی از متن جدا می‌شود
                val parsed = SmartQueryParser.parse(request.text)
                val cleanRequest = if (parsed.cleanQuery != request.text) {
                    request.copy(text = parsed.cleanQuery.ifBlank { null })
                } else request

                allEvaluatedItems = orchestrator.searchAndEvaluate(cleanRequest)

                // اعمال خودکار قیدهای فهمیده‌شده روی فیلترها
                if (parsed.maxPriceToman != null || parsed.minPriceToman != null ||
                    parsed.preferCheapest || parsed.preferExpensive
                ) {
                    val cur = _filterState.value
                    _filterState.value = cur.copy(
                        maxPrice = parsed.maxPriceToman ?: cur.maxPrice,
                        minPrice = parsed.minPriceToman ?: cur.minPrice,
                        sortOption = when {
                            parsed.preferCheapest -> SortOption.CHEAPEST_REAL
                            parsed.preferExpensive -> SortOption.HIGHEST_PRICE
                            else -> cur.sortOption
                        }
                    )
                }

                if (allEvaluatedItems.isEmpty()) {
                    _uiState.value = SearchUiState.Empty("نتیجه‌ای پیدا نشد. ورودی دیگری را امتحان کنید.")
                    return@launch
                }
                applyFilters()
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                // عمداً Throwable: به‌جای کرش برنامه، علت واقعی خطا نمایش داده می‌شود
                _uiState.value = SearchUiState.Error(
                    "خطا در جست‌وجوی ترکیبی (${t.javaClass.simpleName}): ${t.message}"
                )
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
                availableSources = listOf(
                    "digikala" to "دیجی‌کالا",
                    "torob" to "ترب",
                    "divar" to "دیوار",
                    "emalls" to "ایمالز",
                    "snappmarket" to "اسنپ‌مارکت",
                    "okala" to "اکالا"
                ).filter { (id, _) -> allEvaluatedItems.any { it.sourceId == id } }
            )
        )
    }
}
