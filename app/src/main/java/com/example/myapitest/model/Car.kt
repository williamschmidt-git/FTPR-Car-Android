package com.example.myapitest.model

data class CarResponse(
    val id: String,
    val value: Car
)

data class Car(
    val id: String,
    val imageUrl: String,
    val year: String,
    val name: String,
    val licence: String,
    val place: CarLocation
)

data class Place(
    val lat: Double,
    val long: Double
)

data class CarLocation(
    val lat: Double,
    val long: Double,
)