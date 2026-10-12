package ir.arzanyab.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.arzanyab.app.data.local.AppDatabase
import ir.arzanyab.app.data.local.PricePointEntity
import ir.arzanyab.app.data.local.ProductDao
import ir.arzanyab.app.data.local.TrackedProductEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: TrackedProductEntity? = null,
    val isLoading: Boolean = true
)

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val dao: ProductDao = AppDatabase.getInstance(application).productDao()

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState

    private var currentProductId: String? = null

    fun loadProduct(productId: String) {
        currentProductId = productId
        viewModelScope.launch {
            val product = dao.getTrackedProductById(productId)
            _uiState.value = ProductDetailUiState(product = product, isLoading = false)
        }
    }

    fun getPriceHistoryFlow(productId: String) =
        dao.getPriceHistory(productId).stateIn(viewModelScope, SharingStarted.Lazily, emptyList<PricePointEntity>())

    fun toggleFollow(thresholdPrice: Long) {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            val updated = product.copy(
                targetThresholdPrice = if (product.targetThresholdPrice != null) null else thresholdPrice
            )
            dao.insertTrackedProduct(updated)
            _uiState.value = _uiState.value.copy(product = updated)
        }
    }

    fun unfollow(onDone: () -> Unit) {
        val id = currentProductId ?: return
        viewModelScope.launch {
            dao.deleteTrackedProduct(id)
            onDone()
        }
    }
}
