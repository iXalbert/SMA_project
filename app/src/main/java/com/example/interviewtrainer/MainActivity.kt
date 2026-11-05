package com.example.interviewtrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.interviewtrainer.ui.chat.ChatScreen
import com.example.interviewtrainer.ui.feedback.FeedbackScreen
import com.example.interviewtrainer.ui.history.HistoryScreen
import com.example.interviewtrainer.ui.setup.SetupScreen
import com.example.interviewtrainer.ui.theme.AIInterviewTrainerTheme
import com.example.interviewtrainer.viewmodel.InterviewViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: InterviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            AIInterviewTrainerTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: InterviewViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "setup") {
        composable("setup") {
            SetupScreen(
                viewModel = viewModel,
                onStartInterview = {
                    navController.navigate("chat")
                },
                onOpenHistory = {
                    navController.navigate("history")
                }
            )
        }
        composable("chat") {
            ChatScreen(
                viewModel = viewModel,
                onFinishSession = {
                    navController.navigate("feedback")
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
            val savedInterviews by viewModel.savedInterviews.collectAsState()
            HistoryScreen(
                interviews = savedInterviews,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}