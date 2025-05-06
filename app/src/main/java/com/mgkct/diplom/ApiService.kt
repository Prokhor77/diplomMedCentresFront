package com.mgkct.diplom

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {
    @POST("/login-with-key")
    suspend fun loginWithKey(@Body request: LicenseKeyRequest): LoginResponse

    @GET("/users")
    suspend fun getUsers(): List<User>

    @POST("/users")
    suspend fun addUser(@Body user: User): User

    @PUT("/users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: User): User

    @DELETE("/users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<ResponseBody>

    data class LicenseKeyRequest(val key: String)

    data class LoginResponse(
        val role: String?,
        val userId: Int?,
        val medCenterId: Int?,
        val full_name: String?,
        val center_name: String?
    )

    data class User(
        val id: Int = 0,
        val key: String,
        val role: String,
        val medCenterId: Int?,
        val fullName: String?,
        val email: String?,
        val address: String?,
        val tgId: Int?,
        val centerName: String? = null
    )
}

object RetrofitInstance {
    private const val BASE_URL = "http://10.0.2.2:8080"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}