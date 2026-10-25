package com.example.interviewtrainer.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.QuestionBankResponse
import com.example.interviewtrainer.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: InterviewViewModel,
    onStartInterview: () -> Unit,
    onOpenHistory: () -> Unit
) {
    // Înlocuiește cu URL-ul tău RAW din GitHub Gist
    val rawGistUrl = "https://gist.githubusercontent.com/iXalbert/d6f75eebf201f1efa3b108e514c74061/raw/questions.json"

    var questionBank by remember { mutableStateOf<QuestionBankResponse?>(null) }
    var isLoadingBank by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            questionBank = viewModel.repository.fetchQuestionBank(rawGistUrl)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingBank = false
        }
    }

    val domains = questionBank?.domains?.map { it.name } ?: emptyList()
    var selectedDomain by remember(domains) { mutableStateOf(domains.firstOrNull() ?: "IT & Software") }

    val currentDomainObj = questionBank?.domains?.find { it.name == selectedDomain }
    val specializations = currentDomainObj?.specializations?.map { it.name } ?: emptyList()
    var selectedSpecialization by remember(selectedDomain, specializations) {
        mutableStateOf(specializations.firstOrNull() ?: "")
    }

    val currentSpecObj = currentDomainObj?.specializations?.find { it.name == selectedSpecialization }
    val roles = currentSpecObj?.roles?.map { it.title } ?: emptyList()
    var selectedRole by remember(selectedSpecialization, roles) {
        mutableStateOf(roles.firstOrNull() ?: "")
    }

    val levels = listOf("Junior", "Mid-Level", "Senior")
    var selectedLevel by remember { mutableStateOf(levels.first()) }

    val languages = listOf("Română", "Engleză")
    var selectedLanguage by remember { mutableStateOf(languages.first()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Configurare Interviu (SMA)", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (isLoadingBank) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Se încarcă banca de întrebări din cloud...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Alege parametrii interviului",
                    style = MaterialTheme.typography.titleMedium
                )

                DropdownSelector(
                    label = "Domeniu",
                    options = domains,
                    selectedOption = selectedDomain,
                    onOptionSelected = { selectedDomain = it }
                )

                DropdownSelector(
                    label = "Specializare",
                    options = specializations,
                    selectedOption = selectedSpecialization,
                    onOptionSelected = { selectedSpecialization = it }
                )

                DropdownSelector(
                    label = "Rol / Poziție",
                    options = roles,
                    selectedOption = selectedRole,
                    onOptionSelected = { selectedRole = it }
                )

                DropdownSelector(
                    label = "Nivel de experiență",
                    options = levels,
                    selectedOption = selectedLevel,
                    onOptionSelected = { selectedLevel = it }
                )

                DropdownSelector(
                    label = "Limbă",
                    options = languages,
                    selectedOption = selectedLanguage,
                    onOptionSelected = { selectedLanguage = it }
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        viewModel.updateConfig(
                            domain = selectedDomain,
                            specialization = selectedSpecialization,
                            role = selectedRole,
                            experienceLevel = selectedLevel,
                            language = selectedLanguage
                        )
                        viewModel.startInterview(rawGistUrl)
                        onStartInterview()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = selectedRole.isNotBlank()
                ) {
                    Text("Începe Interviul (7 Întrebări Random)")
                }

                OutlinedButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vezi Istoric Interviuri Salvate")
                }
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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