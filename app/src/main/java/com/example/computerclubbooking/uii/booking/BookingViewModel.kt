package com.example.computerclubbooking.uii.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.computerclubbooking.data.models.Booking
import com.example.computerclubbooking.data.models.TimePackage
import com.example.computerclubbooking.uii.BookingResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

class BookingViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val astanaTZ = TimeZone.getTimeZone("Asia/Almaty")

    private val _bookingState = MutableStateFlow<BookingResult>(BookingResult.Idle)
    val bookingState: StateFlow<BookingResult> = _bookingState

    private val _activeBookings = MutableStateFlow<Map<String, Booking>>(emptyMap())
    val activeBookings: StateFlow<Map<String, Booking>> = _activeBookings

    init {
        startListeningBookings()
    }

    // 🔥 Слушаем активные брони
    private fun startListeningBookings() {
        db.collection("bookings")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, _ ->

                val active = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val booking = Booking(
                            id = doc.id,
                            computerId = doc.getString("computerId")!!,
                            computerName = doc.getString("computerName")!!,
                            computerCategory = doc.getString("computerCategory")!!,
                            userEmail = doc.getString("userEmail")!!,
                            userId = doc.getString("userId")!!,
                            startTime = doc.getTimestamp("startTime"),
                            endTime = doc.getTimestamp("endTime"),
                            status = doc.getString("status") ?: "active",
                            totalPrice = doc.getDouble("totalPrice") ?: 0.0,
                            timePackage = doc.getString("timePackage")!!
                        )
                        booking.computerId to booking
                    } catch (e: Exception) {
                        null
                    }
                }?.toMap() ?: emptyMap()

                _activeBookings.value = active
            }
    }

    // 🔥 Полные данные для пакетов
    fun getPackagesForCategory(category: String): List<TimePackage> {
        return when (category) {
            "ВИП" -> listOf(
                TimePackage("2 часа", 2, 2000.0),
                TimePackage("4 часа", 4, 3500.0),
                TimePackage("Ночной пакет", 10, 5000.0, isNightPackage = true)
            )
            "Bootcamp" -> listOf(
                TimePackage("3 часа", 3, 2500.0),
                TimePackage("5 часов", 5, 4000.0),
                TimePackage("Ночной пакет", 12, 5500.0, isNightPackage = true)
            )
            else -> listOf(
                TimePackage("1 час", 1, 900.0),
                TimePackage("3 часа", 3, 1800.0),
                TimePackage("3+2 часов", 5, 2700.0),
                TimePackage("Ночной пакет", 10, 3000.0, isNightPackage = true)
            )
        }
    }

    // 🔥 ОСНОВНОЕ: Получаем серверное время Firestore
    suspend fun getServerTime(): Calendar {
        val ref = db.collection("serverTime").document("now")

        ref.set(mapOf("t" to FieldValue.serverTimestamp())).await()
        val snap = ref.get().await()
        val ts = snap.getTimestamp("t") ?: Timestamp.Companion.now()

        return Calendar.getInstance(astanaTZ).apply {
            time = ts.toDate()
        }
    }

    // 🔥 Создание бронирования
    fun bookComputer(
        userId: String,
        userEmail: String,
        computerId: String,
        computerName: String,
        computerCategory: String,
        timePackage: TimePackage,
        desiredStartTime: Calendar
    ) = viewModelScope.launch {

        try {
            _bookingState.value = BookingResult.InProgress

            val startTs = Timestamp(desiredStartTime.time)
            val endCal = (desiredStartTime.clone() as Calendar).apply {
                add(Calendar.HOUR_OF_DAY, timePackage.hours)
            }
            val endTs = Timestamp(endCal.time)

            val bookingData = mapOf(
                "computerId" to computerId,
                "computerName" to computerName,
                "computerCategory" to computerCategory,
                "userId" to userId,
                "userEmail" to userEmail,
                "startTime" to startTs,
                "endTime" to endTs,
                "status" to "active",
                "timePackage" to timePackage.name,
                "totalPrice" to timePackage.price,
                "status" to "active"
            )

            db.collection("bookings").add(bookingData).await()

            _bookingState.value = BookingResult.Success("Бронь успешно создана!")

        } catch (e: Exception) {
            _bookingState.value = BookingResult.Failure("Ошибка бронирования: ${e.message}")
        }
    }

    fun clearState() {
        _bookingState.value = BookingResult.Idle
    }
}

