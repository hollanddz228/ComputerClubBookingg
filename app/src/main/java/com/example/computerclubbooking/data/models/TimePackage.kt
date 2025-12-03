package com.example.computerclubbooking.data.models

data class TimePackage(
    val name: String,
    val hours: Int,
    val price: Double,
    val isNightPackage: Boolean = false
)