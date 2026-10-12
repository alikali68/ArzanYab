package ir.arzanyab.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.arzanyab.app.R
import ir.arzanyab.app.data.adapter.DigikalaAdapter
import ir.arzanyab.app.data.adapter.ProductQuery
import ir.arzanyab.app.data.adapter.TorobAdapter
import ir.arzanyab.app.data.local.AppDatabase
import ir.arzanyab.app.domain.normalizer.PriceParser
import ir.arzanyab.app.util.PersianFormatter
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** واکشی دوره‌ای قیمت کالاهای دنبال‌شده و هشدار کاهش قیمت */
class PriceCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val UNIQUE_WORK_NAME = "arzanyab_price_check"
        private const val CHANNEL_ID = "price_alerts"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag(UNIQUE_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val dao = AppDatabase.getInstance(applicationContext).productDao()
            val tracked = dao.getAllTrackedProducts().first()
            if (tracked.isEmpty()) return Result.success()

            val digikala = DigikalaAdapter()
            val torob = TorobAdapter()

            for (product in tracked) {
                val query = ProductQuery(queryText = product.title)
                val listings = digikala.search(query) + torob.search(query)
                val freshPrices = listings.mapNotNull { PriceParser.parse(it.priceText).priceInToman }

                if (freshPrices.isNotEmpty()) {
                    val bestPrice = freshPrices.minOrNull() ?: continue
                    dao.recordNewPrice(product.id, bestPrice, 80)

                    val threshold = product.targetThresholdPrice
                    if (threshold != null && bestPrice <= threshold && bestPrice < product.latestPrice) {
                        showPriceDropNotification(product.title, bestPrice)
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    private fun showPriceDropNotification(title: String, price: Long) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "هشدارهای قیمت",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("کاهش قیمت!")
            .setContentText("$title به ${PersianFormatter.formatPrice(price)} رسید")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(title.hashCode(), notification)
    }
}
