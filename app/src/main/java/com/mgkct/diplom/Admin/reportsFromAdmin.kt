package com.mgkct.diplom.Admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Add these to your ApiService interface
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

// Add these functions to your ApiService interface
/*
@POST("reports/generate")
suspend fun generateReport(@Body request: ReportRequest): Response<ReportResponse>

@GET("users/{userId}/email")
suspend fun getUserEmail(@Path("userId") userId: Int): Response<UserEmailResponse>
*/

class ReportsViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var selectedPeriod by mutableStateOf(7) // Default to 7 days
    var selectedFormat by mutableStateOf("docx") // Default to DOCX
    var useUserEmail by mutableStateOf(true) // Default to using user's email
    var customEmail by mutableStateOf(TextFieldValue(""))
    var userEmail by mutableStateOf<String?>(null)

    fun loadUserEmail(userId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getUserEmail(userId)
                userEmail = response.body()?.email
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun generateReport(medCenterId: Int, context: Context) {
        if (!useUserEmail && customEmail.text.isEmpty()) {
            Toast.makeText(context, "Пожалуйста, введите email", Toast.LENGTH_SHORT).show()
            return
        }

        val email = if (useUserEmail) userEmail ?: "" else customEmail.text

        if (email.isEmpty()) {
            Toast.makeText(context, "Email не найден", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        viewModelScope.launch {
            try {
                val request = ReportRequest(
                    med_center_id = medCenterId,
                    period_days = selectedPeriod,
                    email = email,
                    format = selectedFormat
                )

                val response = RetrofitInstance.api.generateReport(request)

                if (response.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Отчет будет отправлен на ${email}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Ошибка: ${response.errorBody()?.string() ?: "Неизвестная ошибка"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Ошибка: ${e.message ?: "Неизвестная ошибка"}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsFromAdminScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    val userId = sharedPref.getInt("userId", 0)
    val medCenterId = sharedPref.getInt("medCenterId", 0)

    val viewModel: ReportsViewModel = viewModel()

    LaunchedEffect(userId) {
        if (userId > 0) {
            viewModel.loadUserEmail(userId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Отчеты") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Генерация отчета",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Period selection
                        Text(
                            text = "Выберите период:",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PeriodButton(
                                text = "1 день",
                                isSelected = viewModel.selectedPeriod == 1,
                                onClick = { viewModel.selectedPeriod = 1 }
                            )

                            PeriodButton(
                                text = "7 дней",
                                isSelected = viewModel.selectedPeriod == 7,
                                onClick = { viewModel.selectedPeriod = 7 }
                            )

                            PeriodButton(
                                text = "30 дней",
                                isSelected = viewModel.selectedPeriod == 30,
                                onClick = { viewModel.selectedPeriod = 30 }
                            )
                        }

                        // Format selection
                        Text(
                            text = "Выберите формат:",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FormatButton(
                                text = "DOCX",
                                isSelected = viewModel.selectedFormat == "docx",
                                onClick = { viewModel.selectedFormat = "docx" }
                            )

                            FormatButton(
                                text = "PDF",
                                isSelected = viewModel.selectedFormat == "pdf",
                                onClick = { viewModel.selectedFormat = "pdf" }
                            )
                        }

                        // Email selection
                        Text(
                            text = "Отправить отчет на:",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.useUserEmail,
                                onClick = { viewModel.useUserEmail = true }
                            )

                            Text(
                                text = "Мой email (${viewModel.userEmail ?: "не найден"})",
                                modifier = Modifier
                                    .clickable { viewModel.useUserEmail = true }
                                    .padding(start = 8.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !viewModel.useUserEmail,
                                onClick = { viewModel.useUserEmail = false }
                            )

                            Text(
                                text = "Другой email:",
                                modifier = Modifier
                                    .clickable { viewModel.useUserEmail = false }
                                    .padding(start = 8.dp)
                            )
                        }

                        if (!viewModel.useUserEmail) {
                            OutlinedTextField(
                                value = viewModel.customEmail,
                                onValueChange = { viewModel.customEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = "Email")
                                }
                            )
                        }

                        // Generate button
                        Button(
                            onClick = { viewModel.generateReport(medCenterId, context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !viewModel.isLoading
                        ) {
                            if (viewModel.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Сгенерировать и отправить отчет")
                            }
                        }
                    }
                }

                // Information about the report
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Информация об отчете",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "Отчет содержит следующую информацию:",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        BulletPoint("Общая статистика приемов")
                        BulletPoint("Финансовая статистика")
                        BulletPoint("Статистика по врачам")
                        BulletPoint("Рейтинги врачей")
                        BulletPoint("Статистика отзывов")
                        BulletPoint("Ежедневная статистика за период")

                        Text(
                            text = "Отчет будет отправлен на указанный email в течение нескольких минут.",
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}

@Composable
fun FormatButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            modifier = Modifier.padding(end = 8.dp, top = 0.dp),
            fontSize = 16.sp
        )
        Text(text = text)
    }
}
