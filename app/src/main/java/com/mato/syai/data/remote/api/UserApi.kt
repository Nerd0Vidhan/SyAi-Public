package com.mato.syai.data.remote.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import com.mato.syai.data.remote.model.MessageResponse
import com.mato.syai.data.remote.model.UserResponse

interface UserApi {

    @POST("api/v1/users")
    suspend fun createUser(): Response<MessageResponse>

//    @GET("api/v1/users")
//    suspend fun getUser(): Response<UserResponse>
}