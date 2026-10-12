package ir.arzanyab.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import ir.arzanyab.app.ui.components.GradientButton
import ir.arzanyab.app.ui.components.HeroPanel
import ir.arzanyab.app.ui.components.MethodCard
import java.io.File

/**
 * صفحه خانه: متن + بارکد + عکس، هرکدام تنها یا ترکیبی.
 * همه ورودی‌ها از مسیر تلفیقی (results_fused) عبور می‌کنند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scannedBarcode: String? = null,
    onOpenScanner: () -> Unit = {},
    onOpenImageSearch: () -> Unit = {},
    onOpenTracked: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onExecuteFusedSearch: (queryText: String?, barcode: String?, imageUri: Uri?) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var fusedBarcode by remember(scannedBarcode) { mutableStateOf(scannedBarcode) }
    var fusedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageOptions by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) fusedImageUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) cameraUri?.let { fusedImageUri = it } }

    fun launchCamera() {
        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    LaunchedEffect(scannedBarcode) {
        if (scannedBarcode != null) fusedBarcode = scannedBarcode
    }

    val hasInput = searchQuery.isNotBlank() || fusedBarcode != null || fusedImageUri != null

    fun executeSearch() {
        val text = searchQuery.trim().takeIf { it.isNotBlank() }
        if (text == null && fusedBarcode == null && fusedImageUri == null) return
        onExecuteFusedSearch(text, fusedBarcode, fusedImageUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ارزان‌یاب", style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    IconButton(onClick = onOpenTracked) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "دنبال‌شده‌ها")
                    }
                    IconButton(onClick = onOpenPrivacy) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            HeroPanel(
                icon = Icons.Default.Sell,
                title = "ارزان‌یاب",
                subtitle = "ارزان‌ترین قیمت، بدون واسطه"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // نوار جست‌وجو با سایه نرم
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    ),
                placeholder = { Text("نام کالا را بنویسید…") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "پاک کردن")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { executeSearch() })
            )

            Spacer(modifier = Modifier.height(14.dp))

            GradientButton(
                text = "جستجوی قیمت",
                icon = Icons.Default.Search,
                onClick = { executeSearch() },
                enabled = hasInput
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MethodCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "اسکن بارکد",
                    subtitle = "اسکن با دوربین",
                    accent = MaterialTheme.colorScheme.secondary,
                    onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
                    badge = fusedBarcode?.let { "خوانده شد ✓" },
                    onClick = onOpenScanner,
                    modifier = Modifier.weight(1f)
                )
                MethodCard(
                    icon = Icons.Default.CameraAlt,
                    title = "عکس کالا",
                    subtitle = "دوربین یا گالری",
                    accent = MaterialTheme.colorScheme.primary,
                    onAccent = MaterialTheme.colorScheme.onPrimaryContainer,
                    badge = if (fusedImageUri != null) "انتخاب شد ✓" else null,
                    onClick = { showImageOptions = true },
                    modifier = Modifier.weight(1f)
                )
            }

            if (fusedBarcode != null || fusedImageUri != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    fusedBarcode?.let { code ->
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = { Text("بارکد: $code") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "حذف بارکد",
                                    modifier = Modifier.clickable { fusedBarcode = null }.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                    if (fusedImageUri != null) {
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = { Text("عکس") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "حذف عکس",
                                    modifier = Modifier.clickable { fusedImageUri = null }.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
                Text(
                    text = "حالت ترکیبی فعال است — همه ورودی‌ها با هم تلفیق می‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "بدون حساب کاربری • بدون ردیابی • داده‌ها فقط روی گوشی شما",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            title = { Text("افزودن عکس کالا") },
            text = {
                Column {
                    TextButton(
                        onClick = { showImageOptions = false; launchCamera() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("گرفتن عکس با دوربین")
                    }
                    TextButton(
                        onClick = { showImageOptions = false; galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("انتخاب از گالری")
                    }
                    if (fusedImageUri == null) {
                        TextButton(
                            onClick = { showImageOptions = false; onOpenImageSearch() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جست‌وجو با عکس")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageOptions = false }) { Text("انصراف") }
            }
        )
    }
}
