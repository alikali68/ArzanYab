package ir.arzanyab.app.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.arzanyab.app.data.local.AppDatabase
import ir.arzanyab.app.domain.engine.EvaluatedPriceItem
import ir.arzanyab.app.domain.engine.MultiModalSearchRequest
import ir.arzanyab.app.domain.engine.SearchOrchestrator
import ir.arzanyab.app.ml.ModelDownloadManager
import ir.arzanyab.app.ml.ModelDownloadState
import ir.arzanyab.app.ml.SmartImageAnalyzer
import ir.arzanyab.app.ml.VisualEmbeddingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ImageSearchUiState {
    data object Idle : ImageSearchUiState
    data object Analyzing : ImageSearchUiState
    /** نتایج مستقیم جست‌وجوی تصویری ترب (قیمت‌دار) */
    data class VisualResult(val items: List<EvaluatedPriceItem>) : ImageSearchUiState
    /** نتایج تشخیص کالا از عکس → جست‌وجوی قیمت */
    data class SmartResult(val keywords: String, val items: List<EvaluatedPriceItem>) : ImageSearchUiState
    data class Error(val message: String) : ImageSearchUiState
}

class ImageSearchViewModel(application: Application) : AndroidViewModel(application) {

    val downloadManager = ModelDownloadManager(application)
    private val embeddingEngine = VisualEmbeddingEngine(application)
    private val smartAnalyzer = SmartImageAnalyzer(application)
    private val orchestrator = SearchOrchestrator(
        context = application,
        productDao = AppDatabase.getInstance(application).productDao(),
        embeddingEngine = embeddingEngine
    )

    private val _uiState = MutableStateFlow<ImageSearchUiState>(ImageSearchUiState.Idle)
    val uiState: StateFlow<ImageSearchUiState> = _uiState.asStateFlow()

    fun downloadModel() {
        viewModelScope.launch { downloadManager.downloadModel() }
    }

    /**
     * جست‌وجوی تصویری هوشمند ترب: آپلود ناشناس عکس → تطبیق تصویری → نتایج قیمت‌دار.
     * رضایت کاربر برای آپلود باید پیش‌تر در UI گرفته شده باشد.
     */
    fun searchVisually(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImageSearchUiState.Analyzing
            try {
                val items = orchestrator.visualSearch(uri)
                _uiState.value = if (items.isEmpty()) {
                    ImageSearchUiState.Error("کالای مشابهی پیدا نشد. عکس واضح‌تری را امتحان کنید.")
                } else {
                    ImageSearchUiState.VisualResult(items)
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _uiState.value = ImageSearchUiState.Error("خطا در جست‌وجوی تصویری (${t.javaClass.simpleName}): ${t.message}")
            }
        }
    }

    /**
     * تشخیص کالا از روی عکس (کاملاً روی گوشی، بدون آپلود)
     * → ساخت کلیدواژه فارسی → جست‌وجوی قیمت در همه منابع.
     */
    fun analyzeSmart(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImageSearchUiState.Analyzing
            try {
                val keywords = smartAnalyzer.analyzeToKeywords(uri)
                val items = orchestrator.searchAndEvaluate(MultiModalSearchRequest(text = keywords))
                _uiState.value = if (items.isEmpty()) {
                    ImageSearchUiState.Error("برای «$keywords» نتیجه‌ای پیدا نشد.")
                } else {
                    ImageSearchUiState.SmartResult(keywords, items)
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _uiState.value = ImageSearchUiState.Error("خطا در تحلیل عکس (${t.javaClass.simpleName}): ${t.message}")
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = ImageSearchUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        smartAnalyzer.close()
        embeddingEngine.close()
    }
}
