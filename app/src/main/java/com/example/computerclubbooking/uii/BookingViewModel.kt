package com.example.computerclubbooking.uii

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    val status: String = "active",
    val totalPrice: Double = 0.0,
    val timePackage: String = ""
)

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
                        val bookingsMap = snapshot?.documents?.associate { doc ->
                            val computerId = doc.getString("computerId") ?: ""

                            // 🔥 БЕЗОПАСНОЕ ПОЛУЧЕНИЕ TIMESTAMP
                            val startTime = try {
                                doc.getTimestamp("startTime") ?: Timestamp.now()
                            } catch (e: Exception) {
                                println("⚠️ Invalid startTime in booking ${doc.id}")
                                Timestamp.now()
                            }

                            val endTime = try {
                                doc.getTimestamp("endTime") ?: Timestamp.now()
                            } catch (e: Exception) {
                                println("⚠️ Invalid endTime in booking ${doc.id}")
                                Timestamp.now()
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
                        } ?: emptyMap()

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
                val startCalendar = selectedStartTime.clone() as Calendar
                val endCalendar = selectedStartTime.clone() as Calendar

                // Для ночного пакета проверяем время начала
                if (timePackage.isNightPackage) {
                    val selectedHour = startCalendar.get(Calendar.HOUR_OF_DAY)
                    if (selectedHour != 22) {
                        _bookingState.value = BookingResult.Failure("❌ Ночной пакет можно бронировать только на 22:00")
                        return@launch
                    }
                }

                // Устанавливаем время окончания
                endCalendar.add(Calendar.HOUR, timePackage.hours)

                val startTime = startCalendar.time
                val endTime = endCalendar.time

                // Проверка на корректность времени
                val now = Calendar.getInstance()
                if (startTime.before(now.time)) {
                    _bookingState.value = BookingResult.Failure("❌ Нельзя бронировать на прошедшее время")
                    return@launch
                }

                // 🔥 УПРОЩЕННАЯ ПРОВЕРКА ПЕРЕСЕЧЕНИЙ (без сложных запросов)
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
        return _activeBookings.value.containsKey(computerId)
    }

    fun clearState() {
        _bookingState.value = null
    }
}