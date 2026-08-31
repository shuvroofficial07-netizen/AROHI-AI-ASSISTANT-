package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AboutSupportScreen
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.AssistantChatScreen
import com.example.ui.screens.BrainScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DeviceDashboardScreen
import com.example.ui.screens.FirstRunSetupScreen
import com.example.ui.screens.GeminiControlCenterScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryRoutinesScreen
import com.example.ui.screens.NotificationCenterScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartTasksScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SystemHealthScreen
import com.example.ui.screens.VisionScreen
import com.example.ui.theme.ArohiBlack
import com.example.ui.theme.ArohiDarkSurface
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.ArohiViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Splash : Screen("splash", "Splash", Icons.Filled.Home, Icons.Outlined.Home)
    object Home : Screen("home", "HOME", Icons.Filled.Home, Icons.Outlined.Home)
    object Chat : Screen("chat", "ASSISTANT", Icons.Filled.Chat, Icons.Outlined.Chat)
    object Dashboard : Screen("dashboard", "DEVICE", Icons.Filled.PhoneAndroid, Icons.Outlined.PhoneAndroid)
    object Notifications : Screen("notifications", "INBOX", Icons.Filled.Notifications, Icons.Outlined.Notifications)
    object Memories : Screen("memories", "MEMORY", Icons.Filled.Bookmark, Icons.Outlined.Bookmark)
    object Settings : Screen("settings", "SETTINGS", Icons.Filled.Settings, Icons.Outlined.Settings)
    object Tasks : Screen("tasks", "Tasks", Icons.Filled.Checklist, Icons.Outlined.Checklist)
    object Vision : Screen("vision", "Vision", Icons.Filled.RemoveRedEye, Icons.Outlined.RemoveRedEye)
    object Diagnostics : Screen("diagnostics", "Health", Icons.Filled.PhoneAndroid, Icons.Outlined.PhoneAndroid)
    object FirstRunSetup : Screen("first_run_setup", "Setup", Icons.Filled.Home, Icons.Outlined.Home)
    object Permissions : Screen("permissions", "Permissions", Icons.Filled.Settings, Icons.Outlined.Settings)
    object Apps : Screen("apps", "Apps", Icons.Filled.Checklist, Icons.Outlined.Checklist)
    object Calls : Screen("calls", "Calls", Icons.Filled.Chat, Icons.Outlined.Chat)
    object Contacts : Screen("contacts", "Contacts", Icons.Filled.Chat, Icons.Outlined.Chat)
    object Brain : Screen("brain", "Brain", Icons.Filled.Home, Icons.Outlined.Home)
    object About : Screen("about", "About", Icons.Filled.Settings, Icons.Outlined.Settings)
    object GeminiControl : Screen("gemini_control", "Gemini AI", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: ArohiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                ArohiMainApp(
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun ArohiMainApp(
    viewModel: ArohiViewModel,
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationItems = listOf(
        Screen.Home,
        Screen.Chat,
        Screen.Dashboard,
        Screen.Notifications,
        Screen.Memories,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ArohiBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute != Screen.Splash.route && currentRoute != Screen.FirstRunSetup.route) {
                // Sleek Floating Glass Dock Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x18FFFFFF),
                                        Color(0x0EFFFFFF),
                                        ArohiDarkSurface.copy(alpha = 0.95f)
                                    )
                                )
                            )
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(26.dp))
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navigationItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            val interactionSource = remember { MutableInteractionSource() }

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("nav_${screen.route}"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .drawBehind {
                                                    drawCircle(
                                                        color = CyanPrimary.copy(alpha = 0.25f),
                                                        radius = size.minDimension * 0.9f
                                                    )
                                                }
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        tint = if (isSelected) CyanPrimary else TextMuted,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Text(
                                    text = screen.title,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 0.5.sp,
                                    color = if (isSelected) CyanPrimary else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val firstLaunch by viewModel.firstLaunchFlow.collectAsState()
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    viewModel = viewModel,
                    onFinish = {
                        // First launch → guided setup wizard; otherwise straight home
                        if (firstLaunch) {
                            navController.navigate(Screen.FirstRunSetup.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(Screen.FirstRunSetup.route) {
                FirstRunSetupScreen(
                    viewModel = viewModel,
                    onDone = {
                        viewModel.markSetupComplete()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.FirstRunSetup.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                    onNavigateToVision = { navController.navigate(Screen.Vision.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToSmartTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToRoutines = { navController.navigate(Screen.Memories.route) },
                    onNavigateToMemory = { navController.navigate(Screen.Memories.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) },
                    onNavigateToApps = { navController.navigate(Screen.Apps.route) },
                    onNavigateToCalls = { navController.navigate(Screen.Calls.route) },
                    onNavigateToBrain = { navController.navigate(Screen.Brain.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
                )
            }
            composable(Screen.Brain.route) {
                BrainScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Apps.route) {
                AppsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Calls.route) {
                CallsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToContacts = { navController.navigate(Screen.Contacts.route) }
                )
            }
            composable(Screen.Contacts.route) {
                ContactsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Permissions.route) {
                PermissionsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.About.route) {
                AboutSupportScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.GeminiControl.route) {
                GeminiControlCenterScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Tasks.route) {
                SmartTasksScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Dashboard.route) {
                DeviceDashboardScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Notifications.route) {
                NotificationCenterScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Vision.route) {
                VisionScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }
            composable(Screen.Chat.route) {
                AssistantChatScreen(viewModel = viewModel)
            }
            composable(Screen.Memories.route) {
                MemoryRoutinesScreen(viewModel = viewModel)
            }
            composable(Screen.Diagnostics.route) {
                SystemHealthScreen(
                    viewModel = viewModel,
                    onNavigateToVision = { navController.navigate(Screen.Vision.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                    onNavigateToGemini = { navController.navigate(Screen.GeminiControl.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToBrain = { navController.navigate(Screen.Brain.route) },
                    onNavigateToApps = { navController.navigate(Screen.Apps.route) },
                    onNavigateToCalls = { navController.navigate(Screen.Calls.route) }
                )
            }
        }
    }
}

