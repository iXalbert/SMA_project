package com.example.interviewtrainer.data.model

data class InterviewReport(
    val score: Int = 85,
    val summary: String = "Ai demonstrat o bună înțelegere a conceptelor fundamentale.",
    val strengths: List<String> = listOf("Comunicare clară", "Cunoștințe solide de arhitectură"),
    val improvements: List<String> = listOf("Aprofundarea gestionării memoriei", "Exemple mai concrete din proiecte anterioare")
)