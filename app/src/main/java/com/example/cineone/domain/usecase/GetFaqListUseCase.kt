package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.FaqItem
import com.example.cineone.domain.repository.SupportRepository
import kotlinx.coroutines.flow.Flow

class GetFaqListUseCase(private val supportRepository: SupportRepository) {
    operator fun invoke(category: String? = null, q: String? = null): Flow<Resource<List<FaqItem>>> {
        return supportRepository.getFaqList(category = category, q = q)
    }
}

