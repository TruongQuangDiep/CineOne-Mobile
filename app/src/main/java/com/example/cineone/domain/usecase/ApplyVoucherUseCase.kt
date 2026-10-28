package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.repository.VoucherRepository
import kotlinx.coroutines.flow.Flow

class ApplyVoucherUseCase(private val repository: VoucherRepository) {
    operator fun invoke(code: String, subtotal: Long): Flow<Resource<Long>> {
        return repository.applyVoucher(code.trim().uppercase(), subtotal)
    }
}