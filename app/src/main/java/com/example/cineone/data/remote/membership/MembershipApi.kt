package com.example.cineone.data.remote.membership

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.membership.MyMembershipDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class MembershipApi(private val client: HttpClient) {
    suspend fun getMyMembership(): ApiResponseDto<MyMembershipDto> {
        return client.get(ApiConfig.Membership.MY_TIER).body()
    }
}