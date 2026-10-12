package ir.arzanyab.app.data.adapter

import android.content.Context
import org.json.JSONObject
import java.io.File

data class SourceEndpointConfig(
    val searchUrlTemplate: String?,
    val itemSelector: String?,
    val titleSelector: String?,
    val priceSelector: String?,
    val linkSelector: String?,
    val imageSelector: String?
)

/** خواندن کانفیگ تأییدشده آداپتورها (verified_adapters.json) با fallback به مقادیر پیش‌فرض کد */
object AdapterConfigLoader {
    private const val FILE_NAME = "verified_adapters.json"
    private var cachedConfig: Map<String, SourceEndpointConfig>? = null

    @Synchronized
    fun getConfigForSource(context: Context, sourceId: String): SourceEndpointConfig? {
        if (cachedConfig == null) loadConfigFromFile(context)
        return cachedConfig?.get(sourceId)
    }

    @Synchronized
    fun invalidateCache() {
        cachedConfig = null
    }

    private fun loadConfigFromFile(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            cachedConfig = emptyMap()
            return
        }
        try {
            val root = JSONObject(file.readText())
            val map = mutableMapOf<String, SourceEndpointConfig>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.optJSONObject(key) ?: continue
                map[key] = SourceEndpointConfig(
                    searchUrlTemplate = obj.optString("searchUrlTemplate", null),
                    itemSelector = obj.optString("itemSelector", null),
                    titleSelector = obj.optString("titleSelector", null),
                    priceSelector = obj.optString("priceSelector", null),
                    linkSelector = obj.optString("linkSelector", null),
                    imageSelector = obj.optString("imageSelector", null)
                )
            }
            cachedConfig = map
        } catch (_: Exception) {
            cachedConfig = emptyMap()
        }
    }
}
