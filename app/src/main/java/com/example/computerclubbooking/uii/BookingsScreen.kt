package com.example.computerclubbooking.uii

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ViewModel для управления бронями пользователя
class UserBookingsViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _activeBookings = MutableStateFlow<List<Booking>>(emptyList())
    val activeBookings: StateFlow<List<Booking>> = _activeBookings

    private val _completedBookings = MutableStateFlow<List<Booking>>(emptyList())
    val completedBookings: StateFlow<List<Booking>> = _completedBookings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadUserBookings()
    }

    private fun loadUserBookings() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            println("❌ Пользователь не авторизован!")
            return
        }

        println("🔍 Загружаем брони для userId: $userId")

        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Загружаем активные брони
                db.collection("bookings")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "active")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            println("❌ Error loading active bookings: ${error.message}")
                            _isLoading.value = false
                            return@addSnapshotListener
                        }

                        println("📦 Получено документов: ${snapshot?.documents?.size ?: 0}")

                        val bookings = snapshot?.documents?.mapNotNull { doc ->
                            println("📄 Документ: ${doc.id}")
                            try {
                                Booking(
                                    id = doc.id,
                                    computerId = doc.getString("computerId") ?: "",
                                    computerName = doc.getString("computerName") ?: "",
                                    computerCategory = doc.getString("computerCategory") ?: "",
                                    userEmail = doc.getString("userEmail") ?: "",
                                    userId = doc.getString("userId") ?: "",
                                    startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                                    endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                                    createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                                    status = doc.getString("status") ?: "active",
                                    totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                                    timePackage = doc.getString("timePackage") ?: ""
                                )
                            } catch (e: Exception) {
                                println("⚠️ Error parsing booking ${doc.id}: ${e.message}")
                                null
                            }
                        } ?: emptyList()

                        println("✅ Загружено активных броней: ${bookings.size}")
                        _activeBookings.value = bookings
                        _isLoading.value = false
                    }

                // Загружаем завершенные брони
                db.collection("bookings")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "completed")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            println("❌ Error loading completed bookings: ${error.message}")
                            return@addSnapshotListener
                        }

                        val bookings = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                Booking(
                                    id = doc.id,
                                    computerId = doc.getString("computerId") ?: "",
                                    computerName = doc.getString("computerName") ?: "",
                                    computerCategory = doc.getString("computerCategory") ?: "",
                                    userEmail = doc.getString("userEmail") ?: "",
                                    userId = doc.getString("userId") ?: "",
                                    startTime = doc.getTimestamp("startTime") ?: Timestamp.now(),
                                    endTime = doc.getTimestamp("endTime") ?: Timestamp.now(),
                                    createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now(),
                                    status = doc.getString("status") ?: "completed",
                                    totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                                    timePackage = doc.getString("timePackage") ?: ""
                                )
                            } catch (e: Exception) {
                                println("⚠️ Error parsing completed booking: ${e.message}")
                                null
                            }
                        } ?: emptyList()

                        println("✅ Загружено завершенных броней: ${bookings.size}")
                        _completedBookings.value = bookings
                    }

            } catch (e: Exception) {
                println("❌ Error in loadUserBookings: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    // Функция отмены бронирования
    fun cancelBooking(bookingId: String, computerId: String) {
        viewModelScope.launch {
            try {
                db.runTransaction { transaction ->
                    // Обновляем статус брони
                    val bookingRef = db.collection("bookings").document(bookingId)
                    transaction.update(bookingRef, "status", "cancelled")

                    // Освобождаем компьютер
                    val computerRef = db.collection("computers").document(computerId)
                    transaction.update(computerRef, "isAvailable", true)
                }.await()

                println("✅ Бронирование $bookingId отменено")
            } catch (e: Exception) {
                println("❌ Error cancelling booking: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    viewModel: UserBookingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val activeBookings by viewModel.activeBookings.collectAsState()
    val completedBookings by viewModel.completedBookings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showCancelDialog by remember { mutableStateOf<Booking?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Мои бронирования",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0D1117)
                )
            )
        },
        containerColor = Color(0xFF0D1117)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Табы
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF161B22),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Активные (${activeBookings.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("История") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            } else {
                when (selectedTab) {
                    0 -> ActiveBookingsList(
                        bookings = activeBookings,
                        onCancelClick = { showCancelDialog = it }
                    )
                    1 -> CompletedBookingsList(bookings = completedBookings)
                }
            }
        }

        // Диалог подтверждения отмены
        showCancelDialog?.let { booking ->
            AlertDialog(
                onDismissRequest = { showCancelDialog = null },
                title = { Text("Отменить бронирование?") },
                text = {
                    Text("Вы уверены, что хотите отменить бронирование ${booking.computerName}?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.cancelBooking(booking.id, booking.computerId)
                            showCancelDialog = null
                        }
                    ) {
                        Text("Да, отменить", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = null }) {
                        Text("Нет")
                    }
                }
            )
        }
    }
}

@Composable
fun ActiveBookingsList(
    bookings: List<Booking>,
    onCancelClick: (Booking) -> Unit
) {
    if (bookings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "У вас нет активных бронирований 📅",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bookings) { booking ->
                BookingCard(
                    booking = booking,
                    isActive = true,
                    onCancelClick = { onCancelClick(booking) }
                )
            }
        }
    }
}

@Composable
fun CompletedBookingsList(bookings: List<Booking>) {
    if (bookings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "История пуста",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bookings) { booking ->
                BookingCard(
                    booking = booking,
                    isActive = false,
                    onCancelClick = null
                )
            }
        }
    }
}

@Composable
fun BookingCard(
    booking: Booking,
    isActive: Boolean,
    onCancelClick: (() -> Unit)?
) {
    val categoryColor = when(booking.computerCategory) {
        "ВИП" -> Color(0xFFFFD700)
        "Bootcamp" -> Color(0xFF00FF00)
        else -> Color(0xFF007AFF)
    }

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161B22)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isActive) Icons.Default.CheckCircle else Icons.Default.History,
                        contentDescription = null,
                        tint = if (isActive) Color(0xFF00FF00) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        booking.computerName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Text(
                    booking.computerCategory,
                    color = categoryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Информация о времени
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "📅 Дата:",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        dateFormat.format(booking.startTime.toDate()),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Column {
                    Text(
                        "⏰ Время:",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        "${timeFormat.format(booking.startTime.toDate())} - ${timeFormat.format(booking.endTime.toDate())}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Пакет и цена
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "📦 ${booking.timePackage}",
                    color = Color(0xFFB0C4DE),
                    fontSize = 14.sp
                )

                Text(
                    "💰 ${booking.totalPrice} ₸",
                    color = categoryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Оставшееся время для активных броней
            if (isActive) {
                val timeLeft = booking.endTime.toDate().time - System.currentTimeMillis()
                val hoursLeft = timeLeft / (1000 * 60 * 60)
                val minutesLeft = (timeLeft % (1000 * 60 * 60)) / (1000 * 60)

                if (hoursLeft > 0 || minutesLeft > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⏳ Осталось: ${hoursLeft}ч ${minutesLeft}м",
                        color = Color(0xFFFFA500),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Кнопка отмены
                onCancelClick?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = it,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Отменить бронирование", color = Color.Red)
                    }
                }
            }
        }
    }
}