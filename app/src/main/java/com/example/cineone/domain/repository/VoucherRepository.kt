package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Voucher
import kotlinx.coroutines.flow.Flow

interface VoucherRepository {
    fun getMyVouchers(tab: String): Flow<Resource<List<Voucher>>>
    fun applyVoucher(code: String, subtotal: Long): Flow<Resource<Long>>
}