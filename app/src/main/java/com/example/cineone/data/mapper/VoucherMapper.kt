package com.example.cineone.data.mapper

import com.example.cineone.data.dto.voucher.VoucherDto
import com.example.cineone.domain.model.Voucher

fun VoucherDto.toDomain(): Voucher = Voucher(
    voucherId = voucherId,
    code = code,
    title = title,
    discountAmount = discountAmount,
    minOrderAmount = minOrderAmount,
    expiresAt = expiresAt,
    status = status
)