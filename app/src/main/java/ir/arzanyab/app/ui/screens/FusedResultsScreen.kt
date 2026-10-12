package ir.arzanyab.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.arzanyab.app.domain.engine.MultiModalSearchRequest

/** صفحه نتایج جست‌وجوی چندحالته (تلفیق متن/بارکد/عکس) */
@Composable
fun FusedResultsScreen(
    request: MultiModalSearchRequest,
    onBackClick: () -> Unit,
    onOpenDetail: (productId: String) -> Unit,
    viewModel: FusedSearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterState by viewModel.filterState.collectAsState()

    LaunchedEffect(request) {
        viewModel.execute(request)
    }

    SearchResultsContent(
        title = "نتایج جست‌وجوی ترکیبی",
        state = uiState,
        filterState = filterState,
        onBackClick = onBackClick,
        onApplyFilters = viewModel::updateFilters,
        onOpenDetail = onOpenDetail
    )
}
