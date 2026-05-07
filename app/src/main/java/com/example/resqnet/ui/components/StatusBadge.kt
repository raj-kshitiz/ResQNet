package com.example.resqnet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.ui.theme.ResQNetTheme

@Composable
fun StatusBadge(
    status: SosStatus,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val (bgColor, textColor) = when (status) {
        SosStatus.PENDING     -> cs.errorContainer     to cs.onErrorContainer
        SosStatus.NOTIFIED    -> cs.tertiaryContainer  to cs.onTertiaryContainer
        SosStatus.ACCEPTED    -> cs.secondaryContainer to cs.onSecondaryContainer
        SosStatus.IN_PROGRESS -> cs.secondaryContainer to cs.onSecondaryContainer
        SosStatus.RESOLVED    -> cs.tertiaryContainer  to cs.onTertiaryContainer
        SosStatus.CANCELLED   -> cs.surfaceVariant     to cs.onSurfaceVariant
        SosStatus.EXPIRED     -> cs.surfaceVariant     to cs.onSurfaceVariant
    }

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

@Preview(showBackground = true)
@Composable
private fun StatusBadgePreview() {
    ResQNetTheme {
        StatusBadge(SosStatus.PENDING, modifier = Modifier.padding(8.dp))
    }
}
