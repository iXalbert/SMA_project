package com.example.interviewtrainer.data.model

data class QuestionBankResponse(
    val domains: List<DomainDto> = emptyList()
)

data class DomainDto(
    val name: String,
    val specializations: List<SpecializationDto> = emptyList()
)

data class SpecializationDto(
    val name: String,
    val roles: List<RoleDto> = emptyList()
)

data class RoleDto(
    val title: String,
    val questions: List<QuestionDto> = emptyList()
)

data class QuestionDto(
    val id: Int,
    val questionRo: String,
    val questionEn: String,
    val referenceAnswerRo: String,
    val referenceAnswerEn: String,
    val keywordsRo: List<String>,
    val keywordsEn: List<String>,
    val hintRo: String,
    val hintEn: String
)