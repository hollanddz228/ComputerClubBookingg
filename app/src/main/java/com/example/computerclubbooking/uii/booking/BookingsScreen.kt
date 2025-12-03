package com.example.computerclubbooking.uii.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore

// ------------------------------------------------------
// 🔥 Локальная модель брони
// ------------------------------------------------------
data class Booking(
    val id: String = "",
    val userId: String = "",
    val userEmail: String? = null,
    val computerId: String? = null,
    val computerName: String = "",
    val computerCategory: String = "",
    val packageName: String? = null,
    val timePackageName: String? = null,
    val totalPrice: Double? = null,
    val price: Double? = null,
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val status: String = "active" // active / completed / cancelled / expired / ...
)

// ------------------------------------------------------
// 🔥 UI state
// ------------------------------------------------------
data class BookingsUiState(
    val isLoading: Boolean = true,
    val activeBookings: List<Booking> = emptyList(),
    val historyBookings: List<Booking> = emptyList(),
    val errorMessage: String? = null
)

// ------------------------------------------------------
// 🔥 ViewModel
// ------------------------------------------------------
class UserBookingsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var uiState by mutableStateOf(BookingsUiState())
        private set

    // текущий фильтр истории
    var statusFilter: String = "all"

    // «сырая» история (все, что не активные)
    private var rawHistory: List<Booking> = emptyList()

    init {
        subscribeToBookings()
    }

    private fun subscribeToBookings() {
        val user = auth.currentUser ?: run {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = "Пользователь не авторизован"
            )
            return
        }

        db.collection("bookings")
            .whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = "Ошибка загрузки: ${error.message}"
                    )
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    uiState = uiState.copy(
                        isLoading = false,
                        activeBookings = emptyList(),
                        historyBookings = emptyList()
                    )
                    return@addSnapshotListener
                }

                val all = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }

                val nowSeconds = Timestamp.now().seconds

                val active = all.filter {
                    it.status == "active" &&
                            (it.endTime?.seconds ?: Long.MAX_VALUE) > nowSeconds
                }.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE }

                // История: всё, кроме реально активных
                rawHistory = all.filter {
                    it.status != "active" || (it.endTime?.seconds ?: 0) <= nowSeconds
                }.sortedByDescending { it.startTime?.seconds ?: 0 }

                uiState = uiState.copy(
                    isLoading = false,
                    activeBookings = active
                )

                recomputeHistory()
            }
    }

    private fun recomputeHistory() {
        val filtered = when (statusFilter) {
            "completed" -> rawHistory.filter { it.status == "completed" }
            "cancelled" -> rawHistory.filter { it.status == "cancelled" }
            "expired" -> rawHistory.filter { it.status == "expired" }
            "all", "" -> rawHistory
            else -> rawHistory
        }

        uiState = uiState.copy(historyBookings = filtered)
    }

    fun applyStatusFilter(value: String) {
        statusFilter = value
        recomputeHistory()
    }

    // 🔥 Очистка истории (completed / cancelled / expired)
    fun clearHistory(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(false, "Пользователь не авторизован")
            return
        }

        val toDelete = rawHistory
        if (toDelete.isEmpty()) {
            onResult(true, null)
            return
        }

        viewModelScope.launch {
            try {
                val batch = db.batch()
                toDelete.forEach { booking ->
                    val ref = db.collection("bookings").document(booking.id)
                    batch.delete(ref)
                }
                batch.commit().await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    // 🔥 Отмена активной брони
    fun cancelActiveBooking(
        bookingId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onResult(false, "Пользователь не авторизован")
            return
        }

        viewModelScope.launch {
            try {
                val ref = db.collection("bookings").document(bookingId)
                ref.update(
                    mapOf(
                        "status" to "cancelled",
                        "endTime" to Timestamp.now()
                    )
                ).await()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}

// ------------------------------------------------------
// 🔥 Главный экран бронирований
// ------------------------------------------------------
@Composable
fun BookingsScreen(
    viewModel: UserBookingsViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 – активные, 1 – история

    val backgroundBrush = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF020712),
                Color(0xFF001F3F),
                Color(0xFF120024)
            )
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color(0xFF101827),
                    contentColor = Color.White,
                    snackbarData = data
                )
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Мои бронирования",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                }

                StatsBar(
                    activeCount = uiState.activeBookings.size,
                    historyCount = uiState.historyBookings.size,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(Modifier.height(8.dp))

                BookingsTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(Modifier.height(6.dp))

                when (selectedTab) {
                    0 -> ActiveBookingsSection(
                        bookings = uiState.activeBookings,
                        isLoading = uiState.isLoading,
                        onCancelBooking = { id ->
                            viewModel.cancelActiveBooking(id) { success, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) "Бронь отменена"
                                        else msg ?: "Ошибка отмены"
                                    )
                                }
                            }
                        }
                    )
                    1 -> HistorySection(
                        bookings = uiState.historyBookings,
                        isLoading = uiState.isLoading,
                        statusFilter = viewModel.statusFilter,
                        onStatusFilterChange = { viewModel.applyStatusFilter(it) },
                        onClearClick = {
                            showClearDialog = true
                        }
                    )
                }
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Очистить историю?", color = Color.White) },
                    text = {
                        Text(
                            "Все завершённые и отменённые брони будут удалены без возможности восстановления.",
                            color = Color(0xFFB0C4FF),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showClearDialog = false
                            viewModel.clearHistory { success, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (success) "История очищена"
                                        else msg ?: "Ошибка очистки истории"
                                    )
                                }
                            }
                        }) {
                            Text("Очистить", color = Color(0xFFFF6B6B))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text("Отмена", color = Color.Gray)
                        }
                    },
                    containerColor = Color(0xFF020814)
                )
            }
        }
    }
}

// ------------------------------------------------------
// 🔥 Верхняя статистика
// ------------------------------------------------------
@Composable
fun StatsBar(
    activeCount: Int,
    historyCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x1900C6FF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatChip("Активные", activeCount.toString(), Color(0xFF00E5FF))
        StatChip("В истории", historyCount.toString(), Color(0xFFFFA500))
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color(0xFFB0C4FF), fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------------------------------------------
// 🔥 Табы
// ------------------------------------------------------
@Composable
fun BookingsTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Активные", "История")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0x3300C6FF))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedTab
            val neonBrush = Brush.linearGradient(
                listOf(Color(0xFF00C6FF), Color(0xFF7A00FF))
            )
            val bgModifier = if (selected) {
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(neonBrush)
            } else {
                Modifier.clip(RoundedCornerShape(50))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .then(bgModifier)
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (selected) Color.Black else Color(0xFFB0C4FF),
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                )
            }
        }
    }
}

// ------------------------------------------------------
// 🔥 Активные брони
// ------------------------------------------------------
@Composable
fun ActiveBookingsSection(
    bookings: List<Booking>,
    isLoading: Boolean,
    onCancelBooking: (String) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00C6FF))
        }
        return
    }

    if (bookings.isEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Нет активных броней",
            color = Color(0xFF8895BF),
            fontSize = 14.sp
        )
        return
    }

    Spacer(Modifier.height(6.dp))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(bookings, key = { it.id }) { booking ->
            BookingCard(
                booking = booking,
                isHistory = false,
                onCancel = { onCancelBooking(booking.id) }
            )
        }
    }
}

// ------------------------------------------------------
// 🔥 История
// ------------------------------------------------------
@Composable
fun HistorySection(
    bookings: List<Booking>,
    isLoading: Boolean,
    statusFilter: String,
    onStatusFilterChange: (String) -> Unit,
    onClearClick: () -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFFA500))
        }
        return
    }

    Spacer(Modifier.height(4.dp))

    HistoryFilterBar(
        currentFilter = statusFilter,
        onFilterChange = onStatusFilterChange,
        onClearClick = if (bookings.isNotEmpty()) onClearClick else null
    )

    if (bookings.isEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            "История пуста",
            color = Color(0xFF8895BF),
            fontSize = 14.sp
        )
        return
    }

    Spacer(Modifier.height(6.dp))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(bookings, key = { it.id }) { booking ->
            BookingCard(booking = booking, isHistory = true)
        }
    }
}

@Composable
fun HistoryFilterBar(
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    onClearClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryFilterChip(
                text = "Все",
                selected = currentFilter == "all",
                onClick = { onFilterChange("all") }
            )
            HistoryFilterChip(
                text = "Завершённые",
                selected = currentFilter == "completed",
                onClick = { onFilterChange("completed") }
            )
            HistoryFilterChip(
                text = "Отменённые",
                selected = currentFilter == "cancelled",
                onClick = { onFilterChange("cancelled") }
            )
            HistoryFilterChip(
                text = "Истёкшие",
                selected = currentFilter == "expired",
                onClick = { onFilterChange("expired") }
            )
        }

        if (onClearClick != null) {
            Text(
                text = "Очистить",
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { onClearClick() }
                    .padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun HistoryFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0x3300C6FF) else Color(0x22081426)
    val borderColor = if (selected) Color(0xFF00C6FF) else Color(0xFF374151)

    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFFEBFAFF) else Color(0xFF9CA3AF),
            fontSize = 12.sp
        )
    }
}

// ------------------------------------------------------
// 🔥 Карточка брони + кнопка "Отменить"
// ------------------------------------------------------
@Composable
fun BookingCard(
    booking: Booking,
    isHistory: Boolean,
    onCancel: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "arrowRotation"
    )

    val astanaTz = remember { TimeZone.getTimeZone("Asia/Almaty") }
    val dateFormat = remember {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).apply {
            timeZone = astanaTz
        }
    }

    val startStr = booking.startTime?.toDate()?.let { dateFormat.format(it) } ?: "—"
    val endStr = booking.endTime?.toDate()?.let { dateFormat.format(it) } ?: "—"
    val packageLabel = booking.packageName ?: booking.timePackageName ?: "—"

    val statusColor = when (booking.status) {
        "cancelled" -> Color(0xFFFF6B6B)
        "completed" -> Color(0xFF22C55E)
        "expired" -> Color(0xFF9CA3AF)
        "active" -> Color(0xFF00E5FF)
        else -> Color(0xFFCBD5F5)
    }

    val statusText = when (booking.status) {
        "cancelled" -> "Отменена"
        "completed" -> "Завершена"
        "expired" -> "Истекла"
        "active" -> "Активна"
        else -> booking.status
    }

    val priceValue = booking.totalPrice ?: booking.price
    val cardBg = if (isHistory) Color(0xFF050B18) else Color(0xFF020814)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (booking.computerCategory) {
                                "ВИП", "VIP" -> Color(0xFFFFD700)
                                "Bootcamp" -> Color(0xFF22C55E)
                                else -> Color(0xFF00C6FF)
                            }
                        )
                )

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = booking.computerName.ifBlank { "ПК (без имени)" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = booking.computerCategory.ifBlank { "Категория не указана" },
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (priceValue != null) {
                        Text(
                            text = "${priceValue.toInt()} ₸",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Начало",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = startStr,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Конец",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                    Text(
                        text = endStr,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x151F2937)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.graphicsLayer {
                            rotationZ = rotation
                        }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                exit = fadeOut(tween(180)) + shrinkVertically(tween(150))
            ) {
                Column {
                    InfoRow("Пакет", packageLabel)

                    InfoRow("Почта", booking.userEmail ?: "—")

                    val createdStr = booking.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "—"
                    InfoRow("Создано", createdStr)

                    if (!isHistory && booking.status == "active") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Напоминание: по завершении бронь автоматически перейдёт в историю.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        if (onCancel != null) {
                            Button(
                                onClick = onCancel,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF4B6B)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Отменить бронь", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color(0xFFE5E7EB),
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, false)
        )
    }
}
