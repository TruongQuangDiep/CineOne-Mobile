package com.example.cineone.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponseDto<T>(
    val status: Int = 200,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = status in 200..299

    // Getter giữ tương thích cho các Repository đang gọi response.success
    val success: Boolean
        get() = isSuccess
}