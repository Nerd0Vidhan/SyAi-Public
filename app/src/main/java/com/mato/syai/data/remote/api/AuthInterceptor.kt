package com.mato.syai.data.remote.network

import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {

override fun intercept(chain: Interceptor.Chain): Response {

    val requestBuilder = chain.request().newBuilder()

    val user = FirebaseAuth.getInstance().currentUser

    val token = user?.let {
        com.google.android.gms.tasks.Tasks.await(it.getIdToken(false)).token
    }

    if (!token.isNullOrEmpty()) {
        requestBuilder.addHeader("Authorization", "Bearer $token")
    }

    return chain.proceed(requestBuilder.build())
}
}