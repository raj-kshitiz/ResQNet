package com.example.resqnet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resqnet.domain.model.EmergencyType
import com.example.resqnet.ui.theme.ResQNetTheme

@Composable
fun EmergencyTypeChip(
    type: EmergencyType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = type.icon(),
                    contentDescription = type.label,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier.padding(end = 4.dp)
    )
}

fun EmergencyType.icon(): ImageVector {
    return when (this) {
        EmergencyType.MEDICAL -> Icons.Default.HealthAndSafety
        EmergencyType.ACCIDENT -> Icons.Default.CarCrash
        EmergencyType.BLOOD_REQUEST -> Icons.Default.Bloodtype
        EmergencyType.SAFETY_ALERT -> Icons.Default.Shield
        EmergencyType.OTHER -> Icons.Default.MoreHoriz
    }
}

@Preview(showBackground = true)
@Composable
private fun EmergencyTypeChipPreview() {
    ResQNetTheme {
        Row {
            EmergencyTypeChip(EmergencyType.MEDICAL, selected = true, onClick = {})
            EmergencyTypeChip(EmergencyType.ACCIDENT, selected = false, onClick = {})
        }
    }
}
