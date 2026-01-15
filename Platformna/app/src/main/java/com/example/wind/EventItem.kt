package com.example.wind

data class EventItem(
    val id: String,
    val topic: String,
    val message: String,
    val timestamp: String,
    val lat: Double,
    val lon: Double
)