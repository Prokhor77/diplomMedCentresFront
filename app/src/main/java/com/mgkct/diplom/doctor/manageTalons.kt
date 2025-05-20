package com.mgkct.diplom.doctor

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
fun ManageTalonsScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE) }
    val medCenterId = sharedPreferences.getInt("medCenterId", -1)
    val centerName = sharedPreferences.getString("centerName", "") ?: ""
    val centerAddress = sharedPreferences.getString("centerAddress", "") ?: ""
    var doctorQuery by remember { mutableStateOf("") }
    var selectedDoctor by remember { mutableStateOf<ApiService.UserSearchResult?>(null) }
    var doctorResults by remember { mutableStateOf<List<ApiService.UserSearchResult>>(emptyList()) }
    var doctorInfo by remember { mutableStateOf<ApiService.DoctorInfo?>(null) }
    var freeSlots by remember { mutableStateOf<List<ApiService.FreeSlot>>(emptyList()) }
    var showPatientDialog by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableStateOf<ApiService.FreeSlot?>(null) }
    var patientQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var patientResults by remember { mutableStateOf<List<ApiService.UserSearchResult>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var selectedPatient by remember { mutableStateOf<ApiService.UserSearchResult?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var patientReason by remember { mutableStateOf("") }

    // Поиск врача по ФИО
    LaunchedEffect(doctorQuery) {
        if (doctorQuery.length > 2 && selectedDoctor == null) {
            coroutineScope.launch {
                try {
                    val allResults = RetrofitInstance.api.searchUsers(medCenterId, doctorQuery)
                    doctorResults = allResults.filter { it.role == "doctor" } // фильтрация по роли
                } catch (e: Exception) {
                    doctorResults = emptyList()
                }
            }
        } else {
            doctorResults = emptyList()
        }
    }

    // Поиск пациента по ФИО
    LaunchedEffect(patientQuery) {
        if (patientQuery.length > 2 && showPatientDialog) {
            coroutineScope.launch {
                try {
                    patientResults = RetrofitInstance.api.searchUsers(medCenterId, patientQuery)
                } catch (e: Exception) {
                    patientResults = emptyList()
                }
            }
        } else {
            patientResults = emptyList()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Выдача талона") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Назад")
                        }
                    }
                )
                // Шапка с адресом и названием медцентра
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (centerName.isNotBlank()) {
                        Text(
                            text = centerName,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (centerAddress.isNotBlank()) {
                        Text(
                            text = centerAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
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
                // Поиск врача показываем только если врач не выбран
                if (selectedDoctor == null) {
                    ExposedDropdownMenuBox(
                        expanded = doctorResults.isNotEmpty(),
                        onExpandedChange = {}
                    ) {
                        OutlinedTextField(
                            value = doctorQuery,
                            onValueChange = { doctorQuery = it },
                            label = { Text("Поиск врача по ФИО") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = doctorResults.isNotEmpty(),
                            onDismissRequest = {}
                        ) {
                            doctorResults.forEach { doctor ->
                                DropdownMenuItem(
                                    text = { Text(doctor.fullName ?: "") },
                                    onClick = {
                                        selectedDoctor = doctor
                                        doctorQuery = doctor.fullName ?: ""
                                        coroutineScope.launch {
                                            doctorInfo = RetrofitInstance.api.getDoctorInfo(doctor.id)
                                            freeSlots = RetrofitInstance.api.getDoctorFreeSlots(doctor.id)
                                        }
                                    }
                                )
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
                            doctorResults = emptyList()
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

                // Список свободных слотов с кнопкой "Записать пациента"
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
                                            // Открываем диалог поиска пациента
                                            patientQuery = ""
                                            patientResults = emptyList()
                                            showPatientDialog = true
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Text("Записать пациента")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог поиска пациента и запись на слот
    if (showPatientDialog && selectedSlot != null) {
        AlertDialog(
            onDismissRequest = { showPatientDialog = false },
            title = { Text("Поиск пациента") },
            text = {
                Column {
                    OutlinedTextField(
                        value = patientQuery,
                        onValueChange = { patientQuery = it },
                        label = { Text("ФИО пациента") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = patientReason,
                        onValueChange = { patientReason = it },
                        label = { Text("Причина") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(patientResults) { patient ->
                            val isSelected = patient == selectedPatient
                            DropdownMenuItem(
                                text = { Text(patient.fullName) },
                                onClick = {
                                    selectedPatient = patient
                                },
                                modifier = if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else Modifier
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedPatient != null) {
                            coroutineScope.launch {
                                try {
                                    RetrofitInstance.api.updateAppointmentStatus(
                                        id = selectedSlot!!.id,
                                        active = "true",
                                        userId = selectedPatient!!.id,
                                        reason = patientReason
                                    )
                                    freeSlots = RetrofitInstance.api.getDoctorFreeSlots(selectedDoctor!!.id)
                                    showPatientDialog = false
                                    selectedSlot = null
                                    selectedPatient = null
                                    patientReason = "" // сброс причины
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    snackbarHostState.showSnackbar("Пациент успешно записан!")
                                } catch (e: Exception) {
                                    showPatientDialog = false
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    snackbarHostState.showSnackbar("Ошибка: ${e.message}")
                                }
                            }
                        }
                    },
                    enabled = selectedPatient != null
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showPatientDialog = false
                    selectedPatient = null
                    patientReason = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}