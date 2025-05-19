package com.mgkct.diplom.doctor

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.LoginActivity
import com.mgkct.diplom.LoginScreen
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainDoctorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "doctor") {
                composable("doctor") {
                    MainDoctorScreen(navController = navController)
                }
                composable("login_screen") { LoginScreen(navController) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDoctorScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
    val fullName = sharedPref.getString("fullName", "") ?: ""
    val centerName = sharedPref.getString("centerName", "") ?: ""
    val doctorId = sharedPref.getInt("userId", 0)
    val medCenterId = sharedPref.getInt("medCenterId", 0)

    val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

    var pendingCount by remember { mutableStateOf(0) }
    var completedCount by remember { mutableStateOf(0) }
    var allAppointments by remember { mutableStateOf<List<ApiService.AppointmentResponse>>(emptyList()) }
    var filteredAppointments by remember { mutableStateOf<List<ApiService.AppointmentResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Функция для загрузки данных
    fun loadData() {
        isRefreshing = true
        error = null

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pendingResponse = RetrofitInstance.api.getDoctorAppointments(
                    doctorId = doctorId,
                    date = today,
                    active = "true"
                )
                val completedResponse = RetrofitInstance.api.getDoctorAppointments(
                    doctorId = doctorId,
                    date = today,
                    active = "false"
                )
                val appsResponse = RetrofitInstance.api.getDoctorAppointments(
                    doctorId = doctorId,
                    date = today,
                    active = null
                )

                withContext(Dispatchers.Main) {
                    pendingCount = pendingResponse.size
                    completedCount = completedResponse.size
                    allAppointments = appsResponse.filter { it.active == "true" }
                    filteredAppointments = allAppointments
                    isLoading = false
                    isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.message
                    isLoading = false
                    isRefreshing = false
                }
            }
        }
    }

    // Функция для фильтрации записей
    fun filterAppointments(query: String) {
        filteredAppointments = if (query.isEmpty()) {
            allAppointments
        } else {
            allAppointments.filter { appointment ->
                appointment.fullName.contains(query, ignoreCase = true) ||
                        appointment.reason?.contains(query, ignoreCase = true) ?: false ||
                        appointment.time.contains(query, ignoreCase = true)
            }
        }
    }

    // Первоначальная загрузка данных
    LaunchedEffect(Unit) {
        loadData()
    }

    // При изменении поискового запроса фильтруем записи
    LaunchedEffect(searchQuery) {
        filterAppointments(searchQuery)
    }

    var expandedMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = "Фон",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.mipmap.medical),
                                contentDescription = "Иконка",
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(fullName)
                        }
                    },
                    actions = {
                        IconButton(onClick = { expandedMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Главная") },
                                onClick = { navController.navigate("doctor") },
                                leadingIcon = {
                                    Icon(Icons.Default.Home, contentDescription = "Главная")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Профиль") },
                                onClick = { /* TODO */ },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = "Профиль")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Выйти") },
                                onClick = {
                                    with(sharedPref.edit()) {
                                        clear()
                                        apply()
                                    }
                                    navController.navigate("login_screen") { popUpTo(0) }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Выход")
                                }
                            )
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
                onRefresh = { loadData() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading && !isRefreshing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Ошибка: $error", color = Color.Red)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PatientStatsCard(
                                title = "Ожидают приема",
                                count = pendingCount,
                                color = Color(0xFFFFA726),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            PatientStatsCard(
                                title = "Принято сегодня",
                                count = completedCount,
                                color = Color(0xFF66BB6A),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Записи на прием:",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Добавленная строка поиска
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Поиск пациентов") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))


                        if (filteredAppointments.isEmpty()) {
                            if (searchQuery.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Ничего не найдено",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Нет записей на сегодня",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredAppointments) { appointment ->
                                    AppointmentCard(
                                        patientName = appointment.fullName,
                                        time = appointment.time,
                                        reason = appointment.reason ?: "Не указана"
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientStatsCard(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}


@Composable
fun AppointmentCard(patientName: String, time: String, reason: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ФИО: $patientName",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = "Время: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = "Причина: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Добавленная кнопка "Начать прием"
            Spacer(modifier = Modifier.height(0.dp))
            Button(
                onClick = {
                    // Здесь будет логика начала приема
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Начать прием")
            }
        }
    }
}