package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.SupportRequest
import com.example.cineone.domain.repository.SupportRepository
import kotlinx.coroutines.flow.Flow

class GetMySupportRequestsUseCase(private val supportRepository: SupportRepository) {
    operator fun invoke(): Flow<Resource<List<SupportRequest>>> {
        return supportRepository.getMySupportRequests()
    }
}

