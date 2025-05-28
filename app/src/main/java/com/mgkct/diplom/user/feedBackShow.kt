package com.mgkct.diplom.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedBackShowScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()

    var centers by remember { mutableStateOf<List<ApiService.MedCenterWithRating>>(emptyList()) }
    var selectedCenter by remember { mutableStateOf<ApiService.MedCenterWithRating?>(null) }
    var doctors by remember { mutableStateOf<List<ApiService.DoctorWithRating>>(emptyList()) }
    var selectedDoctor by remember { mutableStateOf<ApiService.DoctorWithRating?>(null) }
    var feedbacks by remember { mutableStateOf<List<ApiService.DoctorFeedback>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Загрузка медцентров при первом запуске
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            centers = RetrofitInstance.api.getMedCentersWithRating()
        } catch (e: Exception) {
            error = e.localizedMessage
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отзывы") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Фоновое изображение
            Image(
                painter = painterResource(id = R.drawable.background_image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Основной контент поверх фона
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = "Ошибка: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                selectedDoctor != null -> {
                    // Отзывы по врачу
                    DoctorFeedbacksView(
                        doctor = selectedDoctor!!,
                        feedbacks = feedbacks,
                        onBack = {
                            selectedDoctor = null
                        },
                        onRefresh = {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    feedbacks = RetrofitInstance.api.getDoctorFeedbacks(selectedDoctor!!.id)
                                } catch (e: Exception) {
                                    error = e.localizedMessage
                                }
                                isLoading = false
                            }
                        }
                    )
                }
                selectedCenter != null -> {
                    // Врачи центра
                    DoctorsView(
                        center = selectedCenter!!,
                        doctors = doctors,
                        onDoctorClick = { doctor ->
                            selectedDoctor = doctor
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    feedbacks = RetrofitInstance.api.getDoctorFeedbacks(doctor.id)
                                } catch (e: Exception) {
                                    error = e.localizedMessage
                                }
                                isLoading = false
                            }
                        },
                        onBack = { selectedCenter = null },
                        onRefresh = {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    doctors = RetrofitInstance.api.getDoctorsWithRating(selectedCenter!!.id)
                                } catch (e: Exception) {
                                    error = e.localizedMessage
                                }
                                isLoading = false
                            }
                        }
                    )
                }
                else -> {
                    // Медцентры
                    CentersView(
                        centers = centers,
                        onCenterClick = { center ->
                            selectedCenter = center
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    doctors = RetrofitInstance.api.getDoctorsWithRating(center.id)
                                } catch (e: Exception) {
                                    error = e.localizedMessage
                                }
                                isLoading = false
                            }
                        }
                    )
                }
            }
        }
    }
}

// --- Вспомогательные Composable ---

@Composable
fun CentersView(
    centers: List<ApiService.MedCenterWithRating>,
    onCenterClick: (ApiService.MedCenterWithRating) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCenters = centers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.address.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            "Медицинские учреждения",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        // Поисковое поле
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск по названию или адресу") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        LazyColumn {
            items(filteredCenters) { center ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onCenterClick(center) },
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(center.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Адрес: ${center.address}", fontSize = 16.sp)
                        Text("Телефон: ${center.phone}", fontSize = 16.sp)
                        Text(
                            "Средний рейтинг врачей: ${center.averageRating?.toString() ?: "Нет"}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorsView(
    center: ApiService.MedCenterWithRating,
    doctors: List<ApiService.DoctorWithRating>,
    onDoctorClick: (ApiService.DoctorWithRating) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredDoctors = doctors.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) ||
                (it.workType?.contains(searchQuery, ignoreCase = true) == true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text(
                "Врачи центра: ${center.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = onRefresh,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Обновить")
        }
        // Поисковое поле
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск по ФИО или специализации") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        LazyColumn {
            items(filteredDoctors) { doc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onDoctorClick(doc) },
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(doc.fullName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Специализация: ${doc.workType ?: "—"}", fontSize = 15.sp)
                        Text("Категория: ${doc.category ?: "—"}", fontSize = 15.sp)
                        Text(
                            "Средний рейтинг: ${doc.averageRating?.toString() ?: "Нет"}",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun DoctorFeedbacksView(
    doctor: ApiService.DoctorWithRating,
    feedbacks: List<ApiService.DoctorFeedback>,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text(
                "Отзывы о ${doctor.fullName}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = onRefresh,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Обновить")
        }
        if (feedbacks.isEmpty()) {
            Text("Пока нет отзывов.", fontSize = 16.sp, color = Color.Gray)
        } else {
            LazyColumn {
                items(feedbacks) { fb ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            // ФИО пациента сверху
                            Text(
                                text = fb.userFullName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            // Рейтинг звездами
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "Рейтинг: ",
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFFA000),
                                    fontSize = 15.sp
                                )
                                for (i in 1..5) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Звезда",
                                        tint = if (i <= fb.grade) Color(0xFFFFA000) else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            // Текст отзыва
                            Text(fb.description, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}