package com.example.cineone.data.mapper

import com.example.cineone.data.dto.voucher.VoucherDto
import com.example.cineone.domain.model.Voucher

fun VoucherDto.toDomain(): Voucher = Voucher(
    voucherId = voucherId,
    code = code,
    title = title.orEmpty(),
    imageUrl = imageUrl,
    discountType = discountType?.ifBlank { "FIXED" } ?: "FIXED",
    discountAmount = discountAmount.toLong(),
    minOrderAmount = minOrderAmount,
    maxDiscount = maxDiscount,
    startAt = startAt,
    expiresAt = expiresAt,
    status = status
)
