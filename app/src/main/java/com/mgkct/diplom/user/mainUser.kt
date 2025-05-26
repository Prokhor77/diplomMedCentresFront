package com.mgkct.diplom.user

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainUserScreen(navController: NavController) {

    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    val userId = sharedPref.getInt("userId", 0)
    val fullName = sharedPref.getString("fullName", "") ?: ""
    val medCenterId = sharedPref.getInt("medCenterId", 0)

    var qrCodeUrl by remember { mutableStateOf<String?>(null) }
    var avgRating by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }

    // Новые состояния для центра
    var centerName by remember { mutableStateOf<String?>(null) }
    var centerAddress by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Получаем QR-код
                val qrResp = RetrofitInstance.api.getUserQrCode(userId)
                qrCodeUrl = RetrofitInstance.BASE_URL + qrResp.path

                // Получаем рейтинг
                val ratingResp = RetrofitInstance.api.getAverageDoctorRating(medCenterId)
                avgRating = ratingResp.average_rating

                // Получаем данные о медцентре
                val centers = RetrofitInstance.api.getMedicalCenters()
                val myCenter = centers.find { it.id == medCenterId }
                centerName = myCenter?.name ?: "Не найден"
                centerAddress = myCenter?.address ?: "Не найден"
            } catch (e: Exception) {
                error = "Ошибка загрузки данных: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.mipmap.medical),
                            contentDescription = "Иконка",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(fullName, style = MaterialTheme.typography.titleLarge)
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
                            onClick = {
                                expandedMenu = false
                                navController.navigate("user")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = "Главная")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Мои приемы") },
                            onClick = {
                                expandedMenu = false
                                navController.navigate("user_history")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Мои приемы")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Записаться на прием") },
                            onClick = {
                                expandedMenu = false
                                navController.navigate("")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.LocalHospital, contentDescription = "Записаться на прием")
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.background_image),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = error ?: "Ошибка",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Карточка медцентра
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                text = centerName ?: "Загрузка...",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Адрес: ${centerAddress ?: "Загрузка..."}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // QR-код
                    qrCodeUrl?.let { url ->
                        Card(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .size(220.dp)
                                .clickable { showQrDialog = true },
                            elevation = CardDefaults.cardElevation(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(url),
                                    contentDescription = "QR-код пользователя",
                                    modifier = Modifier.size(180.dp)
                                )
                            }
                        }
                    }

                    // Диалог с увеличенным QR-кодом
                    if (showQrDialog && qrCodeUrl != null) {
                        Dialog(onDismissRequest = { showQrDialog = false }) {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                elevation = CardDefaults.cardElevation(16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(24.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(qrCodeUrl),
                                        contentDescription = "Увеличенный QR-код",
                                        modifier = Modifier.size(350.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showQrDialog = false }
                                    ) {
                                        Text("Отмена")
                                    }
                                }
                            }
                        }
                    }

                    // Карточка рейтинга
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(18.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Рейтинг",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Средний рейтинг врачей: ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 20.sp
                            )
                            Text(
                                text = avgRating?.toString() ?: "Нет данных",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}