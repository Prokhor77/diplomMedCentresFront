package com.mgkct.diplom.Admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.LoginActivity
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.launch
import retrofit2.http.*


class UserReportViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
    var selectedPeriod by mutableStateOf(7)
    var selectedFormat by mutableStateOf("docx")
    var useUserEmail by mutableStateOf(true)
    var customEmail by mutableStateOf(TextFieldValue(""))
    var userEmail by mutableStateOf<String?>(null)

    var searchQuery by mutableStateOf("")
    var searchResults by mutableStateOf<List<ApiService.UserSearchResult>>(emptyList())
    var selectedUser by mutableStateOf<ApiService.UserSearchResult?>(null)

    fun searchUsers(medCenterId: Int) {
        viewModelScope.launch {
            try {
                if (searchQuery.isNotBlank()) {
                    val results = RetrofitInstance.api.searchUsers(medCenterId, searchQuery)
                    searchResults = results
                } else {
                    searchResults = emptyList()
                }
            } catch (_: Exception) {
                searchResults = emptyList()
            }
        }
    }

    fun selectUser(user: ApiService.UserSearchResult) {
        selectedUser = user
        searchResults = emptyList()
        searchQuery = user.fullName
        loadUserEmail(user.id)
    }

    fun loadUserEmail(userId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getUserEmail(userId)
                if (response.isSuccessful) {
                    userEmail = response.body()?.email
                } else {
                    userEmail = null
                }
            } catch (_: Exception) {
                userEmail = null
            }
        }
    }

    fun generateUserReport(context: Context) {
        val userId = selectedUser?.id ?: return
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
                val request = ApiService.UserReportRequest(
                    user_id = userId,
                    period_days = selectedPeriod,
                    email = email,
                    format = selectedFormat
                )
                val response = RetrofitInstance.api.generateUserReport(request)
                if (response.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Отчет будет отправлен на $email",
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

// --- Вспомогательные Composable-функции ---

@Composable
fun UserPeriodButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        modifier = modifier
    ) {
        Text(text)
    }
}

@Composable
fun UserFormatButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        modifier = modifier
    ) {
        Text(text)
    }
}

@Composable
fun UserBulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text("• ", fontSize = 16.sp)
        Text(text, fontSize = 16.sp)
    }
}

// --- Основной экран ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserReportScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: UserReportViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val medCenterId = remember {
        context.getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("medCenterId", 0)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Отчет по пользователю") },
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
                // --- Поиск пользователя ---
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = {
                        viewModel.searchQuery = it
                        viewModel.searchUsers(medCenterId)
                    },
                    label = { Text("Поиск пользователя (ФИО или email)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.searchQuery = ""
                                viewModel.searchResults = emptyList()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Очистить"
                                )
                            }
                        }
                    }
                )
                // Список результатов поиска
                if (viewModel.searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column {
                            viewModel.searchResults.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectUser(user) }
                                        .padding(8.dp)
                                ) {
                                    Text("${user.fullName} (${user.role})")
                                }
                            }
                        }
                    }
                }

                // --- Если пользователь выбран, показываем форму отчёта ---
                viewModel.selectedUser?.let { selectedUser ->
                    Spacer(modifier = Modifier.height(16.dp))
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
                                text = "Пользователь: ${selectedUser.fullName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // Период
                            Text("Выберите период:", fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                UserPeriodButton(
                                    text = "1 день",
                                    isSelected = viewModel.selectedPeriod == 1,
                                    onClick = { viewModel.selectedPeriod = 1 }
                                )
                                UserPeriodButton(
                                    text = "7 дней",
                                    isSelected = viewModel.selectedPeriod == 7,
                                    onClick = { viewModel.selectedPeriod = 7 }
                                )
                                UserPeriodButton(
                                    text = "30 дней",
                                    isSelected = viewModel.selectedPeriod == 30,
                                    onClick = { viewModel.selectedPeriod = 30 }
                                )
                            }
                            // Формат
                            Text("Выберите формат:", fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                UserFormatButton(
                                    text = "DOCX",
                                    isSelected = viewModel.selectedFormat == "docx",
                                    onClick = { viewModel.selectedFormat = "docx" }
                                )
                                UserFormatButton(
                                    text = "PDF",
                                    isSelected = viewModel.selectedFormat == "pdf",
                                    onClick = { viewModel.selectedFormat = "pdf" }
                                )
                            }
                            // Email
                            Text("Отправить отчет на:", fontWeight = FontWeight.Bold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = viewModel.useUserEmail,
                                    onClick = { viewModel.useUserEmail = true }
                                )
                                Text(
                                    text = "Email пользователя (${viewModel.userEmail ?: "не найден"})",
                                    modifier = Modifier
                                        .clickable { viewModel.useUserEmail = true }
                                        .padding(start = 8.dp)
                                )
                            }
                            Row(
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
                                        .padding(bottom = 8.dp),
                                    leadingIcon = {
                                        Icon(Icons.Default.Email, contentDescription = "Email")
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    textStyle = TextStyle(fontSize = 16.sp)
                                )
                            }
                            Button(
                                onClick = { viewModel.generateUserReport(context) },
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
                }
            }
        }
    }
}