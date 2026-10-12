package ir.arzanyab.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ir.arzanyab.app.data.local.PricePointEntity
import ir.arzanyab.app.util.PersianFormatter

@Composable
fun PriceHistoryChart(
    pricePoints: List<PricePointEntity>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
) {
    if (pricePoints.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "داده‌های کافی برای رسم نمودار موجود نیست.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val prices = pricePoints.map { it.price }
    val minPrice = prices.minOrNull() ?: 0L
    val maxPrice = prices.maxOrNull() ?: 1L
    val priceRange = if (maxPrice - minPrice == 0L) 1L else maxPrice - minPrice

    Box(
        modifier = modifier.fillMaxWidth().height(200.dp)
            .padding(top = 16.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val horizontalLines = 4
            for (i in 0..horizontalLines) {
                val y = height * (i.toFloat() / horizontalLines)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val points = pricePoints.mapIndexed { index, item ->
                val x = (index.toFloat() / (pricePoints.size - 1)) * width
                val normalizedPrice = (item.price - minPrice).toFloat() / priceRange
                val y = height - (normalizedPrice * height)
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cx = (prev.x + curr.x) / 2
                    cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                }
            }

            drawPath(path = path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            points.forEach { point ->
                drawCircle(color = lineColor, radius = 4.dp.toPx(), center = point)
            }
        }

        Text(
            text = "بیشترین: ${PersianFormatter.formatPrice(maxPrice)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        Text(
            text = "کمترین: ${PersianFormatter.formatPrice(minPrice)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
