package ir.arzanyab.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Results : Screen("results/{query}")
    data object FusedResults : Screen("results_fused")
    data object Scanner : Screen("scanner")
    data object ImageSearch : Screen("image_search")
    data object Tracked : Screen("tracked")
    data object Privacy : Screen("privacy")
    data object Detail : Screen("detail/{productId}") {
        fun createRoute(productId: String) = "detail/$productId"
    }
}
