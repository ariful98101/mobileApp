package com.mfsrelay.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mfsrelay.app.service.SmsRelayForegroundService
import com.mfsrelay.app.ui.main.MainScreen
import com.mfsrelay.app.ui.main.MainViewModel
import com.mfsrelay.app.ui.permissions.PermissionScreen
import com.mfsrelay.app.ui.settings.SettingsScreen
import com.mfsrelay.app.ui.settings.SettingsViewModel
import com.mfsrelay.app.ui.theme.MfsRelayTheme

class MainActivity : ComponentActivity() {

    private val smsPermissions = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private fun hasSmsPermissions(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MfsRelayApplication

        setContent {
            var permissionsGranted by remember { mutableStateOf(hasSmsPermissions()) }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) {
                permissionsGranted = hasSmsPermissions()
            }

            LaunchedEffect(permissionsGranted) {
                if (permissionsGranted) {
                    SmsRelayForegroundService.start(this@MainActivity)
                }
            }

            MfsRelayTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        if (!permissionsGranted) {
                            PermissionScreen(
                                onRequestPermissions = { permissionLauncher.launch(smsPermissions) },
                                onContinueWithoutPermissions = {
                                    // Allow exploring Settings/UI even without granting yet;
                                    // the Settings screen also offers a re-request button.
                                    permissionsGranted = hasSmsPermissions()
                                }
                            )
                        } else {
                            val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.factory(app))
                            MainScreen(
                                viewModel = mainViewModel,
                                onOpenSettings = { navController.navigate("settings") }
                            )
                        }
                    }
                    composable("settings") {
                        val settingsViewModel: SettingsViewModel =
                            viewModel(factory = SettingsViewModel.factory(app))
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            smsPermissionsGranted = permissionsGranted,
                            onRequestSmsPermissions = { permissionLauncher.launch(smsPermissions) },
                            onRequestBatteryExemption = { requestBatteryOptimizationExemption() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
