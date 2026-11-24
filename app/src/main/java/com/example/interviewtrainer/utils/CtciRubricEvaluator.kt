package com.example.interviewtrainer.utils

data class CtciEvaluationResult(
    val score: Int,
    val matchedKeywords: List<String>,
    val missedKeywords: List<String>,
    val hasBigOAnalysis: Boolean,
    val hasEdgeCases: Boolean,
    val hasStructuredReasoning: Boolean,
    val usedHint: Boolean,
    val rubricSummaryRo: String,
    val rubricSummaryEn: String
)

object CtciRubricEvaluator {

    private val bigOPatterns = listOf(
        "o(1)", "o(n)", "o(log n)", "o(n log n)", "o(n^2)", "o(n²)",
        "complexitate", "complexity", "timp", "memorie", "spațiu", "space", "time", "liniar", "logaritmic"
    )

    private val edgeCasePatterns = listOf(
        "null", "gol", "empty", "zero", "0", "negativ", "negative",
        "coliziun", "collision", "overflow", "limită", "edge case", "excepți", "exception", "invalid"
    )

    private val reasoningPatterns = listOf(
        "pentru că", "deoarece", "avantaj", "dezavantaj", "compromis", "trade-off", "tradeoff",
        "alternativ", "situați", "rezultat", "acțiun", "because", "therefore", "however", "result", "impact"
    )

    fun evaluateAnswer(
        userAnswer: String,
        referenceAnswer: String,
        keywords: List<String>,
        usedHint: Boolean
    ): CtciEvaluationResult {
        val normalized = userAnswer.lowercase().trim()

        if (normalized.isBlank()) {
            return CtciEvaluationResult(
                score = 0,
                matchedKeywords = emptyList(),
                missedKeywords = keywords,
                hasBigOAnalysis = false,
                hasEdgeCases = false,
                hasStructuredReasoning = false,
                usedHint = usedHint,
                rubricSummaryRo = "Răspuns gol.",
                rubricSummaryEn = "Empty answer."
            )
        }

        // 1. Potrivire cuvinte cheie (pondere 65%)
        val matched = keywords.filter { kw -> normalized.contains(kw.lowercase().trim()) }
        val missed = keywords.filter { kw -> !normalized.contains(kw.lowercase().trim()) }

        val keywordRatio = if (keywords.isNotEmpty()) {
            matched.size.toFloat() / keywords.size.toFloat()
        } else {
            // Dacă o întrebare custom nu are keywords definite, evaluăm lungimea și relevanța
            if (normalized.split(" ").size >= 8) 0.85f else 0.5f
        }
        var totalScore = keywordRatio * 65f

        // 2. Analiză Big O (+15%)
        val hasBigO = bigOPatterns.any { normalized.contains(it) }
        if (hasBigO) totalScore += 15f

        // 3. Edge Cases (+10%)
        val hasEdge = edgeCasePatterns.any { normalized.contains(it) }
        if (hasEdge) totalScore += 10f

        // 4. Raționament structurat / Trade-offs / STAR (+10%)
        val hasReasoning = reasoningPatterns.any { normalized.contains(it) }
        if (hasReasoning) totalScore += 10f

        // 5. Coachability (penalizare calibrată dacă s-a cerut indiciu)
        if (usedHint) {
            totalScore -= 12f
        }

        val finalScore = totalScore.toInt().coerceIn(0, 100)

        val summaryRo = buildString {
            append("Rubrică CtCI: ")
            append("Big O ${if (hasBigO) "✓" else "✕"} • ")
            append("Edge Cases ${if (hasEdge) "✓" else "✕"} • ")
            append("Argumentare ${if (hasReasoning) "✓" else "✕"}")
            if (usedHint) append(" • Indiciu folosit (-12p)")
        }

        val summaryEn = buildString {
            append("CtCI Rubric: ")
            append("Big O ${if (hasBigO) "✓" else "✕"} • ")
            append("Edge Cases ${if (hasEdge) "✓" else "✕"} • ")
            append("Reasoning ${if (hasReasoning) "✓" else "✕"}")
            if (usedHint) append(" • Hint used (-12p)")
        }

        return CtciEvaluationResult(
            score = finalScore,
            matchedKeywords = matched,
            missedKeywords = missed,
            hasBigOAnalysis = hasBigO,
            hasEdgeCases = hasEdge,
            hasStructuredReasoning = hasReasoning,
            usedHint = usedHint,
            rubricSummaryRo = summaryRo,
            rubricSummaryEn = summaryEn
        )
    }
}