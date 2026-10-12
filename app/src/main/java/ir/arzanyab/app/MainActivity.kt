package ir.arzanyab.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.arzanyab.app.domain.engine.MultiModalSearchRequest
import ir.arzanyab.app.ui.navigation.Screen
import ir.arzanyab.app.ui.screens.BarcodeScannerScreen
import ir.arzanyab.app.ui.screens.FusedResultsScreen
import ir.arzanyab.app.ui.screens.HomeScreen
import ir.arzanyab.app.ui.screens.ImageSearchScreen
import ir.arzanyab.app.ui.screens.PrivacyDashboardScreen
import ir.arzanyab.app.ui.screens.ProductDetailScreen
import ir.arzanyab.app.ui.screens.SearchResultsScreen
import ir.arzanyab.app.ui.screens.TrackedListScreen
import ir.arzanyab.app.ui.theme.ArzanYabTheme
import ir.arzanyab.app.work.PriceCheckWorker

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* نتیجه مهم نیست؛ بدون اجازه فقط نوتیفیکیشن نمی‌آید */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // اجازه نوتیفیکیشن برای هشدار کاهش قیمت (اندروید ۱۳+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // زمان‌بندی واکشی دوره‌ای قیمت‌ها
        PriceCheckWorker.schedule(this)

        setContent {
            ArzanYabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        // خانه
                        composable(Screen.Home.route) { backStackEntry ->
                            val scannedBarcode =
                                backStackEntry.savedStateHandle.get<String>("scanned_barcode")
                            // یک‌بار مصرف: بعد از خواندن پاک شود
                            if (scannedBarcode != null) {
                                backStackEntry.savedStateHandle.remove<String>("scanned_barcode")
                            }

                            HomeScreen(
                                scannedBarcode = scannedBarcode,
                                onOpenScanner = { navController.navigate(Screen.Scanner.route) },
                                onOpenImageSearch = { navController.navigate(Screen.ImageSearch.route) },
                                onOpenTracked = { navController.navigate(Screen.Tracked.route) },
                                onOpenPrivacy = { navController.navigate(Screen.Privacy.route) },
                                onExecuteFusedSearch = { text, barcode, imageUri ->
                                    navController.currentBackStackEntry?.savedStateHandle?.set(
                                        "search_request",
                                        MultiModalSearchRequest(text, barcode, imageUri)
                                    )
                                    navController.navigate(Screen.FusedResults.route)
                                }
                            )
                        }

                        // نتایج جست‌وجوی متنی ساده
                        composable(
                            route = Screen.Results.route,
                            arguments = listOf(navArgument("query") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val query = backStackEntry.arguments?.getString("query") ?: ""
                            SearchResultsScreen(
                                query = query,
                                onBackClick = { navController.popBackStack() },
                                onOpenDetail = { id ->
                                    navController.navigate(Screen.Detail.createRoute(id))
                                }
                            )
                        }

                        // نتایج جست‌وجوی چندحالته (تلفیق RRF)
                        composable(Screen.FusedResults.route) {
                            val request = navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.get<MultiModalSearchRequest>("search_request")
                                ?: MultiModalSearchRequest()
                            FusedResultsScreen(
                                request = request,
                                onBackClick = { navController.popBackStack() },
                                onOpenDetail = { id ->
                                    navController.navigate(Screen.Detail.createRoute(id))
                                }
                            )
                        }

                        // اسکنر بارکد — کد را به خانه برمی‌گرداند (ترکیب با متن/عکس)
                        composable(Screen.Scanner.route) {
                            BarcodeScannerScreen(
                                onProductResolved = { barcode ->
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("scanned_barcode", barcode)
                                    navController.popBackStack()
                                },
                                onSearchBarcode = { barcode ->
                                    navController.currentBackStackEntry?.savedStateHandle?.set(
                                        "search_request",
                                        MultiModalSearchRequest(barcode = barcode)
                                    )
                                    navController.navigate(Screen.FusedResults.route)
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // جست‌وجوی عکسی مستقل
                        composable(Screen.ImageSearch.route) {
                            ImageSearchScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // فهرست دنبال‌شده‌ها
                        composable(Screen.Tracked.route) {
                            TrackedListScreen(
                                onBackClick = { navController.popBackStack() },
                                onOpenDetail = { id ->
                                    navController.navigate(Screen.Detail.createRoute(id))
                                }
                            )
                        }

                        // جزئیات کالا + تاریخچه قیمت
                        composable(
                            route = Screen.Detail.route,
                            arguments = listOf(navArgument("productId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId") ?: ""
                            ProductDetailScreen(
                                productId = productId,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // داشبورد حریم خصوصی
                        composable(Screen.Privacy.route) {
                            PrivacyDashboardScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
