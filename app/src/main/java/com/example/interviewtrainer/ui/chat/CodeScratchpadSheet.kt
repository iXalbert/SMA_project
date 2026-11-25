package com.example.interviewtrainer.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeScratchpadSheet(
    isEn: Boolean,
    initialCode: String = "",
    onDismiss: () -> Unit,
    onAttachCodeToAnswer: (formattedBlock: String) -> Unit
) {
    val languages = listOf("C++", "Kotlin", "Python", "C (Embedded)", "Pseudocod")
    var selectedLang by remember { mutableStateOf(languages.first()) }
    var codeText by remember {
        mutableStateOf(
            initialCode.ifBlank {
                "// Complexitate: Timp O(N), Spațiu O(1)\n// Edge cases: null, empty\n"
            }
        )
    }

    val quickSnippets = listOf("{ }", "[ ]", "->", "O(N)", "O(log N)", "if (ptr == null)")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (isEn) "Whiteboard & Code Scratchpad" else "Whiteboard & Pseudocode Pad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Selector limbaj
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                languages.forEach { lang ->
                    FilterChip(
                        selected = selectedLang == lang,
                        onClick = { selectedLang = lang },
                        label = { Text(lang, fontSize = 11.sp) }
                    )
                }
            }

            // Bară rapidă de simboluri tehnice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickSnippets.forEach { snippet ->
                    AssistChip(
                        onClick = { codeText += " $snippet " },
                        label = {
                            Text(
                                text = snippet,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            }

            OutlinedTextField(
                value = codeText,
                onValueChange = { codeText = it },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )

            Button(
                onClick = {
                    val formatted = "[$selectedLang Whiteboard]\n$codeText"
                    onAttachCodeToAnswer(formatted)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEn) "Submit Whiteboard Solution" else "Trimite Soluția de pe Whiteboard",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}