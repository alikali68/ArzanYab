package ir.arzanyab.app.data.adapter

data class ProductQuery(
    val queryText: String,
    val barcode: String? = null,
    val page: Int = 1
)

data class RawListing(
    val title: String,
    val priceText: String,
    val url: String,
    val imageUrl: String?,
    val sellerName: String?,
    val inStockHint: Boolean,
    val sourceId: String
)

data class ListingDetail(
    val title: String,
    val description: String,
    val attributes: Map<String, String>,
    val additionalImages: List<String>
)

interface SourceAdapter {
    val sourceId: String
    val displayName: String
    suspend fun search(query: ProductQuery): List<RawListing>
    suspend fun fetchDetail(url: String): ListingDetail
}
