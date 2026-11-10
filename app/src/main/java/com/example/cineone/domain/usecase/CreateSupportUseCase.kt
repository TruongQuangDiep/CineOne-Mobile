package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.SupportRequest
import com.example.cineone.domain.repository.SupportRepository
import kotlinx.coroutines.flow.Flow

class CreateSupportUseCase(private val supportRepository: SupportRepository) {
    operator fun invoke(
        type: String,
        category: String,
        content: String,
        bookingId: Long? = null,
        bookingCode: String? = null
    ): Flow<Resource<SupportRequest>> {
        return supportRepository.createSupportRequest(
            type = type,
            category = category,
            content = content,
            bookingId = bookingId,
            bookingCode = bookingCode
        )
    }
}

