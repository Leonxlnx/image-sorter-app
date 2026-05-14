package com.leonxlnx.imagesorter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PhotoCameraBack
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.leonxlnx.imagesorter.R
import com.leonxlnx.imagesorter.ui.folders.FoldersScreen
import com.leonxlnx.imagesorter.ui.permission.PermissionGate
import com.leonxlnx.imagesorter.ui.settings.SettingsScreen
import com.leonxlnx.imagesorter.ui.swipe.SwipeScreen

private object Routes {
    const val Swipe = "swipe"
    const val Folders = "folders"
    const val Settings = "settings"
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Triple(Routes.Swipe, R.string.nav_swipe, Icons.Outlined.PhotoCameraBack),
                    Triple(Routes.Folders, R.string.nav_folders, Icons.Outlined.Folder),
                    Triple(Routes.Settings, R.string.nav_settings, Icons.Outlined.Tune),
                )
                items.forEach { (route, labelRes, icon) ->
                    NavigationBarItem(
                        selected = backEntry?.destination?.hierarchy?.any { it.route == route } == true || currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(Routes.Swipe) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            NavHost(navController = navController, startDestination = Routes.Swipe) {
                composable(Routes.Swipe) {
                    PermissionGate {
                        SwipeScreen()
                    }
                }
                composable(Routes.Folders) {
                    FoldersScreen()
                }
                composable(Routes.Settings) {
                    SettingsScreen()
                }
            }
        }
    }
}
