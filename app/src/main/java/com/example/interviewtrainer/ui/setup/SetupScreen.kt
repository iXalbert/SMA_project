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
    val domains = remember { viewModel.repository.getAvailableDomains() }
    var selectedDomain by remember { mutableStateOf(domains.first()) }

    var availableRoles by remember(selectedDomain) {
        mutableStateOf(viewModel.repository.getRolesForDomain(selectedDomain))
    }
    var selectedRole by remember(selectedDomain) {
        mutableStateOf(availableRoles.first())
    }

    val levels = listOf("Junior", "Mid-Level", "Senior")
    var selectedLevel by remember { mutableStateOf(levels.first()) }

    val languages = listOf("Română", "Engleză")
    var selectedLanguage by remember { mutableStateOf(languages.first()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Configurare Interviu (SMA)") })
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
            Text(
                text = "Alege parametrii interviului",
                style = MaterialTheme.typography.titleMedium
            )

            // Dropdown Domeniu
            DropdownSelector(
                label = "Domeniu",
                options = domains,
                selectedOption = selectedDomain,
                onOptionSelected = {
                    selectedDomain = it
                    availableRoles = viewModel.repository.getRolesForDomain(it)
                    selectedRole = availableRoles.first()
                }
            )

            // Dropdown Rol
            DropdownSelector(
                label = "Rol / Poziție",
                options = availableRoles,
                selectedOption = selectedRole,
                onOptionSelected = { selectedRole = it }
            )

            // Dropdown Nivel
            DropdownSelector(
                label = "Nivel de experiență",
                options = levels,
                selectedOption = selectedLevel,
                onOptionSelected = { selectedLevel = it }
            )

            // Dropdown Limbă
            DropdownSelector(
                label = "Limbă",
                options = languages,
                selectedOption = selectedLanguage,
                onOptionSelected = { selectedLanguage = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.updateConfig(selectedDomain, selectedRole, selectedLevel, selectedLanguage)
                    viewModel.startInterview()
                    onStartInterview()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Începe Interviul Evaluat")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selection ->
                DropdownMenuItem(
                    text = { Text(selection) },
                    onClick = {
                        onOptionSelected(selection)
                        expanded = false
                    }
                )
            }
        }
    }
}