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

private fun colorFor(method: String): Color = when (method.lowercase()) {
    "bkash" -> Color(0xFFE2136E)
    "rocket" -> Color(0xFF8E24AA)
    "nagad" -> Color(0xFFEE7600)
    else -> Color(0xFF616161)
}

@Composable
fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = colorFor(method),
        contentColor = Color.White,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = method.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Masks all but the last 4 digits, e.g. "01812345678" -> "•••••••5678". */
fun maskPhone(phone: String?): String {
    if (phone.isNullOrBlank()) return "—"
    if (phone.length <= 4) return phone
    return "•".repeat(phone.length - 4) + phone.takeLast(4)
}
