package com.mfsrelay.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mfsrelay.app.settings.MethodSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    smsPermissionsGranted: Boolean,
    onRequestSmsPermissions: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard(title = "Server connection") {
                    OutlinedTextField(
                        value = state.serverBaseUrl,
                        onValueChange = viewModel::updateServerBaseUrl,
                        label = { Text("Server base URL") },
                        placeholder = { Text("https://yourshop.example.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var apiKeyVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = viewModel::updateApiKey,
                        label = { Text("API key") },
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { apiKeyVisible = !apiKeyVisible },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(if (apiKeyVisible) "Hide API key" else "Show API key")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Device ID", style = MaterialTheme.typography.labelMedium)
                            Text(state.deviceId, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(state.deviceId)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy device id")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = viewModel::testConnection) { Text("Test connection") }

                    when (state.connectionTestState) {
                        ConnectionTestState.RUNNING -> CircularProgressIndicator(
                            modifier = Modifier.padding(top = 8.dp).height(20.dp)
                        )
                        ConnectionTestState.SUCCESS -> Text(
                            "✅ ${state.connectionTestMessage}",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        ConnectionTestState.FAILURE -> Text(
                            "❌ ${state.connectionTestMessage}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        ConnectionTestState.IDLE -> {}
                    }
                }
            }

            item {
                SectionCard(title = "SMS permissions") {
                    Text(
                        if (smsPermissionsGranted) "✅ SMS permissions granted"
                        else "❌ SMS permissions not granted - the app cannot capture payment SMS"
                    )
                    if (!smsPermissionsGranted) {
                        Button(
                            onClick = onRequestSmsPermissions,
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Grant SMS permissions") }
                    }
                }
            }

            item {
                SectionCard(title = "Battery optimization") {
                    Text("Exempting this app from battery optimization helps it keep listening for SMS reliably in the background.")
                    Button(
                        onClick = onRequestBatteryExemption,
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Request battery optimization exemption") }
                }
            }

            items(state.methods, key = { it.method }) { method ->
                MethodSettingsCard(method = method, onChange = viewModel::updateMethod)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun MethodSettingsCard(method: MethodSettings, onChange: (MethodSettings) -> Unit) {
    SectionCard(title = method.method.uppercase()) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = method.enabled,
                onCheckedChange = { onChange(method.copy(enabled = it)) }
            )
            Text("Enabled")
        }

        OutlinedTextField(
            value = method.senderPatterns,
            onValueChange = { onChange(method.copy(senderPatterns = it)) },
            label = { Text("Sender patterns (comma-separated)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Extraction regex (each needs exactly one capture group)", style = MaterialTheme.typography.labelMedium)

        OutlinedTextField(
            value = method.trxIdRegex,
            onValueChange = { onChange(method.copy(trxIdRegex = it)) },
            label = { Text("Transaction ID regex") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = method.amountRegex,
            onValueChange = { onChange(method.copy(amountRegex = it)) },
            label = { Text("Amount regex") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = method.customerPhoneRegex,
            onValueChange = { onChange(method.copy(customerPhoneRegex = it)) },
            label = { Text("Customer phone regex") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}
