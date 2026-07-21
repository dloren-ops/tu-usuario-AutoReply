package com.autoreply.bot.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.autoreply.bot.AutoReplyApp
import com.autoreply.bot.BuildConfig
import com.autoreply.bot.admin.AdminTools
import com.autoreply.bot.service.NotificationAccess
import com.autoreply.bot.ui.apps.AppSelectionScreen
import com.autoreply.bot.ui.apps.AppSelectionViewModel
import com.autoreply.bot.ui.home.HomeScreen
import com.autoreply.bot.ui.home.HomeViewModel
import com.autoreply.bot.ui.license.LicenseViewModel
import com.autoreply.bot.ui.logs.LogsScreen
import com.autoreply.bot.ui.logs.LogsViewModel
import com.autoreply.bot.ui.rules.RulesScreen
import com.autoreply.bot.ui.rules.RulesViewModel
import com.autoreply.bot.ui.settings.SettingsScreen
import com.autoreply.bot.ui.settings.SettingsViewModel
import com.autoreply.bot.ui.update.UpdateViewModel
import com.autoreply.bot.ui.theme.AutoReplyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AutoReplyApp).container
        val factory = AppViewModelFactory(container)

        setContent {
            AutoReplyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(factory)
                }
            }
        }
    }
}

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Inicio", Icons.Default.Home),
    RULES("rules", "Reglas", Icons.AutoMirrored.Filled.Chat),
    SETTINGS("settings", "Ajustes", Icons.Default.Settings),
    LOGS("logs", "Registro", Icons.AutoMirrored.Filled.List),
    ADMIN("admin", "Admin", Icons.Default.AdminPanelSettings)
}

@Composable
private fun AppRoot(factory: AppViewModelFactory) {
    val navController = rememberNavController()

    // Estado del permiso de acceso a notificaciones, re-evaluado al volver a la app.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember { mutableStateOf(NotificationAccess.isEnabled(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = NotificationAccess.isEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                val visibleDests = Dest.entries.filter { it != Dest.ADMIN || BuildConfig.LICENSE_ADMIN_TOOLS }
                visibleDests.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.HOME.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.HOME.route) {
                val vm: HomeViewModel = viewModel(factory = factory)
                val updateVm: UpdateViewModel = viewModel(factory = factory)
                val licenseVm: LicenseViewModel = viewModel(factory = factory)
                HomeScreen(
                    viewModel = vm,
                    updateViewModel = updateVm,
                    licenseViewModel = licenseVm,
                    permissionGranted = permissionGranted,
                    onOpenPermissionSettings = {
                        context.startActivity(NotificationAccess.settingsIntent())
                    }
                )
            }
            composable(Dest.RULES.route) {
                val vm: RulesViewModel = viewModel(factory = factory)
                RulesScreen(viewModel = vm)
            }
            composable(Dest.SETTINGS.route) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = vm,
                    onOpenAppSelection = { navController.navigate("app_selection") }
                )
            }
            composable("app_selection") {
                val vm: AppSelectionViewModel = viewModel(factory = factory)
                AppSelectionScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Dest.LOGS.route) {
                val vm: LogsViewModel = viewModel(factory = factory)
                LogsScreen(viewModel = vm)
            }
            if (BuildConfig.LICENSE_ADMIN_TOOLS) {
                composable(Dest.ADMIN.route) {
                    AdminTools.Screen()
                }
            }
        }
    }
}
