package ir.arzanyab.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.arzanyab.app.data.local.AppDatabase
import ir.arzanyab.app.data.local.TrackedProductEntity
import ir.arzanyab.app.domain.engine.EvaluatedPriceItem
import ir.arzanyab.app.domain.model.SearchFilterState
import ir.arzanyab.app.ui.components.EmptyView
import ir.arzanyab.app.ui.components.ErrorView
import ir.arzanyab.app.ui.components.LoadingView
import ir.arzanyab.app.ui.components.PriceText
import ir.arzanyab.app.ui.components.SoftCard
import ir.arzanyab.app.ui.components.SourceChip
import ir.arzanyab.app.ui.components.TrustBadge
import ir.arzanyab.app.ui.components.FilterBottomSheet
import ir.arzanyab.app.util.PersianFormatter
import ir.arzanyab.app.util.openListingUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** دنبال‌کردن یک نتیجه جست‌وجو: ذخیره در دیتابیس + رفتن به صفحه جزئیات */
fun CoroutineScope.followListing(
    context: Context,
    item: EvaluatedPriceItem,
    onDone: (productId: String) -> Unit
) {
    launch(Dispatchers.IO) {
        val price = item.priceInToman ?: return@launch
        val dao = AppDatabase.getInstance(context).productDao()
        val id = "${item.sourceId}_${item.url.hashCode()}"
        if (dao.getTrackedProductById(id) == null) {
            dao.insertTrackedProduct(
                TrackedProductEntity(
                    id = id,
                    title = item.title,
                    targetThresholdPrice = price, // هشدار با هر کاهش زیر قیمت فعلی
                    initialPrice = price,
                    latestPrice = price,
                    sourceId = item.sourceId,
                    url = item.url,
                    imageUrl = item.imageUrl
                )
            )
            dao.recordNewPrice(id, price, item.trustScore)
        }
        withContext(Dispatchers.Main) { onDone(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsContent(
    title: String,
    state: SearchUiState,
    filterState: SearchFilterState,
    onBackClick: () -> Unit,
    onApplyFilters: (SearchFilterState) -> Unit,
    onOpenDetail: (productId: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (state is SearchUiState.Success) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "فیلتر")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (state) {
            is SearchUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    verticalArrangement = Arrangement.Center
                ) {
                    LoadingView("در حال جست‌وجو و ارزیابی قیمت‌ها…")
                }
            }
            is SearchUiState.Empty -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyView(Icons.Default.SearchOff, state.message)
                }
            }
            is SearchUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    verticalArrangement = Arrangement.Center
                ) {
                    ErrorView(state.message)
                }
            }
            is SearchUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.data.trustedListings, key = { it.url + it.priceInToman }) { item ->
                        EvaluatedListingCard(
                            item = item,
                            onOpenUrl = { openListingUrl(context, item.url) },
                            onFollowClick = {
                                scope.followListing(context, item, onOpenDetail)
                            }
                        )
                    }
                    if (state.data.suspiciousListings.isNotEmpty()) {
                        item {
                            SuspiciousSectionHeader(count = state.data.suspiciousListings.size)
                        }
                        items(state.data.suspiciousListings, key = { "s_" + it.url + it.priceInToman }) { item ->
                            EvaluatedListingCard(
                                item = item,
                                onOpenUrl = { openListingUrl(context, item.url) },
                                onFollowClick = null
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet && state is SearchUiState.Success) {
        FilterBottomSheet(
            filterState = filterState,
            availableSources = state.data.availableSources,
            onApplyFilters = onApplyFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}



@Composable
private fun SuspiciousSectionHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "موارد مشکوک یا طعمه (${PersianFormatter.toPersianDigits(count.toLong())} مورد)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * کارت نتیجه: تصویر فشرده و دقیقاً به‌اندازه، تیتر، قیمت برجسته،
 * چیپ منبع و بج اعتماد در یک ردیف — بدون باکس اضافه.
 */
@Composable
fun EvaluatedListingCard(
    item: EvaluatedPriceItem,
    onOpenUrl: () -> Unit,
    onFollowClick: (() -> Unit)? = null
) {
    SoftCard(onClick = onOpenUrl) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // تصویر: دقیقاً 76dp، بدون باکس بزرگ‌تر
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                PriceText(
                    priceText = if (item.priceInToman != null)
                        PersianFormatter.formatPrice(item.priceInToman)
                    else item.rawPriceText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceChip(sourceName = item.sourceName)
                    Spacer(modifier = Modifier.width(8.dp))
                    TrustBadge(score = item.trustScore)
                }
                if (item.isBait && item.baitReason != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.baitReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (onFollowClick != null && !item.isBait) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .clickable(onClick = onFollowClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.BookmarkAdd,
                        contentDescription = "دنبال کردن",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
