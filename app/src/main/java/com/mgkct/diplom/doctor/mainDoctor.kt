package com.mgkct.diplom.doctor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
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
                composable("emc_search") {  // Add this new route
                    SearchEMCScreen(navController = navController)
                }
                composable("manageTalons") { ManageTalonsScreen(navController) }
                composable("manageInpCareFromDoc") { ManageInpCareFromDocScreen(navController) }
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
    var showAddAppointmentDialog by remember { mutableStateOf(false) }

    val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

    var pendingCount by remember { mutableStateOf(0) }
    var completedCount by remember { mutableStateOf(0) }
    var allAppointments by remember { mutableStateOf<List<ApiService.AppointmentResponse>>(emptyList()) }
    var filteredAppointments by remember { mutableStateOf<List<ApiService.AppointmentResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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
                    pendingCount = pendingResponse
                        .filter { it.userId != null && it.userId > 0 }
                        .size

                    completedCount = completedResponse
                        .filter { it.userId != null && it.userId > 0 }
                        .size
                    allAppointments = appsResponse.filter {
                        it.active == "true" &&
                                it.userId != null &&
                                it.userId > 0
                    }
                    filteredAppointments = allAppointments
                    isLoading = false
                    isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.message
                    isLoading = false
                    isRefreshing = false
                    snackbarHostState.showSnackbar("Ошибка загрузки: ${e.message}")
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
        try {
            loadData()
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Ошибка загрузки: ${e.message}")
        }
    }

    var expandedMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // При изменении поискового запроса фильтруем записи
    LaunchedEffect(Unit) {
        try {
            loadData()
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Ошибка загрузки: ${e.message}")
        }
    }

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
                                text = { Text("Электронная картотека") },
                                onClick = { navController.navigate("emc_search") },
                                leadingIcon = {
                                    Icon(Icons.Default.Book, contentDescription = "Картотека")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Стационарное лечение") },
                                onClick = { navController.navigate("manageInpCareFromDoc") },
                                leadingIcon = {
                                    Icon(Icons.Default.AddToQueue, contentDescription = "Стац лечение")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Выдача талона") },
                                onClick = { navController.navigate("manageTalons") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = "Выдача талона")
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
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddAppointmentDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить запись")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
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

                        Text(
                            text = "Записи на прием сегодня:",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

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
                                        appointment = appointment,
                                        context = context,
                                        onDelete = {
                                            allAppointments = allAppointments.filter { it.id != appointment.id }
                                            filteredAppointments = filteredAppointments.filter { it.id != appointment.id }
                                            pendingCount--
                                        }
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

    if (showAddAppointmentDialog) {
        AddAppointmentDialog(
            medCenterId = medCenterId,
            doctorId = doctorId,
            onDismiss = { showAddAppointmentDialog = false },
            onSave = {
                showAddAppointmentDialog = false
                loadData()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Запись успешно добавлена")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentDialog(
    medCenterId: Int,
    doctorId: Int,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<ApiService.UserSearchResult?>(null) }
    var users by remember { mutableStateOf<List<ApiService.UserSearchResult>>(emptyList()) }
    var timeInput by remember { mutableStateOf("") }
    var reasonInput by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            coroutineScope.launch {
                try {
                    users = RetrofitInstance.api.searchUsers(medCenterId, searchQuery)
                } catch (e: Exception) {
                    users = emptyList()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить запись на прием") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            expanded = it.length > 1 && users.isNotEmpty()
                        },
                        label = { Text("Поиск пациента") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { expanded = false })
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        users.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.fullName) },
                                onClick = {
                                    selectedUser = user
                                    searchQuery = user.fullName
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it },
                    label = { Text("Время приема (например, 14:30)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Причина приема") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUser != null && timeInput.isNotBlank()) {
                        coroutineScope.launch {
                            try {
                                RetrofitInstance.api.createAppointment(
                                    doctorId = doctorId,
                                    userId = selectedUser!!.id,
                                    date = today,
                                    time = timeInput,
                                    reason = reasonInput
                                )
                                onSave()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Ошибка: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = selectedUser != null && timeInput.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
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
fun AppointmentCard(
    appointment: ApiService.AppointmentResponse,
    context: Context,
    onDelete: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDialog) {
        AppointmentDialog(
            patientName = appointment.fullName,
            patientId = appointment.userId,
            startTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date()),
            onDismiss = { showDialog = false },
            onComplete = { record, imageUris ->
                isLoading = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Загружаем фото
                        val photoPaths = mutableListOf<String>()
                        for (uri in imageUris) {
                            try {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val file = File.createTempFile("upload", ".jpg", context.cacheDir)
                                inputStream?.use { input ->
                                    file.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                val part = MultipartBody.Part.createFormData("files", file.name, requestFile)

                                // Отправляем все фото одним запросом
                                val response = RetrofitInstance.api.uploadPhotos(listOf(part))
                                if (response.isSuccessful) {
                                    photoPaths.addAll(response.body()?.paths ?: emptyList())
                                }
                            } catch (e: Exception) {
                                Log.e("PhotoUpload", "Error uploading photo", e)
                            }
                        }

                        // Обновляем запись с путями к фото
                        val updatedRecord = record.copy(photoPaths = photoPaths)

                        // Отправка данных о приеме
                        RetrofitInstance.api.createRecord(updatedRecord)

                        // Обновление статуса приема
                        RetrofitInstance.api.updateAppointmentStatus(
                            appointment.id,
                            "false"
                        )
                        val notifyBody = ApiService.RecordCompleteNotifyRequest(
                            user_id = appointment.userId,
                            doctor_id = getUserIdFromPrefs(context),
                            description = record.description,
                            assignment = record.assignment,
                            paid_or_free = record.paidOrFree,
                            price = record.price,
                            date = appointment.date,
                            time = appointment.time,
                            photo_urls = photoPaths // список путей, полученных после uploadPhotos
                        )
                        RetrofitInstance.api.notifyRecordComplete(notifyBody)

                        withContext(Dispatchers.Main) {
                            isLoading = false
                            showDialog = false
                            Toast.makeText(context, "Прием успешно завершен", Toast.LENGTH_SHORT).show()
                            onDelete() // Обновляем список после завершения приема
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            Log.e("AppointmentDialog", "Ошибка при отправке данных: ${e.message}")
                            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }

                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Подтверждение удаления") },
            text = { Text("Вы уверены, что хотите удалить запись пациента ${appointment.fullName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        isLoading = true
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                RetrofitInstance.api.updateAppointmentStatus(
                                    appointment.id,
                                    "false",
                                    clearData = true
                                )

                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    onDelete()
                                    Toast.makeText(context, "Запись удалена", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ФИО: ${appointment.fullName}",
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
                    text = appointment.time,
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
                    text = appointment.reason ?: "Не указана",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Начать прием")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF800000),
                        contentColor = Color.White
                    )
                ) {
                    Text("Удалить запись")
                }
            }
        }
    }
}

@Composable
fun AppointmentDialog(
    patientName: String,
    patientId: Int,
    startTime: String,
    onDismiss: () -> Unit,
    onComplete: (ApiService.RecordCreate, List<Uri>) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var assignment by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf<Int?>(null) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        imageUris = uris
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Прием пациента: $patientName") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание приема") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = assignment,
                    onValueChange = { assignment = it },
                    label = { Text("Назначения") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Платный прием", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isPaid,
                        onCheckedChange = { isPaid = it }
                    )
                }

                if (isPaid) {
                    OutlinedTextField(
                        value = price?.toString() ?: "",
                        onValueChange = { price = it.toIntOrNull() },
                        label = { Text("Цена") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Button(
                        onClick = { launcher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить фото")
                        Spacer(Modifier.width(4.dp))
                        Text("Добавить фото (${imageUris.size})")
                    }
                }

                LazyRow {
                    items(imageUris) { uri ->
                        Image(
                            painter = rememberImagePainter(uri),
                            contentDescription = "Фото",
                            modifier = Modifier
                                .size(64.dp)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val record = ApiService.RecordCreate(
                        userId = patientId,
                        doctorId = getUserIdFromPrefs(context),
                        description = description,
                        assignment = assignment,
                        paidOrFree = if (isPaid) "payed" else "free",
                        price = price,
                        timeStart = startTime,
                        timeEnd = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date()),
                        medCenterId = getMedCenterIdFromPrefs(context),
                        photoPaths = emptyList()
                    )
                    onComplete(record, imageUris)
                }
            ) {
                Text("Завершить прием")
            }
        }
    )
}

fun getUserIdFromPrefs(context: Context): Int {
    val prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    return prefs.getInt("userId", 0)
}

fun getMedCenterIdFromPrefs(context: Context): Int {
    val prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    return prefs.getInt("medCenterId", 0)
}