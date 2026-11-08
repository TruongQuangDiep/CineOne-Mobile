package com.example.cineone.ui.ticket

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cineone.domain.model.Ticket
import com.example.cineone.navigation.Screen
import com.example.cineone.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketListScreen(
    navController: NavController,
    viewModel: TicketViewModel = viewModel(factory = TicketViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("UPCOMING" to "Vé sắp tới", "HISTORY" to "Lịch sử vé")

    LaunchedEffect(Unit) {
        viewModel.loadTickets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vé của tôi", color = CineTextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CineTextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CineBackgroundDark)
            )
        },
        containerColor = CineBackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = if (uiState.currentTab == "UPCOMING") 0 else 1,
                containerColor = CineSurfaceCard,
                contentColor = CineRedPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (uiState.currentTab == "UPCOMING") 0 else 1]),
                        color = CineRedPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, pair ->
                    val selected = (if (uiState.currentTab == "UPCOMING") 0 else 1) == index
                    Tab(
                        selected = selected,
                        onClick = { viewModel.switchTab(pair.first) },
                        text = {
                            Text(
                                text = pair.second,
                                color = if (selected) CineRedPrimary else CineTextGray,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = CineRedPrimary)
                } else if (uiState.tickets.isEmpty()) {
                    EmptyTicketView(isUpcoming = uiState.currentTab == "UPCOMING") {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.tickets) { ticket ->
                            TicketItemCard(
                                ticket = ticket,
                                isUpcoming = uiState.currentTab == "UPCOMING",
                                onClick = {
                                    navController.navigate(Screen.TicketDetail.createRoute(ticket.ticketId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketItemCard(
    ticket: Ticket,
    isUpcoming: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CineSurfaceCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ticket.movieTitle.ifBlank { "Vé xem phim" },
                    color = CineTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = if (isUpcoming) CineSuccess.copy(alpha = 0.15f) else CineTextMuted.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (isUpcoming) CineSuccess else CineTextMuted)
                ) {
                    Text(
                        text = if (isUpcoming) "SẮP CHIẾU" else "ĐÃ HOÀN TẤT",
                        color = if (isUpcoming) CineSuccess else CineTextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = CineDivider)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Theaters, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${ticket.cinemaName} • ${ticket.roomName}",
                    color = CineTextGray,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${ticket.showTime} - ${ticket.showDate}",
                    color = CineTextWhite,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AirlineSeatReclineNormal, contentDescription = null, tint = CineRedPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Ghế: ${ticket.seatCodes.ifBlank { "Tiêu chuẩn" }}",
                        color = CineRedPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "Mã: ${ticket.ticketCode}",
                    color = CineTextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun EmptyTicketView(isUpcoming: Boolean, onExploreClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ConfirmationNumber,
            contentDescription = null,
            tint = CineTextMuted,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isUpcoming) "Bạn chưa có vé xem phim nào sắp tới" else "Lịch sử vé của bạn đang trống",
            color = CineTextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Hãy chọn bộ phim bạn yêu thích và đặt vé ngay hôm nay!",
            color = CineTextGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onExploreClick,
            colors = ButtonDefaults.buttonColors(containerColor = CineRedPrimary),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Đặt vé ngay")
        }
    }
}
