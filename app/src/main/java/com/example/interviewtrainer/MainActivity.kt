package com.example.interviewtrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.interviewtrainer.ui.bank.QuestionBankScreen
import com.example.interviewtrainer.ui.chat.ChatScreen
import com.example.interviewtrainer.ui.feedback.FeedbackScreen
import com.example.interviewtrainer.ui.history.HistoryScreen
import com.example.interviewtrainer.ui.setup.SetupScreen
import com.example.interviewtrainer.ui.theme.AIInterviewTrainerTheme
import com.example.interviewtrainer.viewmodel.InterviewViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: InterviewViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val savedInterviews by viewModel.savedInterviews.collectAsState()

            AIInterviewTrainerTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val bottomBarRoutes = setOf("setup", "bank", "history")
                val showBottomBar = currentRoute in bottomBarRoutes

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                NavigationBarItem(
                                    selected = currentRoute == "setup",
                                    onClick = {
                                        navController.navigate("setup") {
                                            popUpTo("setup") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                                    label = { Text("Antrenament") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "bank",
                                    onClick = {
                                        navController.navigate("bank") {
                                            popUpTo("setup") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                                    label = { Text("Bancă") }
                                )
                                NavigationBarItem(
                                    selected = currentRoute == "history",
                                    onClick = {
                                        navController.navigate("history") {
                                            popUpTo("setup") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                                    label = { Text("Istoric") }
                                )
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
                                onStartInterview = { navController.navigate("chat") },
                                onOpenHistory = { navController.navigate("history") }
                            )
                        }
                        composable("bank") {
                            QuestionBankScreen(viewModel = viewModel)
                        }
                        composable("chat") {
                            ChatScreen(
                                viewModel = viewModel,
                                onFinishSession = {
                                    navController.navigate("feedback") {
                                        popUpTo("setup") { inclusive = false }
                                    }
                                }
                            )
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
                        composable("history") {
                            HistoryScreen(
                                interviews = savedInterviews,
                                onAddInterview = { viewModel.addInterviewRecord(it) },
                                onUpdateInterview = { viewModel.updateInterviewRecord(it) },
                                onDeleteInterview = { viewModel.deleteInterviewRecord(it) },
                                onBack = { navController.navigate("setup") }
                            )
                        }
                    }
                }
            }
        }
    }
}