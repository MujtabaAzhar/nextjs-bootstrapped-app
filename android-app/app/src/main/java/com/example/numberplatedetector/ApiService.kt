package com.example.numberplatedetector

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// Data classes for request/response
data class PlateRequest(
    val plateNumber: String
)

data class ApiResponse(
    val success: Boolean,
    val message: String
)

// Retrofit API interface
interface PlateApi {
    @POST("plate-detection.php")
    fun sendPlate(@Body request: PlateRequest): Call<ApiResponse>
}

object ApiService {
    // TODO: Replace with your actual API base URL
    private const val BASE_URL = "https://your-server.com/api/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: PlateApi = retrofit.create(PlateApi::class.java)

    fun postPlate(
        plateNumber: String,
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val request = PlateRequest(plateNumber)
        
        api.sendPlate(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                when (response.code()) {
                    200 -> {
                        val apiResponse = response.body()
                        if (apiResponse?.success == true) {
                            onSuccess(200)
                        } else {
                            onFailure(apiResponse?.message ?: "Unknown error")
                        }
                    }
                    404 -> onSuccess(404) // Not found
                    else -> onFailure("Server returned code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                onFailure("Network error: ${t.message}")
            }
        })
    }
}
