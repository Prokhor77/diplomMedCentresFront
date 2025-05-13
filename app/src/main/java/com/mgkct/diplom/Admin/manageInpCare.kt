package com.mgkct.diplom.Admin

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mgkct.diplom.*
import com.mgkct.diplom.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageInpatientCareScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE) }
    val medCenterId = sharedPreferences.getInt("medCenterId", -1)
    var patients by remember { mutableStateOf<List<ApiService.InpatientCareResponse>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") } // Добавляем состояние для строки поиска
    val coroutineScope = rememberCoroutineScope()

    // Функция для загрузки пациентов
    fun loadPatients() {
        coroutineScope.launch {
            patients = RetrofitInstance.api.getInpatientCares(medCenterId, "true")
        }
    }

    // Загружаем пациентов при первом открытии экрана
    LaunchedEffect(medCenterId) {
        loadPatients()
    }

    // Фильтруем пациентов по поисковому запросу
    val filteredPatients = patients.filter { patient ->
        patient.userFullName?.contains(searchQuery, ignoreCase = true) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Стационарное лечение") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Добавить пациента")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.background_image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Добавляем строку поиска
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск пациентов") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, "Очистить")
                            }
                        }
                    },
                    singleLine = true
                )

                if (filteredPatients.isEmpty()) {
                    if (searchQuery.isNotEmpty()) {
                        // Показываем сообщение, если поиск не дал результатов
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Пациенты не найдены",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        // Показываем сообщение, если нет пациентов
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Нет пациентов на стационарном лечении",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    // Отображаем список пациентов
                    LazyColumn {
                        items(filteredPatients) { patient ->
                            InpatientCareCard(
                                patient = patient,
                                onDischarge = {
                                    coroutineScope.launch {
                                        RetrofitInstance.api.updateInpatientCare(patient.id, "false")
                                        loadPatients() // Перезагружаем список после выписки
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddInpatientCareDialog(
            medCenterId = medCenterId,
            onDismiss = { showAddDialog = false },
            onSave = { newPatient ->
                coroutineScope.launch {
                    RetrofitInstance.api.createInpatientCare(newPatient, medCenterId)
                    loadPatients() // Перезагружаем список после добавления
                }
            }
        )
    }
}

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}


@Composable
fun InpatientCareCard(patient: ApiService.InpatientCareResponse, onDischarge: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = ("ФИО: ${patient.userFullName}"), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Этаж: ${patient.floor}")
                    Text("Палата: ${patient.ward}")
                    patient.receiptDate?.let {
                        Text("Поступление: ${it.toFormattedDate()}")
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onDischarge,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Выписать пациента")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInpatientCareDialog(
    medCenterId: Int,
    onDismiss: () -> Unit,
    onSave: (ApiService.InpatientCareCreate) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<ApiService.UserSearchResult?>(null) }
    var users by remember { mutableStateOf<List<ApiService.UserSearchResult>>(emptyList()) }
    var floor by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    var receiptDate by remember { mutableStateOf("") }
    var expireDate by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            coroutineScope.launch {
                users = RetrofitInstance.api.searchUsers(medCenterId, searchQuery)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить пациента") },
        text = {
            var expanded by remember { mutableStateOf(false) }

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
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { })
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
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Этаж") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ward,
                    onValueChange = { ward = it },
                    label = { Text("Палата") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = receiptDate,
                    onValueChange = { receiptDate = it },
                    label = { Text("Дата поступления (ДД.ММ.ГГГГ)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val receiptTimestamp = try {
                        dateFormat.parse(receiptDate)?.time
                    } catch (e: Exception) {
                        null
                    }

                    val expireTimestamp = try {
                        dateFormat.parse(expireDate)?.time
                    } catch (e: Exception) {
                        null
                    }

                    val newCare = ApiService.InpatientCareCreate(
                        userId = selectedUser?.id ?: 0,
                        floor = floor.toIntOrNull() ?: 0,
                        ward = ward.toIntOrNull() ?: 0,
                        receipt_date = receiptTimestamp?.toString() ?: "",
                        expire_date = expireTimestamp?.toString() ?: ""
                    )
                    onSave(newCare)
                    onDismiss()
                },
                enabled = selectedUser != null
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