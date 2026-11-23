package com.example.interviewtrainer.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.CustomQuestionEntity
import com.example.interviewtrainer.data.model.QuestionBankResponse
import com.example.interviewtrainer.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: InterviewViewModel,
    onStartInterview: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val rawGistUrl = "https://gist.githubusercontent.com/iXalbert/d6f75eebf201f1efa3b108e514c74061/raw/questions.json"

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val customQuestions by viewModel.customQuestions.collectAsState()
    var questionBank by remember { mutableStateOf<QuestionBankResponse?>(null) }
    var isLoadingBank by remember { mutableStateOf(true) }

    var showCustomBankSheet by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<CustomQuestionEntity?>(null) }
    var showCreateQuestionDialog by remember { mutableStateOf(false) }

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
            TopAppBar(
                title = { Text("Configurare Interviu (SMA)", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Comută Tema Dark/Light"
                        )
                    }
                }
            )
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
                    Text("Începe Interviul")
                }

                // Buton Banca Personală de Întrebări (CRUD Custom Questions)
                FilledTonalButton(
                    onClick = { showCustomBankSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Banca Mea de Întrebări (${customQuestions.size})")
                }

                OutlinedButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vezi Istoric Interviuri Salvate")
                }
            }
        }

        // Dialog listă întrebări proprii (Read / Create / Update / Delete)
        if (showCustomBankSheet) {
            AlertDialog(
                onDismissRequest = { showCustomBankSheet = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Întrebările Mele", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { showCreateQuestionDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Adaugă Întrebare")
                        }
                    }
                },
                text = {
                    if (customQuestions.isEmpty()) {
                        Text("Nu ai adăugat încă întrebări proprii. Apasă + pentru a crea una pentru rolul $selectedRole.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(customQuestions, key = { it.id }) { q ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(q.role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(q.questionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Cuvinte cheie: ${q.keywordsCsv}", style = MaterialTheme.typography.bodySmall)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(onClick = { editingQuestion = q }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Editează", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { viewModel.deleteCustomQuestion(q) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Șterge", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCustomBankSheet = false }) {
                        Text("Închide")
                    }
                }
            )
        }

        // Dialog CREATE întrebare custom
        if (showCreateQuestionDialog) {
            CustomQuestionFormDialog(
                title = "Întrebare Nouă ($selectedRole)",
                initialQuestion = "",
                initialAnswer = "",
                initialKeywords = "",
                initialHint = "",
                onDismiss = { showCreateQuestionDialog = false },
                onSave = { qText, refAns, kw, hint ->
                    viewModel.addCustomQuestion(
                        CustomQuestionEntity(
                            domain = selectedDomain,
                            specialization = selectedSpecialization,
                            role = selectedRole.ifBlank { "Software Engineer" },
                            questionText = qText,
                            referenceAnswer = refAns,
                            keywordsCsv = kw,
                            hint = hint
                        )
                    )
                    showCreateQuestionDialog = false
                }
            )
        }

        // Dialog UPDATE întrebare custom
        editingQuestion?.let { current ->
            CustomQuestionFormDialog(
                title = "Editează Întrebarea",
                initialQuestion = current.questionText,
                initialAnswer = current.referenceAnswer,
                initialKeywords = current.keywordsCsv,
                initialHint = current.hint,
                onDismiss = { editingQuestion = null },
                onSave = { qText, refAns, kw, hint ->
                    viewModel.updateCustomQuestion(
                        current.copy(
                            questionText = qText,
                            referenceAnswer = refAns,
                            keywordsCsv = kw,
                            hint = hint
                        )
                    )
                    editingQuestion = null
                }
            )
        }
    }
}

@Composable
fun CustomQuestionFormDialog(
    title: String,
    initialQuestion: String,
    initialAnswer: String,
    initialKeywords: String,
    initialHint: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var questionText by remember { mutableStateOf(initialQuestion) }
    var referenceAnswer by remember { mutableStateOf(initialAnswer) }
    var keywordsCsv by remember { mutableStateOf(initialKeywords) }
    var hint by remember { mutableStateOf(initialHint) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Întrebarea") },
                    maxLines = 3
                )
                OutlinedTextField(
                    value = referenceAnswer,
                    onValueChange = { referenceAnswer = it },
                    label = { Text("Răspuns ideal") },
                    maxLines = 3
                )
                OutlinedTextField(
                    value = keywordsCsv,
                    onValueChange = { keywordsCsv = it },
                    label = { Text("Cuvinte cheie (ex: O(1), HashMap)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Indiciu (opțional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (questionText.isNotBlank()) {
                        onSave(questionText.trim(), referenceAnswer.trim(), keywordsCsv.trim(), hint.trim())
                    }
                }
            ) {
                Text("Salvează")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anulează")
            }
        }
    )
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