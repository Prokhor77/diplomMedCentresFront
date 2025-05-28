package com.mgkct.diplom.SudoAdmin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {
    val userEmail: String? = "prokhorodinets@gmail.com" // Получите из профиля пользователя

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    var lastReportType: String = ""

    fun generatePersonalReport(
        period: Int,
        format: String,
        email: String,
        context: Context
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            lastReportType = "personal"
            try {
                val response = RetrofitInstance.api.generatePersonalReport(
                    ApiService.AllCentersReportRequest(
                        period_days = period,
                        email = email,
                        format = format
                    )
                )
                if (response.isSuccessful) {
                    Toast.makeText(context, "Отчет отправлен на $email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ошибка: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateAllCentersReport(
        period: Int,
        format: String,
        email: String,
        context: Context
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            lastReportType = "all"
            try {
                val response = RetrofitInstance.api.generateAllCentersReport(
                    ApiService.AllCentersReportRequest(
                        period_days = period,
                        email = email,
                        format = format
                    )
                )
                if (response.isSuccessful) {
                    Toast.makeText(context, "Общий отчет отправлен на $email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ошибка: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFromSudoAdmScreen(
    viewModel: ReportViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current

    val periodOptions = listOf(1, 7, 30)
    val formatOptions = listOf("docx", "pdf")

    var selectedPeriod by remember { mutableStateOf(7) }
    var selectedFormat by remember { mutableStateOf("docx") }
    var useUserEmail by remember { mutableStateOf(true) }
    var customEmail by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
         Image(
             painter = painterResource(id = R.drawable.background_image),
             contentDescription = "Background",
             modifier = Modifier.fillMaxSize(),
             contentScale = ContentScale.Crop
         )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Отчеты") }
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

                        // Период
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
                            periodOptions.forEach { days ->
                                PeriodButton(
                                    text = when (days) {
                                        1 -> "1 день"
                                        7 -> "7 дней"
                                        30 -> "30 дней"
                                        else -> "$days дн."
                                    },
                                    isSelected = selectedPeriod == days,
                                    onClick = { selectedPeriod = days }
                                )
                            }
                        }

                        // Формат
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
                            formatOptions.forEach { format ->
                                FormatButton(
                                    text = format.uppercase(),
                                    isSelected = selectedFormat == format,
                                    onClick = { selectedFormat = format }
                                )
                            }
                        }

                        // Email
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
                                selected = useUserEmail,
                                onClick = { useUserEmail = true }
                            )
                            Text(
                                text = "Мой email (${viewModel.userEmail ?: "не задан"})",
                                modifier = Modifier
                                    .clickable { useUserEmail = true }
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
                                selected = !useUserEmail,
                                onClick = { useUserEmail = false }
                            )
                            Text(
                                text = "Другой email:",
                                modifier = Modifier
                                    .clickable { useUserEmail = false }
                                    .padding(start = 8.dp)
                            )
                        }
                        if (!useUserEmail) {
                            OutlinedTextField(
                                value = customEmail,
                                onValueChange = { customEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = "Email")
                                }
                            )
                        }

                        // Кнопка только для общего отчета!
                        Button(
                            onClick = {
                                viewModel.generateAllCentersReport(
                                    period = selectedPeriod,
                                    format = selectedFormat,
                                    email = if (useUserEmail) viewModel.userEmail ?: "" else customEmail,
                                    context = context
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading && viewModel.lastReportType == "all") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Сгенерировать и отправить общий отчет")
                            }
                        }
                    }
                }

                // Информация об отчете
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