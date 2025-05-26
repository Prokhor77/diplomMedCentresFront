package com.mgkct.diplom.user

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMyRecordsScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE) }
    val userId = sharedPreferences.getInt("userId", -1)
    val medCenterId = sharedPreferences.getInt("medCenterId", -1)
    var doctorQuery by remember { mutableStateOf("") }
    var selectedDoctor by remember { mutableStateOf<ApiService.UserSearchResult?>(null) }
    var doctorInfo by remember { mutableStateOf<ApiService.DoctorInfo?>(null) }
    var freeSlots by remember { mutableStateOf<List<ApiService.FreeSlot>>(emptyList()) }
    var selectedSlot by remember { mutableStateOf<ApiService.FreeSlot?>(null) }
    var reason by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Состояния для всех врачей центра и их фильтрации
    var allDoctors by remember { mutableStateOf<List<ApiService.UserSearchResult>>(emptyList()) }
    var filteredDoctors by remember { mutableStateOf<List<ApiService.UserSearchResult>>(emptyList()) }
    // Словарь для хранения информации о врачах
    var doctorsInfo by remember { mutableStateOf<Map<Int, ApiService.DoctorInfo>>(emptyMap()) }

    // Загрузка всех врачей центра при старте
    LaunchedEffect(medCenterId) {
        try {
            val users = RetrofitInstance.api.getUsers()
            allDoctors = users
                .filter { it.role == "doctor" && it.medCenterId == medCenterId }
                .map { user ->
                    ApiService.UserSearchResult(
                        id = user.id,
                        fullName = user.fullName ?: "",
                        role = user.role
                    )
                }
            filteredDoctors = allDoctors

            // Загрузка информации о каждом враче
            val infoMap = mutableMapOf<Int, ApiService.DoctorInfo>()
            allDoctors.forEach { doctor ->
                try {
                    val info = RetrofitInstance.api.getDoctorInfo(doctor.id)
                    infoMap[doctor.id] = info
                } catch (e: Exception) {
                    // Если не удалось загрузить информацию, используем пустой объект
                    infoMap[doctor.id] = ApiService.DoctorInfo(null, null)
                }
            }
            doctorsInfo = infoMap
        } catch (e: Exception) {
            allDoctors = emptyList()
            filteredDoctors = emptyList()
        }
    }

    // Фильтрация врачей по поисковому запросу
    LaunchedEffect(doctorQuery, allDoctors) {
        filteredDoctors = if (doctorQuery.isBlank()) {
            allDoctors
        } else {
            allDoctors.filter { it.fullName.contains(doctorQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Запись к врачу") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.background_image),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Поиск врача и карточки всех врачей центра
                if (selectedDoctor == null) {
                    // Обычное текстовое поле без выпадающего списка
                    OutlinedTextField(
                        value = doctorQuery,
                        onValueChange = { doctorQuery = it },
                        label = { Text("Поиск врача по ФИО") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Все врачи центра:", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDoctors) { doctor ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDoctor = doctor
                                        doctorQuery = doctor.fullName
                                        coroutineScope.launch {
                                            doctorInfo = RetrofitInstance.api.getDoctorInfo(doctor.id)
                                            freeSlots = RetrofitInstance.api.getDoctorFreeSlots(doctor.id)
                                        }
                                    },
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(doctor.fullName, style = MaterialTheme.typography.titleMedium)
                                    // Отображаем специализацию и категорию из словаря doctorsInfo
                                    val info = doctorsInfo[doctor.id]
                                    Text(
                                        "Специализация: ${info?.workType ?: "Не указана"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Категория: ${info?.category ?: "Не указана"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Кнопка "Сменить врача"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Врач: ${selectedDoctor?.fullName}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            selectedDoctor = null
                            doctorQuery = ""
                            doctorInfo = null
                            freeSlots = emptyList()
                        }) {
                            Text("Сменить")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Карточка врача
                selectedDoctor?.let { doctor ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("ФИО: ${doctor.fullName}")
                            Text("Специализация: ${doctorInfo?.workType ?: "Не указана"}")
                            Text("Категория: ${doctorInfo?.category ?: "Не указана"}")
                        }
                    }
                }

                // Список свободных слотов
                if (freeSlots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Свободное время:")
                    LazyColumn {
                        items(freeSlots) { slot ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Дата: ${slot.date}")
                                    Text("Время: ${slot.time}")
                                    Text("Причина: ${slot.reason ?: "Не указана"}")
                                    Button(
                                        onClick = {
                                            selectedSlot = slot
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text("Записаться")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог для ввода причины и подтверждения записи
    if (selectedSlot != null) {
        AlertDialog(
            onDismissRequest = {
                selectedSlot = null
                reason = ""
            },
            title = { Text("Запись на прием") },
            text = {
                Column {
                    Text("Вы записываетесь к врачу: ${selectedDoctor?.fullName}")
                    Text("Дата: ${selectedSlot?.date}, Время: ${selectedSlot?.time}")
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Причина") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                RetrofitInstance.api.updateAppointmentStatus(
                                    id = selectedSlot!!.id,
                                    active = "true",
                                    userId = userId,
                                    reason = reason
                                )
                                freeSlots = RetrofitInstance.api.getDoctorFreeSlots(selectedDoctor!!.id)
                                selectedSlot = null
                                reason = ""
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                snackbarHostState.showSnackbar("Вы успешно записались на прием!")
                            } catch (e: Exception) {
                                selectedSlot = null
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                            }
                        }
                    },
                    enabled = reason.isNotBlank()
                ) {
                    Text("Записаться")
                }
            },
            dismissButton = {
                Button(onClick = {
                    selectedSlot = null
                    reason = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}