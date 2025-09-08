package com.example.myapitest.service

import com.example.myapitest.model.Car
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("car") suspend fun getCars(): List<Car>
    @GET("car/{id}") suspend fun getCar(@Path("id") id: String): Car
}