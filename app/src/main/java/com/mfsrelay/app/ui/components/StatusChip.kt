package com.mfsrelay.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mfsrelay.app.data.local.UploadStatus

private data class StatusStyle(val label: String, val emoji: String, val background: Color, val foreground: Color)

private fun styleFor(status: UploadStatus): StatusStyle = when (status) {
    UploadStatus.PENDING_UPLOAD -> StatusStyle("Pending", "⏳", Color(0xFFFFF3CD), Color(0xFF7A5B00))
    UploadStatus.SUBMITTED -> StatusStyle("Submitted", "✅", Color(0xFFD7F0DB), Color(0xFF1B5E20))
    UploadStatus.FAILED -> StatusStyle("Failed", "❌", Color(0xFFFBD7D7), Color(0xFFB00020))
    UploadStatus.MATCHED -> StatusStyle("Matched", "🔗", Color(0xFFD6E4FF), Color(0xFF1A3C8C))
    UploadStatus.UNMATCHED -> StatusStyle("Unmatched", "❓", Color(0xFFEDEDED), Color(0xFF424242))
}

@Composable
fun StatusChip(status: UploadStatus, modifier: Modifier = Modifier) {
    val style = styleFor(status)
    Surface(
        modifier = modifier,
        color = style.background,
        contentColor = style.foreground,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = "${style.emoji} ${style.label}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
