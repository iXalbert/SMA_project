package com.example.interviewtrainer.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: InterviewViewModel,
    onStartInterview: () -> Unit
) {
    var domain by remember { mutableStateOf("IT / Software") }
    var role by remember { mutableStateOf("Android Developer") }
    var level by remember { mutableStateOf("Junior") }
    var language by remember { mutableStateOf("Română") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Configurare Interviu SMA") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Domeniu") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = role,
                onValueChange = { role = it },
                label = { Text("Rol / Poziție") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = level,
                onValueChange = { level = it },
                label = { Text("Nivel de experiență (ex: Junior, Mid)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("Limbă interviu") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.updateConfig(domain, role, level, language)
                    viewModel.startInterview()
                    onStartInterview()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Începe Interviul")
            }
        }
    }
}