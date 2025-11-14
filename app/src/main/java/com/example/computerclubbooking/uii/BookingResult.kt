package com.example.computerclubbooking.uii

// Результаты операции бронирования
sealed class BookingResult {
    object InProgress : BookingResult()
    data class Success(val message: String) : BookingResult()
    data class Failure(val message: String) : BookingResult()
}