package com.mgkct.diplom

import com.google.gson.annotations.SerializedName
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

    // В интерфейсе ApiService добавьте:
    @GET("/feedbacks")
    suspend fun getFeedbacks(): List<Feedback>

    @POST("/feedbacks/{id}/approve")
    suspend fun approveFeedback(@Path("id") id: Int)

    @POST("/feedbacks/{id}/reject")
    suspend fun rejectFeedback(@Path("id") id: Int, @Body reason: RejectReason)

    @GET("inpatient-cares")
    suspend fun getInpatientCares(
        @Query("med_center_id") medCenterId: Int,
        @Query("active") active: String
    ): List<InpatientCareResponse>

    @POST("inpatient-cares")
    suspend fun createInpatientCare(
        @Body care: InpatientCareCreate,
        @Query("med_center_id") medCenterId: Int
    ): InpatientCareResponse

    @PATCH("inpatient-cares/{careId}")
    suspend fun updateInpatientCare(
        @Path("careId") careId: Int,
        @Query("active") active: String
    )

    @GET("users/search")
    suspend fun searchUsers(
        @Query("med_center_id") medCenterId: Int,
        @Query("query") query: String
    ): List<UserSearchResult>

    @GET("/med-centers")
    suspend fun getMedicalCenters(): List<MedicalCenter>

    @PUT("/med-centers/{id}")
    suspend fun updateMedicalCenter(
        @Path("id") id: Int,
        @Body center: MedicalCenter
    ): Response<ResponseBody>

    @POST("/med-centers")
    suspend fun createMedicalCenter(@Body center: MedicalCenter): MedicalCenter

    @DELETE("/med-centers/{id}")
    suspend fun deleteMedicalCenter(@Path("id") id: Int): Response<ResponseBody>

    data class MedicalCenter(
        @SerializedName("idCenter") val id: Int,
        @SerializedName("centerName") val name: String,
        @SerializedName("centerDescription") val description: String?,
        @SerializedName("centerAddress") val address: String,
        @SerializedName("centerNumber") val phone: String
    ) {
        fun toCreateRequest() = MedicalCenterCreate(
            centerName = name,
            centerDescription = description,
            centerAddress = address,
            centerNumber = phone
        )
    }

    data class MedicalCenterCreate(
        val centerName: String,
        val centerDescription: String?,
        val centerAddress: String,
        val centerNumber: String
    )

    @GET("/doctor/appointments")
    suspend fun getDoctorAppointments(
        @Query("doctorId") doctorId: Int,
        @Query("date") date: String,
        @Query("active") active: String?
    ): List<AppointmentResponse>

    data class AppointmentResponse(
        @SerializedName("id") val id: Int,
        @SerializedName("userId") val userId: Int,
        @SerializedName("fullName") val fullName: String,
        @SerializedName("time") val time: String,
        @SerializedName("reason") val reason: String?,
        @SerializedName("active") val active: String
    )

    // Модели данных
    data class InpatientCareResponse(
        @SerializedName("id") val id: Int,
        @SerializedName("userFullName") val userFullName: String?,
        @SerializedName("userId") val userId: Int,
        @SerializedName("floor") val floor: Int,
        @SerializedName("ward") val ward: Int,
        @SerializedName("receipt_date") val receiptDate: String?,  // Изменено на String
        @SerializedName("expire_date") val expireDate: String?,    // Изменено на String
        @SerializedName("active") val active: String
    )

    data class UserSearchResult(
        @SerializedName("id") val id: Int,
        @SerializedName("fullName") val fullName: String
    )

    data class InpatientCareCreate(
        val userId: Int,
        val floor: Int,
        val ward: Int,
        val receipt_date: String, // Изменено на Long для timestamp
        val expire_date: String // Или можно оставить String если нужно
    )

    data class RejectReason(val reason: String)

    // И добавьте модель Feedback:
    data class Feedback(
        val id: Int,
        val userId: Int,
        val doctorId: Int,
        val medCenterId: Int,
        val grade: Int,
        val description: String,
        val active: String, // "in_progress", "true", "false"
        val reason: String? = null
    )

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
        val centerName: String? = null,
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