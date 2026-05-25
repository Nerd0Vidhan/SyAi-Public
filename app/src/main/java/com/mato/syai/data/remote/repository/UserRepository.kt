package com.mato.syai.data.remote.repository

import android.util.Log
import com.mato.syai.data.remote.network.RetrofitInstance
import javax.inject.Inject

class UserRepository @Inject constructor() {

    suspend fun createUser(): Result<Unit> {
        return try {
            val response = RetrofitInstance.api.createUser()

            Log.d("API", "Code: ${response.code()}")
            Log.d("API", "Body: ${response.body()}")

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception("API Error: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Log.e("API", "Create User Failed", e)
            Result.failure(e)
        }
    }
}