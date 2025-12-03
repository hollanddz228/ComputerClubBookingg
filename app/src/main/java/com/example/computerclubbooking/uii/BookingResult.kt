package com.example.computerclubbooking.uii

sealed class BookingResult {
    object Idle : BookingResult()
    object InProgress : BookingResult()
    data class Success(val message: String) : BookingResult()
    data class Failure(val message: String) : BookingResult()
}
