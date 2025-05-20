package com.mgkct.diplom.doctor

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.mgkct.diplom.*
import com.mgkct.diplom.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEMCScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE) }
    val medCenterId = sharedPreferences.getInt("medCenterId", -1)
    var records by remember { mutableStateOf<List<ApiService.RecordResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Новые состояния для поиска по ФИО
    var userSearchQuery by remember { mutableStateOf("") }
    var userSearchResults by remember { mutableStateOf<List<ApiService.UserSearchResult>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<ApiService.UserSearchResult?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // Поиск пользователей по части ФИО
    LaunchedEffect(userSearchQuery) {
        if (userSearchQuery.length > 2) {
            coroutineScope.launch {
                try {
                    userSearchResults = RetrofitInstance.api.searchUsers(medCenterId, userSearchQuery)
                } catch (e: Exception) {
                    userSearchResults = emptyList()
                }
            }
        } else {
            userSearchResults = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Электронная картотека") },
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
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Новый поиск по ФИО
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = userSearchQuery,
                        onValueChange = {
                            userSearchQuery = it
                            expanded = it.length > 2 && userSearchResults.isNotEmpty()
                            selectedUser = null
                            records = emptyList()
                            error = null
                        },
                        label = { Text("Поиск пациента по ФИО") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        trailingIcon = {
                            if (userSearchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    userSearchQuery = ""
                                    userSearchResults = emptyList()
                                    selectedUser = null
                                    records = emptyList()
                                    error = null
                                }) {
                                    Icon(Icons.Default.Close, "Очистить")
                                }
                            }
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        userSearchResults.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.fullName) },
                                onClick = {
                                    selectedUser = user
                                    userSearchQuery = user.fullName
                                    expanded = false
                                    coroutineScope.launch {
                                        isLoading = true
                                        error = null
                                        try {
                                            records = RetrofitInstance.api.getPatientRecords(user.id)
                                        } catch (e: Exception) {
                                            error = e.message ?: "Ошибка загрузки записей"
                                            records = emptyList()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            error!!,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else if (records.isEmpty()) {
                    if (userSearchQuery.isNotEmpty() && selectedUser != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Записи не найдены",
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
                                "Введите ФИО пациента для поиска",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                } else {
                    LazyColumn {
                        items(records) { record ->
                            MedicalRecordCard(
                                record = record,
                                onImageClick = { photo ->
                                    expandedImageUrl = "${RetrofitInstance.BASE_URL.trimEnd('/')}/${photo.photo_path.trimStart('/')}"
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    expandedImageUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { expandedImageUrl = null },
            confirmButton = {
                TextButton(onClick = { expandedImageUrl = null }) {
                    Text("Закрыть")
                }
            },
            title = { Text("Просмотр изображения") },
            text = {
                Image(
                    painter = rememberAsyncImagePainter(url),
                    contentDescription = "Увеличенное изображение",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        )
    }
}

@Composable
fun MedicalRecordCard(
    record: ApiService.RecordResponse,
    onImageClick: (ApiService.RecordPhotoResponse) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Прием от ${record.time_start}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Врач: ${record.doctorFullName ?: "Неизвестно"}")
                Text("Специализация: ${record.doctorWorkType ?: "Не указана"}")
                Text("Тип приема: ${if (record.paidOrFree == "payed") "Платный" else "Бесплатный"}")
                if (record.price != null) {
                    Text("Стоимость: ${record.price} руб.")
                }
                Text("Время начала: ${record.time_start}")
                Text("Время окончания: ${record.time_end ?: "Не указано"}")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Описание:",
                    fontWeight = FontWeight.Bold
                )
                Text(record.description ?: "Не указано")

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Назначения:",
                    fontWeight = FontWeight.Bold
                )
                Text(record.assignment ?: "Не указаны")

                if (!record.photos.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Прикрепленные фото:",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (record.photos.size > 3) {
                        // Горизонтальный скролл с LazyRow
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(record.photos) { photo ->
                                val imageUrl = "${RetrofitInstance.BASE_URL.trimEnd('/')}/${photo.photo_path.trimStart('/')}"
                                Box(modifier = Modifier.size(100.dp)) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Медицинское фото",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { onImageClick(photo) },
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }
                    } else {
                        // Обычный Row для <= 5 фото
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            record.photos.forEach { photo ->
                                val imageUrl = "${RetrofitInstance.BASE_URL.trimEnd('/')}/${photo.photo_path.trimStart('/')}"
                                Box(modifier = Modifier.size(100.dp)) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Медицинское фото",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { onImageClick(photo) },
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}