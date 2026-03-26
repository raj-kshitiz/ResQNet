package com.example.resqnet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.ui.theme.*

@Composable
fun StatusBadge(
    status: SosStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = status.badgeColors()

    Text(
        text = status.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun SosStatus.badgeColors(): Pair<Color, Color> {
    return when (this) {
        SosStatus.PENDING -> Red100 to Red900
        SosStatus.NOTIFIED -> Amber100 to Color(0xFF795548)
        SosStatus.ACCEPTED -> Blue100 to Blue900
        SosStatus.IN_PROGRESS -> Blue100 to Blue900
        SosStatus.RESOLVED -> Green100 to Green900
        SosStatus.CANCELLED -> Gray200 to Gray700
        SosStatus.EXPIRED -> Gray200 to Gray700
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgePreview() {
    ResQNetTheme {
        StatusBadge(SosStatus.PENDING, modifier = Modifier.padding(8.dp))
    }
}
