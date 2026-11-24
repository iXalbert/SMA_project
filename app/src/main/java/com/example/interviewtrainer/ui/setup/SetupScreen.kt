package com.example.interviewtrainer.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.QuestionBankResponse
import com.example.interviewtrainer.viewmodel.InterviewViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: InterviewViewModel,
    onStartInterview: () -> Unit,
    onOpenHistory: () -> Unit = {}
) {
    val rawGistUrl = "https://gist.githubusercontent.com/iXalbert/d6f75eebf201f1efa3b108e514c74061/raw/questions.json"

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var questionBank by remember { mutableStateOf<QuestionBankResponse?>(null) }
    var isLoadingBank by remember { mutableStateOf(true) }
    var showDailyAnswer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            questionBank = viewModel.repository.fetchQuestionBank(rawGistUrl)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingBank = false
        }
    }

    val dailyQuestion = remember(questionBank) {
        val allQuestions = questionBank?.domains
            ?.flatMap { it.specializations }
            ?.flatMap { it.roles }
            ?.flatMap { it.questions }
            .orEmpty()
        if (allQuestions.isNotEmpty()) {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            allQuestions[dayOfYear % allQuestions.size]
        } else null
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

    val sessionModes = listOf("Antrenament", "Simulare (2 min)")
    var selectedMode by remember { mutableStateOf(sessionModes.first()) }

    // Format Interviu: Scris + Audio (Chat) vs. Apel Video Live cu Amalia
    var isVideoCallSelected by remember { mutableStateOf(true) }
    val interviewerStyles = listOf("Friendly HR", "Tech Lead")
    var selectedStyle by remember { mutableStateOf(interviewerStyles.first()) }

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
                // Card Întrebarea Zilei (Daily Warmup)
                dailyQuestion?.let { dq ->
                    val isEn = selectedLanguage == "Engleză"
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showDailyAnswer = !showDailyAnswer }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isEn) "DAILY WARMUP" else "ÎNTREBAREA ZILEI",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = if (showDailyAnswer) "Ascunde" else "Vezi răspuns",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEn) dq.questionEn else dq.questionRo,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            AnimatedVisibility(visible = showDailyAnswer) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isEn) dq.referenceAnswerEn else dq.referenceAnswerRo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 1: Format Interviu (Scris + Audio vs. Apel Video Live cu Amalia)
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
                            text = "1. Experiență Interviu",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Opțiunea A: Scris + Audio
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!isVideoCallSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    width = if (!isVideoCallSelected) 2.dp else 1.dp,
                                    color = if (!isVideoCallSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isVideoCallSelected = false }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = if (!isVideoCallSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Scris + Audio",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Chat & dictare vocală",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Opțiunea B: Apel Video Full-Screen cu Amalia
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isVideoCallSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    width = if (isVideoCallSelected) 2.dp else 1.dp,
                                    color = if (isVideoCallSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isVideoCallSelected = true }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = if (isVideoCallSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Apel cu asistentul AI",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Video live + AI Vision",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isVideoCallSelected) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillSelectorGroup(
                                    label = "Stil Intervievator (Amalia)",
                                    options = interviewerStyles,
                                    selectedOption = selectedStyle,
                                    onOptionSelected = { selectedStyle = it }
                                )
                                Text(
                                    text = if (selectedStyle == "Tech Lead")
                                        "Tech Lead: Cere obligatoriu complexitatea Big O și cazurile limită + analiză TensorFlow Lite (față, umeri, mâini)."
                                    else
                                        "Friendly HR: Interviu conversațional cald + follow-up pe răspunsurile scurte + analiză nonverbală live.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Card 2: Poziția vizată
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
                            text = "2. Poziția vizată",
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

                // Card 3: Nivel, Limbă și Mod Simulare
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
                            text = "3. Parametri sesiune",
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

                        PillSelectorGroup(
                            label = "Mod de desfășurare",
                            options = sessionModes,
                            selectedOption = selectedMode,
                            onOptionSelected = { selectedMode = it }
                        )

                        if (selectedMode == sessionModes[1]) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Fără indicii • Cronometru 02:00 per întrebare",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        viewModel.updateConfig(
                            domain = selectedDomain,
                            specialization = selectedSpecialization,
                            role = selectedRole,
                            experienceLevel = selectedLevel,
                            language = selectedLanguage,
                            isTimedMode = (selectedMode == sessionModes[1]),
                            isVideoCallMode = isVideoCallSelected,
                            interviewerStyle = selectedStyle
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
                    Icon(
                        imageVector = if (isVideoCallSelected) Icons.Default.Videocam else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVideoCallSelected) "Apelează asistentul AI (Start)" else "Începe Interviul",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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