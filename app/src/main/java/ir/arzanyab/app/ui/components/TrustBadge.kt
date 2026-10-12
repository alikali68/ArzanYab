package ir.arzanyab.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ir.arzanyab.app.ui.theme.TrustHigh
import ir.arzanyab.app.ui.theme.TrustLow
import ir.arzanyab.app.ui.theme.TrustMedium
import ir.arzanyab.app.util.PersianFormatter

/** بج فشرده اعتماد: نقطه رنگی + درصد، هماهنگ با چیپ منبع */
@Composable
fun TrustBadge(score: Int, modifier: Modifier = Modifier) {
    val badgeColor = when {
        score >= 80 -> TrustHigh
        score >= 60 -> TrustMedium
        else -> TrustLow
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${PersianFormatter.toPersianDigits(score.toLong())}٪",
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor
        )
    }
}
