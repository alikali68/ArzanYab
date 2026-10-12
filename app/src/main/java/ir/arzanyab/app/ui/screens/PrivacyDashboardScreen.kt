package ir.arzanyab.app.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.arzanyab.app.data.adapter.AdapterConfigLoader
import ir.arzanyab.app.data.local.AppDatabase
import ir.arzanyab.app.util.PersianFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** داشبورد شفافیت: نمایش حجم داده محلی + امحای کامل با یک لمس */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dbSizeBytes by remember { mutableLongStateOf(0L) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isWiped by remember { mutableStateOf(false) }

    fun calculateStorage() {
        val dbFile = context.getDatabasePath("arzanyab_secure.db")
        dbSizeBytes = if (dbFile.exists()) dbFile.length() else 0L
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { calculateStorage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("داشبورد حریم خصوصی", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "تمامی داده‌ها — تاریخچه قیمت، علاقه‌مندی‌ها و کش — ۱۰۰٪ روی همین گوشی ذخیره می‌شود. هیچ سروری وجود ندارد.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "وضعیت حافظه محلی", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("حجم پایگاه داده رمزنگاری‌شده:")
                        Text(
                            text = "${PersianFormatter.toPersianDigits(dbSizeBytes / 1024)} کیلوبایت",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("کلید امنیتی:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = androidx.compose.ui.Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AES-256 در Keystore سخت‌افزاری", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("امحای کامل داده‌ها با یک لمس")
            }

            if (isWiped) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "تمامی داده‌های ذخیره‌شده و کلید رمزنگاری دیتابیس نابود شدند. (کلید مستر سخت‌افزاری در Keystore باقی می‌ماند.)",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "درباره سازنده", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "سازنده: alikali68",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CreatorLinkRow(
                        icon = Icons.Default.Code,
                        label = "گیت‌هاب",
                        value = "github.com/alikali68",
                        url = "https://github.com/alikali68",
                        context = context
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CreatorLinkRow(
                        icon = Icons.Default.AlternateEmail,
                        label = "ایکس (توییتر)",
                        value = "x.com/alikali68",
                        url = "https://x.com/alikali68",
                        context = context
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("پاک‌سازی کامل داده‌ها") },
            text = { Text("مطمئنید؟ کالاهای دنبال‌شده، سوابق قیمت و کش آداپتورها برای همیشه از روی دستگاه پاک می‌شوند.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            // ۱. بستن امن اتصال دیتابیس قبل از حذف فایل
                            AppDatabase.closeDatabaseInstance()

                            // ۲. حذف فیزیکی دیتابیس و فایل‌های ژورنال
                            val dbFile = context.getDatabasePath("arzanyab_secure.db")
                            if (dbFile.exists()) dbFile.delete()
                            for (suffix in listOf("-journal", "-wal", "-shm")) {
                                val f = File(dbFile.path + suffix)
                                if (f.exists()) f.delete()
                            }

                            // ۳. پاک‌سازی تنظیمات (شامل پس‌فریز رمزنگاری‌شده دیتابیس)
                            context.getSharedPreferences("arzanyab_secure_prefs", Context.MODE_PRIVATE)
                                .edit().clear().commit()

                            // ۴. تخلیه کش کانفیگ آداپتورها
                            AdapterConfigLoader.invalidateCache()

                            withContext(Dispatchers.Main) {
                                calculateStorage()
                                isWiped = true
                                showConfirmDialog = false
                            }
                        }
                    }
                ) {
                    Text("بله، پاک شود", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("انصراف") }
            }
        )
    }
}

/** ردیف لینک سازنده: با لمس، لینک در مرورگر باز می‌شود */
@Composable
private fun CreatorLinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    url: String,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                } catch (_: Exception) { }
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
