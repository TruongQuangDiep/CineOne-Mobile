package com.example.cineone.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponseDto<T>(
    val success: Boolean? = null,
    val status: Int = 200,
    val message: String? = null,
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = (success == true) || (success == null && status in 200..299)

    // Getter giữ tương thích cho các Repository
    val isResponseSuccess: Boolean
        get() = isSuccess
}
