package com.mgkct.diplom.doctor

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageInpCareFromDocScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE) }
    val medCenterId = sharedPreferences.getInt("medCenterId", -1)
    val doctorId = sharedPreferences.getInt("userId", -1)
    var patients by remember { mutableStateOf<List<ApiService.InpatientCareResponse>>(emptyList()) }
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
                            InpatientCareCardForDoc(
                                patient = patient,
                                doctorId = doctorId,
                                medCenterId = medCenterId,
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
                                },
                                onReceptionComplete = {
                                    refreshTrigger++
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

@Composable
fun InpatientCareCardForDoc(
    patient: ApiService.InpatientCareResponse,
    doctorId: Int,
    medCenterId: Int,
    onDischarge: () -> Unit,
    onReceptionComplete: () -> Unit
) {
    var showReceptionDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showReceptionDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Начать прием")
                }
                Button(
                    onClick = onDischarge,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000))
                ) {
                    Text("Выписать пациента")
                }
            }
        }
    }

    if (showReceptionDialog) {
        AppointmentDialogForInpCare(
            patientName = patient.userFullName ?: "",
            patientId = patient.userId,
            doctorId = doctorId,
            medCenterId = medCenterId,
            onDismiss = { showReceptionDialog = false },
            onComplete = { record, imageUris ->
                isLoading = true
                CoroutineScope(Dispatchers.IO).launch {
                    try {
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
                                val response = RetrofitInstance.api.uploadPhotos(listOf(part))
                                if (response.isSuccessful) {
                                    photoPaths.addAll(response.body()?.paths ?: emptyList())
                                }
                            } catch (e: Exception) {
                                Log.e("PhotoUpload", "Error uploading photo", e)
                            }
                        }
                        val updatedRecord = record.copy(photoPaths = photoPaths)
                        RetrofitInstance.api.createRecord(updatedRecord)
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            showReceptionDialog = false
                            onReceptionComplete()
                            Toast.makeText(context, "Прием завершен", Toast.LENGTH_SHORT).show()
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

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDialogForInpCare(
    patientName: String,
    patientId: Int,
    doctorId: Int,
    medCenterId: Int,
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
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки на одном уровне
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = {
                            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            val record = ApiService.RecordCreate(
                                userId = patientId,
                                doctorId = doctorId,
                                description = description,
                                assignment = assignment,
                                paidOrFree = if (isPaid) "payed" else "free",
                                price = price,
                                timeStart = now,
                                timeEnd = now,
                                medCenterId = medCenterId,
                                photoPaths = emptyList()
                            )
                            onComplete(record, imageUris)
                        }
                    ) {
                        Text("Завершить прием")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}


fun String?.toFormattedDate(): String {
    if (this.isNullOrEmpty()) return "Не указана"
    return try {
        val timestamp = this.toLong()
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        this
    }
}