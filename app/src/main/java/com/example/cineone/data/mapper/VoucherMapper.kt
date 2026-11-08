package com.example.cineone.data.mapper

import com.example.cineone.data.dto.voucher.VoucherDto
import com.example.cineone.domain.model.Voucher

fun VoucherDto.toDomain(): Voucher = Voucher(
    voucherId = voucherId,
    code = code,
    title = title ?: description ?: ("Ưu đãi " + code),
    description = description,
    imageUrl = imageUrl,
    discountType = discountType ?: "FIXED",
    discountAmount = discountValue.toLong(),
    minOrderAmount = minOrder,
    maxDiscount = maxDiscount,
    startAt = startAt,
    expiresAt = endAt,
    status = status,
    discountDisplay = discountDisplay ?: ("Giảm " + discountValue.toLong() + "đ"),
    minOrderDisplay = minOrderDisplay ?: ("Đơn tối thiểu: " + minOrder.toLong() + "đ"),
    maxDiscountDisplay = maxDiscountDisplay,
    expiryDisplay = expiryDisplay ?: ("HSD: " + (endAt ?: "")),
    periodDisplay = periodDisplay ?: "",
    remainingUsage = remainingUsage ?: 999,
    tab = tab ?: "AVAILABLE"
)