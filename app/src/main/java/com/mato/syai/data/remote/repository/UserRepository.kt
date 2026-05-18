package com.mato.syai.data.remote.repository

import com.mato.syai.data.remote.network.RetrofitInstance

class UserRepository {

    suspend fun createUser(token: String) =
        RetrofitInstance.api.createUser("Bearer $token")

    suspend fun getUser(token: String) =
        RetrofitInstance.api.getUser("Bearer $token")
}