package ir.arzanyab.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import ir.arzanyab.app.domain.engine.BarcodeResolver
import ir.arzanyab.app.data.resolver.BarcodeProductResolver
import ir.arzanyab.app.scanner.BarcodeAnalyzer
import ir.arzanyab.app.ui.components.GradientButton
import ir.arzanyab.app.ui.components.SoftCard
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerScreen(
    onProductResolved: (barcode: String) -> Unit,
    onSearchBarcode: (barcode: String) -> Unit = {},
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var scannedValue by remember { mutableStateOf<String?>(null) }
    var resolvedName by remember { mutableStateOf<String?>(null) }
    var resolvingName by remember { mutableStateOf(false) }
    val resolver = remember { BarcodeResolver() }
    val productResolver = remember { BarcodeProductResolver() }

    // بعد از اسکن، نام کالا را از بارکد پیدا کن و نشان بده
    androidx.compose.runtime.LaunchedEffect(scannedValue) {
        val code = scannedValue ?: return@LaunchedEffect
        resolvingName = true
        resolvedName = try {
            kotlinx.coroutines.withTimeoutOrNull(8000) { productResolver.resolveProductName(code) }
        } catch (_: Throwable) { null }
        resolvingName = false
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(120)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("اسکن بارکد") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!cameraPermissionState.status.isGranted) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                Text("برای اسکن بارکد، دسترسی دوربین لازم است.")
            }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val analyzer = BarcodeAnalyzer { barcode ->
                            scope.launch {
                                if (scannedValue == null) {
                                    scannedValue = barcode
                                    vibrate()
                                }
                            }
                        }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer) }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (_: Exception) { }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // کادر راهنمای اسکن
            if (scannedValue == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(260.dp)
                        .border(3.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                )
                Text(
                    text = "بارکد را داخل کادر بگیرید",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(top = 300.dp)
                )
            }

            // کارت تأیید مقدار خوانده‌شده
            val code = scannedValue
            if (code != null) {
                SoftCard(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "بارکد خوانده شد ✓",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        SelectionContainer {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        when {
                            resolvingName -> Text(
                                text = "در حال شناسایی کالا…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            resolvedName != null -> Text(
                                text = "کالا: $resolvedName",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        GradientButton(
                            text = "شروع جستجو",
                            icon = Icons.Default.Search,
                            onClick = { onSearchBarcode(code) }
                        )
                        OutlinedButton(
                            onClick = { scannedValue = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("اسکن مجدد")
                        }
                        TextButton(onClick = { onProductResolved(code) }) {
                            Text("افزودن به جست‌وجوی ترکیبی در خانه")
                        }
                    }
                }
            }
        }
    }
}
