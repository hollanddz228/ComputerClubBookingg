package com.example.computerclubbooking.uii

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.computerclubbooking.Computer
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingDialog(
    computer: Computer,
    bookingViewModel: BookingViewModel,
    onConfirm: (TimePackage, Calendar) -> Unit,
    onCancel: () -> Unit
) {
    val packages = bookingViewModel.getPackagesForCategory(computer.category)
    var selectedPackage by remember { mutableStateOf<TimePackage?>(null) }
    var showSmsDialog by remember { mutableStateOf(false) }
    var smsCode by remember { mutableStateOf("") }

    // Время бронирования
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedHour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(0) }

    // 🔥 НЕОНОВАЯ АНИМАЦИЯ
    val categoryColor = when(computer.category) {
        "ВИП" -> Color(0xFFFFD700) to Color(0xFFFFA500)
        "Bootcamp" -> Color(0xFF00FF00) to Color(0xFF00CC00)
        else -> Color(0xFF007AFF) to Color(0xFF00C6FF)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    val neonBrush = Brush.linearGradient(
        listOf(categoryColor.first.copy(alpha = glowAlpha), categoryColor.second)
    )

    // Обновляем время при выборе пакета
    LaunchedEffect(selectedPackage) {
        selectedPackage?.let { pkg ->
            if (pkg.isNightPackage) {
                selectedHour = 22
                selectedMinute = 0
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 22)
                calendar.set(Calendar.MINUTE, 0)
                if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 22) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                selectedDate = calendar
            }
        }
    }

    if (showSmsDialog) {
        SmsVerificationDialog(
            smsCode = smsCode,
            onSmsCodeChange = { smsCode = it },
            onVerify = {
                selectedPackage?.let { pkg ->
                    // Устанавливаем выбранное время
                    val bookingTime = Calendar.getInstance().apply {
                        time = selectedDate.time
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                    }
                    onConfirm(pkg, bookingTime)
                    showSmsDialog = false
                }
            },
            onCancel = {
                showSmsDialog = false
                smsCode = ""
            }
        )
    } else {
        Dialog(onDismissRequest = onCancel) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(neonBrush)
                    .padding(2.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp),
                    color = Color(0xFF0D1B3D),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // ЗАГОЛОВОК
                        Text(
                            text = "🎮 ${computer.name}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )

                        Text(
                            text = computer.category,
                            color = categoryColor.second,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ВЫБОР ПАКЕТА
                        Text(
                            text = "📦 Выберите пакет:",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(packages) { pkg ->
                                PackageItem(
                                    timePackage = pkg,
                                    isSelected = selectedPackage == pkg,
                                    onSelect = {
                                        selectedPackage = pkg
                                        // Для ночного пакета автоматически ставим 22:00
                                        if (pkg.isNightPackage) {
                                            selectedHour = 22
                                            selectedMinute = 0
                                        }
                                    },
                                    categoryColor = categoryColor.second
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ВЫБОР ВРЕМЕНИ
                        Text(
                            text = "⏰ Выберите время начала:",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Выбор даты
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Дата:", color = Color(0xFFB0C4DE))
                            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                            Text(
                                text = dateFormat.format(selectedDate.time),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Выбор часа
                        Text("Час:", color = Color(0xFFB0C4DE))
                        Spacer(modifier = Modifier.height(4.dp))

                        // Горизонтальный список часов
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items((8..23).toList()) { hour ->
                                val isSelected = selectedHour == hour
                                val isDisabled = selectedPackage?.isNightPackage == true && hour != 22

                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when {
                                                isSelected -> categoryColor.second
                                                isDisabled -> Color.Gray.copy(alpha = 0.3f)
                                                else -> Color.Transparent
                                            },
                                            shape = CircleShape
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "$hour:00",
                                        color = when {
                                            isSelected -> Color.White
                                            isDisabled -> Color.Gray
                                            else -> Color.White
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.clickable(enabled = !isDisabled) {
                                            if (!isDisabled) {
                                                selectedHour = hour
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Информация о бронировании
                        selectedPackage?.let { pkg ->
                            Spacer(modifier = Modifier.height(16.dp))

                            val startCalendar = Calendar.getInstance().apply {
                                time = selectedDate.time
                                set(Calendar.HOUR_OF_DAY, selectedHour)
                                set(Calendar.MINUTE, selectedMinute)
                            }
                            val endCalendar = startCalendar.clone() as Calendar
                            endCalendar.add(Calendar.HOUR, pkg.hours)

                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())

                            Column {
                                Text(
                                    text = "📅 Время брони:",
                                    color = Color(0xFFB0C4DE),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${dateFormat.format(startCalendar.time)} ${timeFormat.format(startCalendar.time)} - ${timeFormat.format(endCalendar.time)}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                if (pkg.isNightPackage) {
                                    Text(
                                        text = "🌙 Ночной пакет (22:00 - 08:00)",
                                        color = Color(0xFFFFA500),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // КНОПКИ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onCancel) {
                                Text("Отмена", color = Color.Gray)
                            }

                            Button(
                                onClick = {
                                    if (selectedPackage != null) {
                                        showSmsDialog = true
                                    }
                                },
                                enabled = selectedPackage != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = categoryColor.second
                                )
                            ) {
                                Text("💳 Оплатить ${selectedPackage?.price ?: 0} ₸")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackageItem(
    timePackage: TimePackage,
    isSelected: Boolean,
    onSelect: () -> Unit,
    categoryColor: Color
) {
    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) categoryColor.copy(alpha = 0.2f) else Color(0xFF1E2A3D)
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = timePackage.name,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${timePackage.hours} часов",
                    color = Color(0xFFB0C4DE),
                    fontSize = 14.sp
                )
                if (timePackage.isNightPackage) {
                    Text(
                        text = "🌙 Только на 22:00",
                        color = Color(0xFFFFA500),
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                text = "${timePackage.price} ₸",
                color = categoryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SmsVerificationDialog(
    smsCode: String,
    onSmsCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = Color(0xFF0D1B3D),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "📱 Подтверждение оплаты",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Введите код из SMS:",
                    color = Color(0xFFB0C4DE)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = smsCode,
                    onValueChange = onSmsCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0000", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E2A3D),
                        unfocusedContainerColor = Color(0xFF1E2A3D),
                        focusedIndicatorColor = Color(0xFF007AFF),
                        unfocusedIndicatorColor = Color(0xFF2D3748)
                    ),
                    singleLine = true,
                    maxLines = 1
                )

                Text(
                    text = "Код: 1234 (для демо)",
                    color = Color(0xFFFFA500),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Отмена", color = Color.Gray)
                    }

                    Button(
                        onClick = onVerify,
                        enabled = smsCode == "1234",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C6FF)
                        )
                    ) {
                        Text("Подтвердить")
                    }
                }
            }
        }
    }
}