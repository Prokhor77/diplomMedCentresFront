package com.mgkct.diplom

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
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

    // Add to your ApiService interface
    @GET("/users/{userId}/qrcode")
    suspend fun getUserQrCode(@Path("userId") userId: Int): QrCodeResponse

    data class QrCodeResponse(val path: String)

    @GET("/med-centers/{centerId}/doctors/average-rating")
    suspend fun getAverageDoctorRating(@Path("centerId") centerId: Int): AverageRatingResponse

    data class AverageRatingResponse(val average_rating: Double?)

    @PATCH("/users/{id}/education")
    suspend fun updateEducation(@Path("id") id: Int)

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

    @GET("/doctor/appointments/range")
    suspend fun getDoctorAppointmentsRange(
        @Query("doctorId") doctorId: Int,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): List<ApiService.AppointmentResponse>

    data class AppointmentResponse(
        @SerializedName("id") val id: Int,
        @SerializedName("userId") val userId: Int,
        @SerializedName("fullName") val fullName: String,
        @SerializedName("time") val time: String,
        @SerializedName("date") val date: String,
        @SerializedName("reason") val reason: String?,
        @SerializedName("active") val active: String
    )

    @POST("/appointments")
    suspend fun createAppointment(
        @Query("doctorId") doctorId: Int,
        @Query("userId") userId: Int,
        @Query("date") date: String,
        @Query("time") time: String,
        @Query("reason") reason: String?
    ): Response<Unit>

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
        @SerializedName("fullName") val fullName: String,
        @SerializedName("role") val role: String
    )

    @GET("records/patient/{user_id}")
    suspend fun getPatientRecords(
        @Path("user_id") userId: Int
    ): List<RecordResponse>

    data class RecordPhotoResponse(
        val id: Int,
        val photo_path: String
    )

    data class RecordResponse(
        val id: Int,
        val userId: Int,
        val doctorId: Int,
        val doctorFullName: String?,   // новое поле
        val doctorWorkType: String?,
        val description: String?,
        val assignment: String?,
        val paidOrFree: String,
        val price: Int?,
        val time_start: String,
        val time_end: String?,
        val medCenterId: Int,
        val photos: List<RecordPhotoResponse>
    )

    data class InpatientCareCreate(
        val userId: Int,
        val floor: Int,
        val ward: Int,
        val receipt_date: String, // Изменено на Long для timestamp
        val expire_date: String // Или можно оставить String если нужно
    )

    @GET("/doctors/{doctor_id}/free-slots")
    suspend fun getDoctorFreeSlots(@Path("doctor_id") doctorId: Int): List<FreeSlot>

    data class FreeSlot(
        val id: Int,
        val date: String,
        val time: String,
        val reason: String?
    )

    @DELETE("/appointments/{id}")
    suspend fun deleteAppointment(@Path("id") id: Int): Response<Unit>

    @GET("/doctors/{doctor_id}/info")
    suspend fun getDoctorInfo(@Path("doctor_id") doctorId: Int): DoctorInfo

    data class DoctorInfo(
        @SerializedName("work_type") val workType: String?,
        @SerializedName("category") val category: String?
    )

    @PATCH("appointments/{id}")
    suspend fun updateAppointmentStatus(
        @Path("id") id: Int,
        @Query("active") active: String,
        @Query("userId") userId: Int,
        @Query("reason") reason: String? = null
    ): Response<Unit>

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
        val work_type: String? = null,
        val experience: String? = null,
        val category: String? = null,
        val education: String? = null
    )
    @POST("records")
    suspend fun createRecord(@Body record: RecordCreate): Response<Unit>

    @PATCH("appointments/{id}")
    suspend fun updateAppointmentStatus(
        @Path("id") id: Int,
        @Query("active") active: String,
        @Query("clear_data") clearData: Boolean = false
    ): Response<Unit>

    @POST("/tg-bind/start")
    suspend fun tgBindStart(@Query("user_id") userId: Int): TgBindCodeResponse

    @GET("/users/{userId}/tg-id")
    suspend fun getUserTgId(@Path("userId") userId: Int): TgIdResponse

    data class RecordCompleteNotifyRequest(
        val user_id: Int,
        val doctor_id: Int,
        val description: String,
        val assignment: String,
        val paid_or_free: String,
        val price: Int?,
        val date: String,
        val time: String,
        val photo_urls: List<String>
    )
    @POST("/notify/record-complete")
    suspend fun notifyRecordComplete(@Body body: RecordCompleteNotifyRequest): Response<Unit>

    @GET("/stats/appointments-today")
    suspend fun getAppointmentsToday(@Query("med_center_id") medCenterId: Int): CountResponse

    @GET("/stats/doctors-count")
    suspend fun getDoctorsCount(@Query("med_center_id") medCenterId: Int): CountResponse

    @POST("/feedbacks")
    suspend fun createFeedback(@Body feedback: FeedbackCreate): ApiService.Feedback

    data class FeedbackCreate(
        val userId: Int,
        val doctorId: Int,
        val medCenterId: Int,
        val grade: Int,
        val description: String,
        val active: String = "in_progress"
    )


    @GET("/med-centers/with-rating")
    suspend fun getMedCentersWithRating(): List<MedCenterWithRating>

    data class MedCenterWithRating(
        @SerializedName("idCenter") val id: Int,
        @SerializedName("centerName") val name: String,
        @SerializedName("centerAddress") val address: String,
        @SerializedName("centerNumber") val phone: String,
        @SerializedName("average_rating") val averageRating: Double?
    )

    @GET("/med-centers/{center_id}/doctors/with-rating")
    suspend fun getDoctorsWithRating(@Path("center_id") centerId: Int): List<DoctorWithRating>

    data class DoctorWithRating(
        @SerializedName("doctorId") val id: Int,
        @SerializedName("fullName") val fullName: String,
        @SerializedName("work_type") val workType: String?,
        @SerializedName("category") val category: String?,
        @SerializedName("average_rating") val averageRating: Double?
    )

    @GET("/doctors/{doctor_id}/feedbacks")
    suspend fun getDoctorFeedbacks(@Path("doctor_id") doctorId: Int): List<DoctorFeedback>

    data class DoctorFeedback(
        val id: Int,
        val grade: Int,
        val description: String,
        val userFullName: String
    )

    @GET("/stats/inpatient-patients-count")
    suspend fun getInpatientPatientsCount(@Query("med_center_id") medCenterId: Int): CountResponse

    @GET("/stats/average-appointment-time")
    suspend fun getAverageAppointmentTime(@Query("med_center_id") medCenterId: Int): AverageTimeResponse

    data class AverageTimeResponse(val average_time_minutes: Double)

    @GET("/stats/income-today")
    suspend fun getIncomeToday(@Query("med_center_id") medCenterId: Int): IncomeTodayResponse

    data class IncomeTodayResponse(val income: Int)

    @GET("/stats/paid-free-counts")
    suspend fun getPaidFreeCounts(@Query("med_center_id") medCenterId: Int): PaidFreeCountsResponse

    data class PaidFreeCountsResponse(val paid: Int, val free: Int)

    @GET("/stats/feedbacks-in-progress")
    suspend fun getFeedbacksInProgress(@Query("med_center_id") medCenterId: Int): CountResponse

    @GET("/stats/feedbacks-week")
    suspend fun getFeedbacksWeek(@Query("med_center_id") medCenterId: Int): CountResponse

    data class CountResponse(val count: Int)

    data class TgIdResponse(val tgId: Int?)
    @POST("/tg-bind/unlink")
    suspend fun tgUnlink(@Body request: TgUnlinkRequest)
    data class TgUnlinkRequest(val user_id: Int)

    data class TgBindCodeResponse(val code: String)

    @Multipart
    @POST("records/upload-photos")
    suspend fun uploadPhotos(@Part files: List<MultipartBody.Part>): Response<UploadPhotosResponse>

    @GET("/stats/appointments-month")
    suspend fun getAppointmentsMonth(
        @Query("med_center_id") medCenterId: Int
    ): CountResponse

    @GET("/stats/appointments-by-date")
    suspend fun getAppointmentsByDate(
        @Query("med_center_id") medCenterId: Int,
        @Query("date") date: String
    ): CountResponse

    @GET("/stats/appointments-week")
    suspend fun getAppointmentsWeek(): List<DayCount>

    @GET("/stats/inpatient-week")
    suspend fun getInpatientWeek(): List<DayCount>

    data class ReportRequest(
        val med_center_id: Int,
        val period_days: Int,  // 1, 7, or 30
        val email: String,
        val format: String = "docx"  // "docx" or "pdf"
    )

    data class ReportResponse(
        val message: String
    )

    data class UserEmailResponse(
        val email: String
    )

    @POST("reports/generate")
    suspend fun generateReport(@Body request: com.mgkct.diplom.Admin.ReportRequest): Response<ReportResponse>

    @GET("users/{userId}/email")
    suspend fun getUserEmail(@Path("userId") userId: Int): Response<UserEmailResponse>

    data class DayCount(
        val date: String, // "2024-06-01"
        val count: Int
    )

    data class UploadPhotosResponse(val paths: List<String>)

    data class RecordCreate(
        @SerializedName("userId") val userId: Int,
        @SerializedName("doctorId") val doctorId: Int,
        @SerializedName("description") val description: String,
        @SerializedName("assignment") val assignment: String,
        @SerializedName("paidOrFree") val paidOrFree: String,
        @SerializedName("price") val price: Int?,
        @SerializedName("time_start") val timeStart: String,
        @SerializedName("time_end") val timeEnd: String,
        @SerializedName("medCenterId") val medCenterId: Int,
        @SerializedName("photos") val photoPaths: List<String> = emptyList()
    )
}

object RetrofitInstance {
    const val BASE_URL = "http://10.0.2.2:8080"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}