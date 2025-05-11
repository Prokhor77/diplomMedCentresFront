package com.mgkct.diplom.Admin

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.LoginActivity
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Composable
fun FeedBackEdit(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
    val medCenterId = sharedPref.getInt("medCenterId", 0)

    var feedbacks by remember { mutableStateOf<List<ApiService.Feedback>>(emptyList()) }
    var users by remember { mutableStateOf<List<ApiService.User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedFeedbackId by remember { mutableStateOf<Int?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val feedbacksResponse = RetrofitInstance.api.getFeedbacks()
            val usersResponse = RetrofitInstance.api.getUsers()

            feedbacks = feedbacksResponse.filter {
                it.medCenterId == medCenterId && it.active == "in_progress"
            }
            users = usersResponse
            isLoading = false
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка загрузки данных: ${e.message}", Toast.LENGTH_LONG).show()
            isLoading = false
        }
    }

    fun approveFeedback(feedbackId: Int) {
        // Launch a coroutine to call the suspend function
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitInstance.api.approveFeedback(feedbackId)
                withContext(Dispatchers.Main) {
                    feedbacks = feedbacks.filter { it.id != feedbackId }
                    Toast.makeText(context, "Отзыв одобрен", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun rejectFeedback(feedbackId: Int, reason: String) {
        // Launch a coroutine to call the suspend function
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitInstance.api.rejectFeedback(feedbackId, ApiService.RejectReason(reason))
                withContext(Dispatchers.Main) {
                    feedbacks = feedbacks.filter { it.id != feedbackId }
                    Toast.makeText(context, "Отзыв отклонен", Toast.LENGTH_SHORT).show()
                    showRejectDialog = false
                    rejectReason = ""
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Модерация отзывов") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Загрузка...")
                    }
                } else if (feedbacks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет отзывов для модерации")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(6.dp)) } // Добавляем отступ перед первым отзывом
                        items(feedbacks) { feedback ->
                            FeedbackCard(
                                feedback = feedback,
                                users = users,
                                onApprove = { approveFeedback(feedback.id) },
                                onReject = {
                                    selectedFeedbackId = feedback.id
                                    showRejectDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }


    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Причина отклонения") },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("Укажите причину") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedFeedbackId?.let {
                            rejectFeedback(it, rejectReason)
                        }
                    }
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showRejectDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun FeedbackCard(
    feedback: ApiService.Feedback,
    users: List<ApiService.User>,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val patient = users.find { it.id == feedback.userId }
    val doctor = users.find { it.id == feedback.doctorId }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Пациент: ${patient?.fullName ?: "Неизвестно"}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Врач: ${doctor?.fullName ?: "Неизвестно"}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Рейтинг звездами
            Row {
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Звезда",
                        tint = if (i <= feedback.grade) Color(0xFFFFA000) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = feedback.description,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Одобрить")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Одобрить")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Отклонить")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отклонить")
                }
            }
        }
    }
}