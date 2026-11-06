package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.AiExtractedCriteria
import com.example.cineone.domain.model.AiMessage
import com.example.cineone.domain.repository.AiRepository
import kotlinx.coroutines.flow.Flow

class AskAssistantUseCase(
    private val aiRepository: AiRepository
) {
    operator fun invoke(
        query: String,
        previousCriteria: AiExtractedCriteria? = null,
        currentLat: Double? = null,
        currentLon: Double? = null,
        homeLat: Double? = null,
        homeLon: Double? = null,
        homeAddress: String = ""
    ): Flow<Resource<AiMessage>> {
        return aiRepository.askAssistant(
            query = query,
            previousCriteria = previousCriteria,
            currentLat = currentLat,
            currentLon = currentLon,
            homeLat = homeLat,
            homeLon = homeLon,
            homeAddress = homeAddress
        )
    }
}

