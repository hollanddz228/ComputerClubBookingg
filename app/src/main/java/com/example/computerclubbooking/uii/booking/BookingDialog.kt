package com.example.computerclubbooking.uii.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.computerclubbooking.uii.home.Computer
import com.example.computerclubbooking.data.models.TimePackage
import com.example.computerclubbooking.components.PackageItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BookingDialog(
    computer: Computer,
    bookingViewModel: BookingViewModel,
    onConfirm: (TimePackage, Calendar) -> Unit,
    onCancel: () -> Unit
) {
    val astanaTZ = remember { TimeZone.getTimeZone("Asia/Almaty") }
    val df = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    df.timeZone = astanaTZ

    var loading by remember { mutableStateOf(true) }
    var serverCal by remember { mutableStateOf(Calendar.getInstance(astanaTZ)) }

    var selectedDate by remember { mutableStateOf(Calendar.getInstance(astanaTZ)) }
    var selectedHour by remember { mutableStateOf(12) }
    var selectedPackage by remember { mutableStateOf<TimePackage?>(null) }

    // ---------- ЗАГРУЗКА СЕРВЕРНОГО ВРЕМЕНИ ----------
    LaunchedEffect(Unit) {
        loading = true
        val srv = bookingViewModel.getServerTime()
        serverCal = srv.clone() as Calendar
        selectedDate = srv.clone() as Calendar
        selectedHour = srv.get(Calendar.HOUR_OF_DAY)
        loading = false
    }

    if (loading) {
        Dialog(onDismissRequest = {}) {
            Box(
                Modifier
                    .size(140.dp)
                    .background(Color(0xFF050B1E), MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00C6FF))
            }
        }
        return
    }

    val categoryColor = when (computer.category) {
        "ВИП" -> Color(0xFFFFD700)
        "Bootcamp" -> Color(0xFF00E676)
        else -> Color(0xFF00C6FF)
    }

    val neonBrush = Brush.linearGradient(listOf(categoryColor, Color(0xFF007AFF)))
    val packages = bookingViewModel.getPackagesForCategory(computer.category)

    // ---------- НОЧНОЙ ПАКЕТ ----------
    LaunchedEffect(selectedPackage) {
        if (selectedPackage?.isNightPackage == true) {
            selectedHour = 22
        }
    }

    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .background(neonBrush, MaterialTheme.shapes.large)
                .padding(2.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color(0xFF050B1E)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    Text("🎮 ${computer.name}", color = Color.White, fontSize = 22.sp)
                    Text(computer.category, color = categoryColor, fontSize = 15.sp)

                    Spacer(Modifier.height(15.dp))

                    // ПАКЕТЫ
                    Text("📦 Выберите пакет:", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))

                    packages.forEach { pkg ->
                        PackageItem(
                            timePackage = pkg,
                            isSelected = selectedPackage == pkg,
                            onSelect = { selectedPackage = pkg },
                            categoryColor = categoryColor
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(15.dp))

                    // ВРЕМЯ НАЧАЛА
                    Text("⏰ Время начала:", color = Color.White, fontSize = 16.sp)
                    Text("Дата: ${df.format(selectedDate.time)}", color = Color.LightGray)

                    Spacer(Modifier.height(8.dp))

                    val todayString = df.format(serverCal.time)
                    val selectedString = df.format(selectedDate.time)

                    val isToday = todayString == selectedString

                    // 🔥 если ночной пакет — показываем только 22:00
                    val hoursList = if (selectedPackage?.isNightPackage == true) {
                        listOf(22)
                    } else {
                        (0..23).toList()
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(hoursList) { hour ->
                            val disabled =
                            // для ночного пакета сюда уже не зайдём,
                                // потому что список = listOf(22)
                                (isToday && hour < serverCal.get(Calendar.HOUR_OF_DAY))

                            Box(
                                modifier = Modifier
                                    .background(
                                        if (hour == selectedHour && !disabled)
                                            categoryColor
                                        else if (disabled)
                                            Color.DarkGray
                                        else
                                            Color.Transparent,
                                        MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .clickable(enabled = !disabled) {
                                        selectedHour = hour
                                    }
                            ) {
                                Text("%02d:00".format(hour), color = Color.White)
                            }
                        }
                    }


                    Spacer(Modifier.height(16.dp))

                    // ИНТЕРВАЛ
                    selectedPackage?.let { pkg ->
                        val start = Calendar.getInstance(astanaTZ).apply {
                            timeInMillis = selectedDate.timeInMillis
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, 0)
                        }

                        val end = (start.clone() as Calendar).apply {
                            add(Calendar.HOUR_OF_DAY, pkg.hours)
                        }

                        Text("📅 Интервал:", color = Color.LightGray)
                        Text(
                            "${df.format(start.time)}  %02d:00 — %02d:00".format(
                                start.get(Calendar.HOUR_OF_DAY),
                                end.get(Calendar.HOUR_OF_DAY)
                            ),
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // КНОПКИ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onCancel) {
                            Text("Отмена", color = Color.Gray)
                        }

                        Button(
                            enabled = selectedPackage != null,
                            onClick = {
                                val pkg = selectedPackage ?: return@Button

                                val start = Calendar.getInstance(astanaTZ).apply {
                                    timeInMillis = selectedDate.timeInMillis
                                    set(Calendar.HOUR_OF_DAY, selectedHour)
                                    set(Calendar.MINUTE, 0)
                                }

                                onConfirm(pkg, start)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = categoryColor)
                        ) {
                            Text("Оплатить ${selectedPackage?.price ?: 0.0} ₸")
                        }
                    }
                }
            }
        }
    }
}
