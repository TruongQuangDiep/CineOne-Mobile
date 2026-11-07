package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.payment.PaymentLinkResponseDto
import com.example.cineone.data.dto.payment.PaymentStatusDto
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun createPaymentLink(bookingId: Long): Flow<Resource<PaymentLinkResponseDto>>
    fun getPaymentStatus(bookingId: Long): Flow<Resource<PaymentStatusDto>>
}