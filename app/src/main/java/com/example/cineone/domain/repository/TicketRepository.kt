package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Ticket
import com.example.cineone.domain.model.TicketDetail
import kotlinx.coroutines.flow.Flow

interface TicketRepository {
    fun getMyTickets(tab: String): Flow<Resource<List<Ticket>>>
    fun getTicketDetail(ticketId: Long): Flow<Resource<TicketDetail>>
}