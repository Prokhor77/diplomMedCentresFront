package com.mgkct.diplom.user

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.launch

@Composable
fun ReviewDialog(
    record: ApiService.RecordResponse,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var grade by remember { mutableStateOf(5) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(grade, description)
                },
                enabled = description.isNotBlank()
            ) {
                Text("Отправить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Оставить отзыв") },
        text = {
            Column {
                Text("Врач: ${record.doctorFullName ?: "Неизвестно"}")
                Text("Дата приема: ${record.time_start}")
                Spacer(Modifier.height(8.dp))
                Text("Оценка:")
                Row {
                    for (i in 1..5) {
                        IconButton(onClick = { grade = i }) {
                            Icon(
                                painterResource(
                                    if (i <= grade) R.drawable.baseline_star_24 else R.drawable.outline_star_outline_24
                                ),
                                contentDescription = null,
                                tint = if (i <= grade) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание отзыва") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE) }
    val userId = sharedPreferences.getInt("userId", -1)
    var records by remember { mutableStateOf<List<ApiService.RecordResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var reviewedIds by remember {
        mutableStateOf(sharedPreferences.getStringSet("reviewedIds", emptySet())!!.toMutableSet())
    }
    var showReviewDialogForRecord by remember { mutableStateOf<ApiService.RecordResponse?>(null) }

    // Загрузка записей при запуске экрана
    LaunchedEffect(userId) {
        if (userId != -1) {
            isLoading = true
            error = null
            try {
                records = RetrofitInstance.api.getPatientRecords(userId)
            } catch (e: Exception) {
                error = e.message ?: "Ошибка загрузки посещений"
                records = emptyList()
            } finally {
                isLoading = false
            }
        } else {
            error = "Не удалось получить ID пользователя"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои посещения") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
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

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            error!!,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                records.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Посещений не найдено",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        items(records) { record ->
                            MedicalRecordCard(
                                record = record,
                                onImageClick = { photo ->
                                    expandedImageUrl = "${RetrofitInstance.BASE_URL.trimEnd('/')}/${photo.photo_path.trimStart('/')}"
                                },
                                onLeaveReview = {
                                    showReviewDialogForRecord = record
                                },
                                reviewLeft = reviewedIds.contains(record.id.toString())
                            )
                        }
                    }

                    showReviewDialogForRecord?.let { record ->
                        ReviewDialog(
                            record = record,
                            onDismiss = { showReviewDialogForRecord = null },
                            onSubmit = { grade, description ->
                                coroutineScope.launch {
                                    try {
                                        val userId = sharedPreferences.getInt("userId", -1)
                                        val medCenterId = sharedPreferences.getInt("medCenterId", -1)
                                        RetrofitInstance.api.createFeedback(
                                            ApiService.FeedbackCreate(
                                                userId = userId,
                                                doctorId = record.doctorId,
                                                medCenterId = medCenterId,
                                                grade = grade,
                                                description = description
                                            )
                                        )
                                        // Обновляем reviewedIds через setState
                                        val newReviewedIds = reviewedIds.toMutableSet()
                                        newReviewedIds.add(record.id.toString())
                                        reviewedIds = newReviewedIds
                                        sharedPreferences.edit().putStringSet("reviewedIds", newReviewedIds).apply()
                                        showReviewDialogForRecord = null
                                        snackbarHostState.showSnackbar("Отзыв отправлен")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Ошибка отправки отзыва: ${e.message}")
                                    }
                                }
                            }
                        )
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
    onImageClick: (ApiService.RecordPhotoResponse) -> Unit,
    onLeaveReview: () -> Unit,
    reviewLeft: Boolean
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

            Spacer(modifier = Modifier.height(8.dp))

            if (reviewLeft) {
                Button(
                    onClick = {},
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отзыв уже оставлен")
                }
            } else {
                Button(
                    onClick = onLeaveReview,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Оставить отзыв")
                }
            }
        }
    }
}