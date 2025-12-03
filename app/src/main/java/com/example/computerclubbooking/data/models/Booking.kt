package com.example.computerclubbooking.data.models

import com.google.firebase.Timestamp

data class Booking(
    val id: String = "",
    val computerId: String = "",
    val computerName: String = "",
    val computerCategory: String = "",
    val userEmail: String = "",
    val userId: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val status: String = "active",
    val totalPrice: Double = 0.0,
    val timePackage: String = ""
)