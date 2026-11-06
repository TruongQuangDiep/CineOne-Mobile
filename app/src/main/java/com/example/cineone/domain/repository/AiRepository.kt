package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.AiExtractedCriteria
import com.example.cineone.domain.model.AiMessage
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    fun askAssistant(
        query: String,
        previousCriteria: AiExtractedCriteria? = null,
        currentLat: Double? = null,
        currentLon: Double? = null,
        homeLat: Double? = null,
        homeLon: Double? = null,
        homeAddress: String = ""
    ): Flow<Resource<AiMessage>>
}

