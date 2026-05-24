package com.mato.syai.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import com.mato.syai.data.remote.model.MessageResponse
import com.mato.syai.data.remote.model.UserResponse

interface UserApi {

    @POST("user")
    suspend fun createUser(
        @Header("Authorization") token: String
    ): Response<MessageResponse>

    @GET("user")
    suspend fun getUser(
        @Header("Authorization") token: String
    ): Response<UserResponse>
}