package ir.arzanyab.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.arzanyab.app.ml.ModelDownloadState
import ir.arzanyab.app.ui.components.LoadingView
import ir.arzanyab.app.ui.components.SoftCard
import ir.arzanyab.app.util.PersianFormatter
import ir.arzanyab.app.util.openListingUrl

/**
 * جست‌وجوی عکسی — دو مسیر:
 * ۱) جست‌وجوی تصویری ترب (تطبیق تصویری دقیق؛ آپلود ناشناس با رضایت کاربر)
 * ۲) تشخیص کالا از عکس (شناسایی خودکار روی گوشی → جست‌وجوی قیمت؛ بدون آپلود)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSearchScreen(
    onBackClick: () -> Unit,
    viewModel: ImageSearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val downloadState by viewModel.downloadManager.state.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showChoiceDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUri = uri
            showChoiceDialog = true
        }
    }

    // دیالوگ انتخاب روش جست‌وجو — کارت‌های تصویری به‌جای دکمه ساده
    if (showChoiceDialog && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { showChoiceDialog = false; pendingUri = null },
            title = { Text("روش جست‌وجوی عکسی") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SearchMethodRow(
                        icon = Icons.Default.ImageSearch,
                        title = "جست‌وجوی تصویری ترب",
                        description = "تطبیق دقیق با کاتالوگ ترب • آپلود ناشناس",
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val uri = pendingUri
                            showChoiceDialog = false; pendingUri = null
                            if (uri != null) viewModel.searchVisually(uri)
                        }
                    )
                    SearchMethodRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "تشخیص کالا از عکس",
                        description = "شناسایی خودکار روی گوشی • بدون آپلود • خصوصی",
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            val uri = pendingUri
                            showChoiceDialog = false; pendingUri = null
                            if (uri != null) viewModel.analyzeSmart(uri)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showChoiceDialog = false; pendingUri = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جست‌وجوی عکسی") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // دکمه انتخاب عکس با آیکون در قاب
                SoftCard(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "انتخاب عکس کالا",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "از گالری انتخاب کنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // کارت دانلود مدل — فشرده و تمیز
            item {
                when (val ds = downloadState) {
                    is ModelDownloadState.Idle, is ModelDownloadState.Error -> {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer
                                                .copy(alpha = 0.6f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "مدل تصویری (${viewModel.downloadManager.getApproxSizeText()})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "یک‌بار دانلود برای جست‌وجوی ترکیبی",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (ds is ModelDownloadState.Error) {
                                        Text(
                                            ds.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = { viewModel.downloadModel() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                            ) {
                                Text("دانلود مدل")
                            }
                        }
                    }
                    is ModelDownloadState.Downloading -> {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "در حال دانلود مدل… ${PersianFormatter.toPersianDigits(ds.progressPercent.toLong())}٪",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { ds.progressPercent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    is ModelDownloadState.Ready -> { }
                }
            }

            when (val s = uiState) {
                is ImageSearchUiState.Analyzing -> {
                    item { LoadingView("در حال جست‌وجو…") }
                }
                is ImageSearchUiState.Error -> {
                    item {
                        Text(
                            s.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is ImageSearchUiState.VisualResult -> {
                    item {
                        ResultHeader(
                            title = "کالاهای مشابه در ترب",
                            count = s.items.size
                        )
                    }
                    items(s.items, key = { it.url + it.priceInToman }) { item ->
                        EvaluatedListingCard(
                            item = item,
                            onOpenUrl = { openListingUrl(context, item.url) },
                            onFollowClick = null
                        )
                    }
                }
                is ImageSearchUiState.SmartResult -> {
                    item {
                        SoftCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer
                                                .copy(alpha = 0.6f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "کالای شناسایی‌شده:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        s.keywords,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        ResultHeader(
                            title = "نتایج قیمت",
                            count = s.items.size
                        )
                    }
                    items(s.items, key = { it.url + it.priceInToman }) { item ->
                        EvaluatedListingCard(
                            item = item,
                            onOpenUrl = { openListingUrl(context, item.url) },
                            onFollowClick = null
                        )
                    }
                }
                is ImageSearchUiState.Idle -> { }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

/** ردیف انتخاب روش: آیکون در قاب رنگی + تیتر + توضیح — بدون باکس اضافه */
@Composable
private fun SearchMethodRow(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResultHeader(title: String, count: Int) {
    Text(
        text = "$title (${PersianFormatter.toPersianDigits(count.toLong())} مورد)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

