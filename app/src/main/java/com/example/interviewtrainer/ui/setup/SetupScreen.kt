package com.example.interviewtrainer.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val savedInterviews by viewModel.savedInterviews.collectAsState()
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            text = "Interview Trainer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Simulator AI & Pregătire Tehnică",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Comută Tema",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Se sincronizează banca de întrebări...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Poziția vizată
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. Poziția vizată",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                    }
                }

                // Card 2: Nivel și Limbă (Pill Selectors moderne)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "2. Parametri sesiune",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        PillSelectorGroup(
                            label = "Nivel de experiență",
                            options = levels,
                            selectedOption = selectedLevel,
                            onOptionSelected = { selectedLevel = it }
                        )

                        PillSelectorGroup(
                            label = "Limba interviului",
                            options = languages,
                            selectedOption = selectedLanguage,
                            onOptionSelected = { selectedLanguage = it }
                        )
                    }
                }

                // Carduri rapide pentru Banca Personală și Istoric
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Întrebările Mele",
                        subtitle = "${customQuestions.size} salvate",
                        icon = Icons.Default.Quiz,
                        onClick = { showCustomBankSheet = true },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = "Istoric",
                        subtitle = "${savedInterviews.size} sesiuni",
                        icon = Icons.Default.History,
                        onClick = onOpenHistory,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Buton principal CTA
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = selectedRole.isNotBlank()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Începe Interviul",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Dialog listă întrebări proprii (CRUD Custom Questions)
        if (showCustomBankSheet) {
            AlertDialog(
                onDismissRequest = { showCustomBankSheet = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Întrebările Mele", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        FilledTonalIconButton(onClick = { showCreateQuestionDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Adaugă Întrebare")
                        }
                    }
                },
                text = {
                    if (customQuestions.isEmpty()) {
                        Text(
                            text = "Nu ai adăugat încă întrebări proprii. Apasă + pentru a crea una pentru rolul $selectedRole.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(customQuestions, key = { it.id }) { q ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = q.role,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = q.questionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (q.keywordsCsv.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Cuvinte cheie: ${q.keywordsCsv}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(onClick = { editingQuestion = q }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Editează", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { viewModel.deleteCustomQuestion(q) }, modifier = Modifier.size(32.dp)) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Șterge",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
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
fun PillSelectorGroup(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOptionSelected(option) }
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Întrebarea") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = referenceAnswer,
                    onValueChange = { referenceAnswer = it },
                    label = { Text("Răspuns ideal") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = keywordsCsv,
                    onValueChange = { keywordsCsv = it },
                    label = { Text("Cuvinte cheie (ex: O(1), HashMap)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Indiciu (opțional)") },
                    shape = RoundedCornerShape(12.dp),
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
            shape = RoundedCornerShape(12.dp),
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