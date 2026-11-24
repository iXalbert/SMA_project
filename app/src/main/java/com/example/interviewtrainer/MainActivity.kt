package com.example.interviewtrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.interviewtrainer.ui.bank.QuestionBankScreen
import com.example.interviewtrainer.ui.chat.ChatScreen
import com.example.interviewtrainer.ui.chat.VideoCallScreen
import com.example.interviewtrainer.ui.feedback.FeedbackScreen
import com.example.interviewtrainer.ui.history.HistoryScreen
import com.example.interviewtrainer.ui.setup.SetupScreen
import com.example.interviewtrainer.ui.theme.AIInterviewTrainerTheme
import com.example.interviewtrainer.viewmodel.InterviewViewModel

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: InterviewViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isVideoCallMode by viewModel.isVideoCallMode.collectAsState()
            val savedInterviews by viewModel.savedInterviews.collectAsState()

            AIInterviewTrainerTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val navItems = listOf(
                    BottomNavItem("setup", "Antrenament", Icons.Default.PlayArrow),
                    BottomNavItem("bank", "Bancă", Icons.Default.MenuBook),
                    BottomNavItem("history", "Istoric", Icons.Default.History)
                )

                val showBottomBar = currentRoute in listOf("setup", "bank", "history")

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                navItems.forEach { item ->
                                    NavigationBarItem(
                                        selected = currentRoute == item.route,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "setup",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("setup") {
                            SetupScreen(
                                viewModel = viewModel,
                                onStartInterview = {
                                    navController.navigate("chat")
                                }
                            )
                        }

                        composable("bank") {
                            QuestionBankScreen(viewModel = viewModel)
                        }

                        composable("history") {
                            HistoryScreen(
                                interviews = savedInterviews,
                                onAddInterview = { viewModel.addInterviewRecord(it) },
                                onUpdateInterview = { viewModel.updateInterviewRecord(it) },
                                onDeleteInterview = { viewModel.deleteInterviewRecord(it) },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("chat") {
                            if (isVideoCallMode) {
                                VideoCallScreen(
                                    viewModel = viewModel,
                                    onFinishSession = {
                                        navController.navigate("feedback") {
                                            popUpTo("setup")
                                        }
                                    }
                                )
                            } else {
                                ChatScreen(
                                    viewModel = viewModel,
                                    onFinishSession = {
                                        navController.navigate("feedback") {
                                            popUpTo("setup")
                                        }
                                    }
                                )
                            }
                        }

                        composable("feedback") {
                            FeedbackScreen(
                                viewModel = viewModel,
                                onBackToSetup = {
                                    navController.navigate("setup") {
                                        popUpTo("setup") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}