package com.example.computerclubbooking.uii

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Модель пакета часов
data class TimePackage(
    val name: String,
    val hours: Int,
    val price: Double,
    val isNightPackage: Boolean = false
)

// Модель брони с временем окончания
data class Booking(
    val id: String = "",
    val computerId: String = "",
    val computerName: String = "",
    val computerCategory: String = "",
    val userEmail: String = "",
    val userId: String = "",
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = "active", // active, completed, cancelled
    val totalPrice: Double = 0.0,
    val timePackage: String = ""
)

sealed class BookingResult {
    data class Success(val message: String) : BookingResult()
    data class Failure(val message: String) : BookingResult()
    object InProgress : BookingResult()
}

class BookingViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    companion object {
        // Пакеты часов по категориям
        val STANDARD_PACKAGES = listOf(
            TimePackage("1 час", 1, 900.0),
            TimePackage("2+1 часов", 3, 1800.0),
            TimePackage("3+2 часов", 5, 2700.0),
            TimePackage("Ночной пакет", 10, 3000.0, true)
        )

        val VIP_PACKAGES = listOf(
            TimePackage("1 час", 1, 1400.0),
            TimePackage("2+1 часов", 3, 2800.0),
            TimePackage("3+2 часов", 5, 4200.0),
            TimePackage("Ночной пакет", 10, 4500.0, true)
        )

        val BOOTCAMP_PACKAGES = listOf(
            TimePackage("1 час", 1, 1400.0),
            TimePackage("2+1 часов", 3, 2800.0),
            TimePackage("3+2 часов", 5, 4200.0),
            TimePackage("Ночной пакет", 10, 4500.0, true)
        )
    }

    private val _bookingState = MutableStateFlow<BookingResult?>(null)
    val bookingState: StateFlow<BookingResult?> = _bookingState

    private val _activeBookings = MutableStateFlow<Map<String, Booking>>(emptyMap())
    val activeBookings: StateFlow<Map<String, Booking>> = _activeBookings

    init {
        loadActiveBookings()
        startExpiredBookingsCleanup() // 🔥 АВТОМАТИЧЕСКАЯ ОЧИСТКА
    }

    // 🔥 НОВАЯ ФУНКЦИЯ: Автоматическая очистка истекших броней
    private fun startExpiredBookingsCleanup() {
        viewModelScope.launch {
            while (true) {
                delay(30000) // Проверяем каждые 30 секунд
                cleanupExpiredBookings()
            }
        }
    }

    // 🔥 НОВАЯ ФУНКЦИЯ: Очистка истекших броней
    private suspend fun cleanupExpiredBookings() {
        try {
            val now = Timestamp.now()

            // Находим все активные брони, которые истекли
            val expiredBookings = db.collection("bookings")
                .whereEqualTo("status", "active")
                .whereLessThan("endTime", now)
                .get()
                .await()

            println("🧹 Найдено истекших броней: ${expiredBookings.documents.size}")

            // Обновляем каждую истекшую бронь
            expiredBookings.documents.forEach { doc ->
                val computerId = doc.getString("computerId") ?: return@forEach

                db.runTransaction { transaction ->
                    // Обновляем статус брони на "completed"
                    val bookingRef = db.collection("bookings").document(doc.id)
                    transaction.update(bookingRef, "status", "completed")

                    // Освобождаем компьютер
                    val computerRef = db.collection("computers").document(computerId)
                    transaction.update(computerRef, "isAvailable", true)
                }.await()

                println("✅ Компьютер $computerId освобожден")
            }
        } catch (e: Exception) {
            println("❌ Ошибка очистки: ${e.message}")
        }
    }

    private fun loadActiveBookings() {
        viewModelScope.launch {
            db.collection("bookings")
                .whereEqualTo("status", "active")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("❌ Error loading bookings: ${error.message}")
                        return@addSnapshotListener
                    }

                    try {
                        val currentTime = System.currentTimeMillis()

                        val bookingsMap = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                val computerId = doc.getString("computerId") ?: return@mapNotNull null

                                val startTime = doc.getTimestamp("startTime") ?: Timestamp.now()
                                val endTime = doc.getTimestamp("endTime") ?: Timestamp.now()

                                // 🔥 ПРОВЕРКА: Пропускаем истекшие брони
                                if (endTime.toDate().time < currentTime) {
                                    println("⏰ Бронь ${doc.id} истекла, пропускаем")
                                    return@mapNotNull null
                                }

                                computerId to Booking(
                                    id = doc.id,
                                    computerId = computerId,
                                    computerName = doc.getString("computerName") ?: "",
                                    computerCategory = doc.getString("computerCategory") ?: "",
                                    userEmail = doc.getString("userEmail") ?: "",
                                    userId = doc.getString("userId") ?: "",
                                    startTime = startTime,
                                    endTime = endTime,
                                    totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                                    timePackage = doc.getString("timePackage") ?: ""
                                )
                            } catch (e: Exception) {
                                println("⚠️ Ошибка обработки брони ${doc.id}: ${e.message}")
                                null
                            }
                        }?.toMap() ?: emptyMap()

                        _activeBookings.value = bookingsMap

                    } catch (e: Exception) {
                        println("❌ Critical error in bookings listener: ${e.message}")
                    }
                }
        }
    }

    fun getPackagesForCategory(category: String): List<TimePackage> {
        return when (category) {
            "ВИП" -> VIP_PACKAGES
            "Bootcamp" -> BOOTCAMP_PACKAGES
            else -> STANDARD_PACKAGES
        }
    }

    fun bookComputer(
        userId: String,
        userEmail: String,
        computerId: String,
        computerName: String,
        computerCategory: String,
        timePackage: TimePackage,
        selectedStartTime: Calendar
    ) {
        viewModelScope.launch {
            _bookingState.value = BookingResult.InProgress

            try {
                // 🔥 ИСПРАВЛЕНО: Используем локальный часовой пояс
                val localTimeZone = TimeZone.getDefault()

                val startCalendar = Calendar.getInstance(localTimeZone).apply {
                    timeInMillis = selectedStartTime.timeInMillis
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val endCalendar = Calendar.getInstance(localTimeZone).apply {
                    timeInMillis = startCalendar.timeInMillis
                    add(Calendar.HOUR_OF_DAY, timePackage.hours)
                }

                println("🕐 Локальное время начала: ${SimpleDateFormat("dd.MM.yyyy HH:mm z", Locale.getDefault()).format(startCalendar.time)}")
                println("🕐 Локальное время окончания: ${SimpleDateFormat("dd.MM.yyyy HH:mm z", Locale.getDefault()).format(endCalendar.time)}")

                // Для ночного пакета проверяем время начала
                if (timePackage.isNightPackage) {
                    val selectedHour = startCalendar.get(Calendar.HOUR_OF_DAY)
                    if (selectedHour != 22) {
                        _bookingState.value = BookingResult.Failure("❌ Ночной пакет можно бронировать только на 22:00")
                        return@launch
                    }
                }

                val startTime = startCalendar.time
                val endTime = endCalendar.time

                // Проверка на корректность времени
                val now = System.currentTimeMillis()
                if (startTime.time < now) {
                    _bookingState.value = BookingResult.Failure("❌ Нельзя бронировать на прошедшее время")
                    return@launch
                }

                // 🔥 УПРОЩЕННАЯ ПРОВЕРКА ПЕРЕСЕЧЕНИЙ
                val activeBookings = db.collection("bookings")
                    .whereEqualTo("computerId", computerId)
                    .whereEqualTo("status", "active")
                    .get()
                    .await()

                val overlapFound = activeBookings.documents.any { doc ->
                    val existingStart = doc.getTimestamp("startTime")?.toDate()?.time ?: 0
                    val existingEnd = doc.getTimestamp("endTime")?.toDate()?.time ?: 0

                    // Проверяем пересечение временных интервалов
                    (startTime.time < existingEnd && endTime.time > existingStart)
                }

                if (overlapFound) {
                    _bookingState.value = BookingResult.Failure("❌ Этот компьютер уже забронирован на выбранное время")
                    return@launch
                }

                // Создание брони
                db.runTransaction { transaction ->
                    val compRef = db.collection("computers").document(computerId)
                    val compSnap = transaction.get(compRef)

                    if (!compSnap.getBoolean("isAvailable")!!) {
                        throw IllegalStateException("computer_unavailable")
                    }

                    val newRef = db.collection("bookings").document()
                    val booking = Booking(
                        id = newRef.id,
                        computerId = computerId,
                        computerName = computerName,
                        computerCategory = computerCategory,
                        userEmail = userEmail,
                        userId = userId,
                        startTime = Timestamp(startTime),
                        endTime = Timestamp(endTime),
                        totalPrice = timePackage.price,
                        timePackage = timePackage.name
                    )
                    transaction.set(newRef, booking)
                    transaction.update(compRef, "isAvailable", false)
                }.await()

                val timeFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
                _bookingState.value = BookingResult.Success(
                    "✅ Бронирование успешно!\nПакет: ${timePackage.name}\nНачало: ${timeFormat.format(startTime)}\nКонец: ${timeFormat.format(endTime)}\nСтоимость: ${timePackage.price} ₸"
                )

            } catch (e: Exception) {
                println("❌ Booking error: ${e.message}")
                _bookingState.value = BookingResult.Failure("❌ Ошибка бронирования: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    fun getBookingEndTime(computerId: String): Date? {
        return _activeBookings.value[computerId]?.endTime?.toDate()
    }

    fun isComputerBooked(computerId: String): Boolean {
        val booking = _activeBookings.value[computerId]
        if (booking != null) {
            // Проверяем, не истекла ли бронь
            val now = System.currentTimeMillis()
            if (booking.endTime.toDate().time < now) {
                return false // Бронь истекла
            }
        }
        return booking != null
    }

    fun clearState() {
        _bookingState.value = null
    }
}