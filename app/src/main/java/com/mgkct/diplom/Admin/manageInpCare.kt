package com.mgkct.diplom.Admin

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.ui.focus.onFocusChanged
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
    var searchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(medCenterId, refreshTrigger) {
        try {
            patients = RetrofitInstance.api.getInpatientCares(medCenterId, "true")
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Ошибка загрузки пациентов: ${e.message}")
        }
    }

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
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Column(modifier = Modifier.padding(16.dp)) {
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
                    LazyColumn {
                        items(filteredPatients) { patient ->
                            InpatientCareCard(
                                patient = patient,
                                onDischarge = {
                                    coroutineScope.launch {
                                        try {
                                            RetrofitInstance.api.updateInpatientCare(patient.id, "false")
                                            refreshTrigger++
                                            snackbarHostState.showSnackbar("Пациент успешно выписан")
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Ошибка при выписке: ${e.message}")
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
                    try {
                        val existingPatients = RetrofitInstance.api.getInpatientCares(medCenterId, "true")
                        val isAlreadyInCare = existingPatients.any { it.userId == newPatient.userId }

                        if (isAlreadyInCare) {
                            snackbarHostState.showSnackbar("Этот пациент уже находится на стационарном лечении")
                        } else {
                            RetrofitInstance.api.createInpatientCare(newPatient, medCenterId)
                            refreshTrigger++
                            snackbarHostState.showSnackbar("Пациент успешно добавлен")
                            showAddDialog = false
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Ошибка при добавлении: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
fun InpatientCareCard(patient: ApiService.InpatientCareResponse, onDischarge: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ФИО: ${patient.userFullName ?: "Не указано"}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Этаж: ${patient.floor}")
                Text("Палата: ${patient.ward}")
                Text("Поступление: ${patient.receiptDate.toFormattedDate()}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDischarge,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Выписать пациента")
            }
        }
    }
}

fun String?.toFormattedDate(): String {
    if (this.isNullOrEmpty()) return "Не указана"
    return try {
        // Предполагаем, что дата приходит в формате ISO (например "2023-12-31T00:00:00.000Z")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = sdf.parse(this)
        val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        // Если формат не совпадает, пытаемся разобрать как timestamp строку
        try {
            val timestamp = this.toLong()
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: NumberFormatException) {
            "Неверный формат даты"
        }
    }
}



fun String.toUnixTimestampString(): String {
    return try {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val date = sdf.parse(this)
        (date?.time?.toString()) ?: "0"
    } catch (e: Exception) {
        "0"
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
    var showDateError by remember { mutableStateOf(false) }

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

    fun validateAndSave() {
        if (selectedUser == null) {
            return
        }

        if (floor.isBlank() || ward.isBlank() || receiptDate.isBlank()) {
            return
        }

        // Проверка формата даты
        if (!isValidDate(receiptDate)) {
            showDateError = true
            return
        }

        showDateError = false

        val newCare = ApiService.InpatientCareCreate(
            userId = selectedUser?.id ?: 0,
            floor = floor.toIntOrNull() ?: 0,
            ward = ward.toIntOrNull() ?: 0,
            receipt_date = receiptDate.toUnixTimestampString(),
            expire_date = expireDate
        )
        onSave(newCare)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить пациента") },
        text = {
            var expanded by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth()) {
                var expanded by remember { mutableStateOf(false) }
                var fieldWasFocused by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded && users.isNotEmpty(),
                    onExpandedChange = { isExpanded ->
                        // Открывать меню только если есть пользователи
                        expanded = isExpanded && users.isNotEmpty()
                    }
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            // Открывать меню только если есть пользователи и длина запроса > 1
                            expanded = it.length > 1 && users.isNotEmpty()
                        },
                        label = { Text("Поиск пациента") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .onFocusChanged { focusState ->
                                // Открывать меню только если есть пользователи и поле не было в фокусе ранее
                                if (focusState.isFocused && !fieldWasFocused) {
                                    expanded = users.isNotEmpty()
                                    fieldWasFocused = true
                                }
                                if (!focusState.isFocused) {
                                    expanded = false
                                    fieldWasFocused = false
                                }
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { })
                    )

                    ExposedDropdownMenu(
                        expanded = expanded && users.isNotEmpty(),
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
                    modifier = Modifier.fillMaxWidth(),
                    isError = showDateError,
                    supportingText = {
                        if (showDateError) {
                            Text("Введите дату в формате ДД.ММ.ГГГГ")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndSave() },
                enabled = selectedUser != null && floor.isNotBlank() && ward.isNotBlank() && receiptDate.isNotBlank()
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

// Функция для проверки формата даты
fun isValidDate(dateStr: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        sdf.isLenient = false
        sdf.parse(dateStr)
        true
    } catch (e: Exception) {
        false
    }
}