package com.example.cineone.data.remote.ticket

import com.example.cineone.core.network.ApiConfig
import com.example.cineone.data.dto.ApiResponseDto
import com.example.cineone.data.dto.ticket.TicketDetailDto
import com.example.cineone.data.dto.ticket.TicketDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class TicketApi(private val client: HttpClient) {

    suspend fun getMyTickets(tab: String = "UPCOMING"): ApiResponseDto<List<TicketDto>> {
        return client.get(ApiConfig.Ticket.MY_TICKETS) {
            parameter("tab", tab)
        }.body()
    }

    suspend fun getTicketDetail(ticketId: Long): ApiResponseDto<TicketDetailDto> {
        return client.get("${ApiConfig.Ticket.DETAIL}/$ticketId").body()
    }
}