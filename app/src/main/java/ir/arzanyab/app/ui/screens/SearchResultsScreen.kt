package ir.arzanyab.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

/** صفحه نتایج جست‌وجوی متنی ساده */
@Composable
fun SearchResultsScreen(
    query: String,
    onBackClick: () -> Unit,
    onOpenDetail: (productId: String) -> Unit,
    viewModel: SearchResultsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    LaunchedEffect(query) {
        viewModel.executeSearch(query)
    }

    SearchResultsContent(
        title = "نتایج: $query",
        state = uiState,
        filterState = filterState,
        onBackClick = onBackClick,
        onApplyFilters = viewModel::updateFilters,
        onOpenDetail = onOpenDetail
    )
}
