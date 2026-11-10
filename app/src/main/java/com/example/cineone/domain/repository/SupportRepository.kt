package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.FaqItem
import com.example.cineone.domain.model.SupportRequest
import kotlinx.coroutines.flow.Flow

interface SupportRepository {
    fun createSupportRequest(
        type: String,
        category: String,
        content: String,
        bookingId: Long? = null,
        bookingCode: String? = null
    ): Flow<Resource<SupportRequest>>

    fun getMySupportRequests(): Flow<Resource<List<SupportRequest>>>

    fun getSupportDetail(id: Long): Flow<Resource<SupportRequest>>

    fun getFaqList(category: String? = null, q: String? = null): Flow<Resource<List<FaqItem>>>
}

