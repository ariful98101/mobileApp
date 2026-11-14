package com.mfsrelay.app.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Rationale screen shown before/instead of requesting the dangerous RECEIVE_SMS/READ_SMS
 * permissions. Google Play scrutinizes SMS permissions heavily (this is one reason the
 * README recommends sideloading/internal distribution rather than a Play Store release) -
 * this screen exists so the user understands exactly why the app needs them before the
 * system dialog appears.
 */
@Composable
fun PermissionScreen(
    onRequestPermissions: () -> Unit,
    onContinueWithoutPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SMS access is required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "MFS SMS Relay reads incoming payment-confirmation SMS from bKash, Rocket " +
                "and Nagad that are sent to this phone's SIM, so it can forward the " +
                "transaction details (amount, TrxID, payer number) to your shop's order " +
                "verification system.\n\n" +
                "It does not read any other SMS content, does not send SMS, and only " +
                "forwards messages whose sender matches the patterns you configure in " +
                "Settings. Messages are sent only to the server URL you configure.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )
        Button(onClick = onRequestPermissions) {
            Text("Grant SMS permissions")
        }
        TextButton(
            onClick = onContinueWithoutPermissions,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Continue without granting (app will not capture SMS)")
        }
    }
}
