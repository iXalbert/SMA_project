package com.example.interviewtrainer.ui.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: InterviewViewModel,
    onRestart: () -> Unit
) {
    val report by viewModel.report.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Raport Final Interviu") })
        }
    ) { padding ->
        report?.let { rep ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Scor Obținut: ${rep.score} / 100",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = rep.summary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                item {
                    Text(text = "Puncte Tare:", style = MaterialTheme.typography.titleLarge)
                }
                items(rep.strengths) { strength ->
                    Text(text = "• $strength", style = MaterialTheme.typography.bodyMedium)
                }

                item {
                    Text(text = "Aspecte de Îmbunătățit:", style = MaterialTheme.typography.titleLarge)
                }
                items(rep.improvements) { imp ->
                    Text(text = "• $imp", style = MaterialTheme.typography.bodyMedium)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRestart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Începe un nou interviu")
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}