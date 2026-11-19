package com.example.routemate.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var sessionManager: SessionManager? = null
    @Volatile
    private var apiServiceInstance: ApiService? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        sessionManager = SessionManager.getInstance(context)
        ApiConstants.init(context)
    }

    fun getApiService(): ApiService {
        return apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: buildRetrofit().create(ApiService::class.java).also {
                apiServiceInstance = it
            }
        }
    }

    fun refreshApiService() {
        synchronized(this) {
            apiServiceInstance = null
        }
    }

    fun resolveMediaUrl(urlOrPath: String?): String? {
        if (urlOrPath.isNullOrBlank()) return null
        if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
            return urlOrPath
        }
        val cleanPath = urlOrPath.removePrefix("/")
        val base = ApiConstants.BASE_URL
        return if (base.endsWith("/")) base + cleanPath else "$base/$cleanPath"
    }

    private fun buildRetrofit(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor {
            sessionManager?.getToken()
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
