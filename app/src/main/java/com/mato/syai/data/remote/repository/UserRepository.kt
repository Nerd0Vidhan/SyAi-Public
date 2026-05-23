package com.mato.syai.data.remote.repository

import android.util.Log
import com.mato.syai.data.remote.network.RetrofitInstance

class UserRepository {

    suspend fun createUser() {

        try {

            val response = RetrofitInstance.api.createUser()

            Log.d("API", "Code: ${response.code()}")
            Log.d("API", "Body: ${response.body()}")

        } catch (e: Exception) {

            Log.e("API", "Create User Failed", e)
        }
    }

    suspend fun getUser() =
        RetrofitInstance.api.getUser()
}