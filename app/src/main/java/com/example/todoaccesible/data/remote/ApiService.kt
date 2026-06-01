package com.example.todoaccesible.data.remote

import com.example.todoaccesible.data.remote.model.LoginRequest
import com.example.todoaccesible.data.remote.model.LoginResponse
import com.example.todoaccesible.data.remote.model.RegisterRequest
import com.example.todoaccesible.data.remote.model.RegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
}
