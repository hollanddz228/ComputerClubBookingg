package com.example.computerclubbooking

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.computerclubbooking.uii.BookingViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Модель данных компьютера 💻
data class Computer(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Стандарт",
    val isAvailable: Boolean = true
)

// ViewModel для списка ПК
class MainViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _computers = MutableStateFlow<List<Computer>>(emptyList())
    val computers: StateFlow<List<Computer>> = _computers

    init {
        viewModelScope.launch {
            db.collection("computers").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                _computers.value = snapshot?.documents?.map { doc ->
                    Computer(
                        id = doc.id,
                        name = doc.getString("name") ?: "PC",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: "Стандарт",
                        isAvailable = doc.getBoolean("isAvailable") ?: true
                    )
                } ?: emptyList()
            }
        }
    }
}

// Главный экран 🎮
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    bookingViewModel: BookingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val computers by mainViewModel.computers.collectAsState()
    val bookingState by bookingViewModel.bookingState.collectAsState()
    val activeBookings by bookingViewModel.activeBookings.collectAsState()

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val user = auth.currentUser

    var selectedComputer by remember { mutableStateOf<Computer?>(null) }

    val categories = listOf("Стандарт", "ВИП", "Bootcamp")
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Стандарт") }

    // 🎯 Реакция на результат брони
    LaunchedEffect(bookingState) {
        when (val state = bookingState) {
            is com.example.computerclubbooking.uii.BookingResult.Success -> {
                Toast.makeText(context, "✅ ${state.message}", Toast.LENGTH_SHORT).show()
                selectedComputer = null
                bookingViewModel.clearState()
            }
            is com.example.computerclubbooking.uii.BookingResult.Failure -> {
                Toast.makeText(context, "❌ ${state.message}", Toast.LENGTH_SHORT).show()
                bookingViewModel.clearState()
            }
            is com.example.computerclubbooking.uii.BookingResult.InProgress -> {
                // Показываем загрузку если нужно
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Computer Club",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    Box {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню", tint = Color.White)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0D1117)
                )
            )
        },
        containerColor = Color(0xFF0D1117)
    ) { padding ->
        val filteredComputers = computers.filter { it.category == selectedCategory }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredComputers) { computer ->
                ComputerCard(
                    computer = computer,
                    bookingViewModel = bookingViewModel,
                    onBookClick = {
                        if (computer.isAvailable && !bookingViewModel.isComputerBooked(computer.id)) {
                            selectedComputer = computer
                        } else {
                            Toast.makeText(context, "Этот компьютер занят ❌", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // 💡 Диалог бронирования
        selectedComputer?.let { computer ->
            com.example.computerclubbooking.uii.BookingDialog(
                computer = computer,
                bookingViewModel = bookingViewModel,
                onConfirm = { timePackage, startTime ->
                    if (user != null) {
                        bookingViewModel.bookComputer(
                            userId = user.uid,
                            userEmail = user.email ?: "unknown",
                            computerId = computer.id,
                            computerName = computer.name,
                            computerCategory = computer.category,
                            timePackage = timePackage,
                            selectedStartTime = startTime
                        )
                    } else {
                        Toast.makeText(context, "Ошибка: войдите в аккаунт", Toast.LENGTH_SHORT).show()
                    }
                },
                onCancel = { selectedComputer = null }
            )
        }
    }
}

// Карточка ПК 💻
// Карточка ПК 💻
// Карточка ПК 💻
// Карточка ПК 💻
// 🔥 ЗАМЕНИ ФУНКЦИЮ ComputerCard в твоем MainScreen.kt на эту:

@Composable
fun ComputerCard(
    computer: Computer,
    bookingViewModel: BookingViewModel,
    onBookClick: () -> Unit
) {
    val isBooked = bookingViewModel.isComputerBooked(computer.id)
    val bookingEndTime = bookingViewModel.getBookingEndTime(computer.id)

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // 🔥 ИСПРАВЛЕНО: Используем локальное время для расчета
    val currentTime = Calendar.getInstance().timeInMillis

    val statusText = if (!isBooked && computer.isAvailable) {
        "Свободен ✅"
    } else if (isBooked && bookingEndTime != null) {
        "Занят до ${timeFormat.format(bookingEndTime)} ❌"
    } else {
        "Занят ❌"
    }

    val statusColor = if (!isBooked && computer.isAvailable) {
        Color(0xFF66FF99)
    } else {
        Color(0xFFFF6666)
    }

    val isAvailableForBooking = computer.isAvailable && !isBooked

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBooked) Color(0xFF2A1F1F) else Color(0xFF161B22)
        ),
        elevation = CardDefaults.cardElevation(6.dp),
        border = if (isBooked) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Иконка
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.ic_computer),
                    contentDescription = "ПК",
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = computer.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = computer.description,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )

            // Статус
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // 🔥 ИСПРАВЛЕНО: Оставшееся время с учетом часового пояса
            if (isBooked && bookingEndTime != null) {
                val timeLeft = bookingEndTime.time - currentTime

                if (timeLeft > 0) {
                    val hoursLeft = timeLeft / (1000 * 60 * 60)
                    val minutesLeft = (timeLeft % (1000 * 60 * 60)) / (1000 * 60)

                    Text(
                        text = "Осталось: ${hoursLeft}ч ${minutesLeft}м",
                        color = Color(0xFFFFA500),
                        fontSize = 10.sp
                    )
                } else {
                    Text(
                        text = "Истекло ⏰",
                        color = Color.Red,
                        fontSize = 10.sp
                    )
                }
            }

            Button(
                onClick = onBookClick,
                enabled = isAvailableForBooking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAvailableForBooking) Color(0xFF007AFF) else Color.Gray
                )
            ) {
                Text(
                    if (isAvailableForBooking) "Забронировать" else "Занят",
                    color = Color.White
                )
            }
        }
    }
}