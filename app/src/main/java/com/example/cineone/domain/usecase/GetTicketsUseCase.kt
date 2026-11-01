package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Ticket
import com.example.cineone.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow

class GetTicketsUseCase(
    private val ticketRepository: TicketRepository
) {
    operator fun invoke(tab: String = "UPCOMING"): Flow<Resource<List<Ticket>>> {
        return ticketRepository.getMyTickets(tab)
    }
}