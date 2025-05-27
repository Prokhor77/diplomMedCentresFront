import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mgkct.diplom.Admin.EditAccountsFromAdminScreen
import com.mgkct.diplom.Admin.EditDocWorkTimeScreen
import com.mgkct.diplom.Admin.FeedBackEdit
import com.mgkct.diplom.Admin.ManageInpatientCareScreen
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.LoginActivity
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.launch

class AdminStatsViewModel : ViewModel() {
    var appointmentsToday by mutableStateOf<Int?>(null)
    var doctorsCount by mutableStateOf<Int?>(null)
    var averageRating by mutableStateOf<Double?>(null)
    var inpatientPatientsCount by mutableStateOf<Int?>(null)
    var isLoading by mutableStateOf(false)

    fun loadStats(medCenterId: Int) {
        isLoading = true
        viewModelScope.launch {
            try {
                appointmentsToday = RetrofitInstance.api.getAppointmentsToday(medCenterId).count
                doctorsCount = RetrofitInstance.api.getDoctorsCount(medCenterId).count
                averageRating = RetrofitInstance.api.getAverageDoctorRating(medCenterId).average_rating
                inpatientPatientsCount = RetrofitInstance.api.getInpatientPatientsCount(medCenterId).count
            } catch (e: Exception) {
                // Можно добавить обработку ошибок
            } finally {
                isLoading = false
            }
        }
    }
}

// --- MAIN ACTIVITY ---

class MainAdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "main_admin") {
                composable("main_admin") { MainAdminScreen(navController) }
                // Добавьте остальные экраны по необходимости
            }
        }
    }
}

// --- MAIN ADMIN SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAdminScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    val fullName = sharedPref.getString("fullName", "") ?: ""
    val centerName = sharedPref.getString("centerName", "") ?: ""
    val medCenterId = sharedPref.getInt("medCenterId", 0)

    val viewModel: AdminStatsViewModel = viewModel()

    LaunchedEffect(medCenterId) {
        if (medCenterId != 0) {
            viewModel.loadStats(medCenterId)
        }
    }

    var expandedMenu by remember { mutableStateOf(false) }

    val data = listOf(
        "Приемов за сегодня: ${viewModel.appointmentsToday?.toString() ?: "…"}",
        "Общее кол-во врачей: ${viewModel.doctorsCount?.toString() ?: "…"}",
        "Общий рейтинг врачей: ${viewModel.averageRating?.toString() ?: "…"}",
        "Количество пациентов стац лечения: ${viewModel.inpatientPatientsCount?.toString() ?: "…"}"
    )

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
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.medical),
                                contentDescription = "Иконка",
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(fullName)
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
                                onClick = { navController.navigate("main_admin") },
                                leadingIcon = {
                                    Icon(Icons.Default.Home, contentDescription = "Главная")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Управление аккаунтами") },
                                onClick = { navController.navigate("edit_accounts_admin") },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = "Управление аккаунтами")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Модерация отзывов") },
                                onClick = { navController.navigate("edit_feedback") },
                                leadingIcon = {
                                    Icon(Icons.Default.Message, contentDescription = "Модерация отзывов")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Управление стац лечением") },
                                onClick = { navController.navigate("manage_inp_care") },
                                leadingIcon = {
                                    Icon(Icons.Default.AddToQueue, contentDescription = "Управление стац лечением")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Управление рабочим временем") },
                                onClick = { navController.navigate("editDocWorkTime") },
                                leadingIcon = {
                                    Icon(Icons.Default.Timelapse, contentDescription = "Управление рабочим временем")
                                }
                            )
                            val context = LocalContext.current

                            DropdownMenuItem(
                                text = { Text("Выйти") },
                                onClick = {
                                    val sharedPref = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                                    with(sharedPref.edit()) {
                                        clear()
                                        apply()
                                    }
                                    navController.navigate("login_screen") {
                                        popUpTo(0)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Выход")
                                }
                            )
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
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = centerName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(1.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(data) { text ->
                        InfoCard(text)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                InfoSection(
                    title = "📊 Статистика пациентов",
                    items = listOf(
                        "Количество записанных пациентов на сегодня: 120",
                        "Среднее время приема: 15 минут"
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoSection(
                    title = "💰 Финансовая статистика",
                    items = listOf(
                        "Доход за день: 2500 BYN",
                        "Оплаченные услуги: 48",
                        "Бесплатные услуги: 165"
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                InfoSection(
                    title = "📩 Заявки и жалобы",
                    items = listOf(
                        "Новые отзывы на рассмотрение: 5",
                        "Отзывы от пациентов за неделю: 2"
                    )
                )
            }
        }
    }
}

@Composable
fun InfoSection(title: String, items: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(6.dp))
        items.forEach { item ->
            Text(
                text = item,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun InfoCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(1.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainAdminScreen() {
    MainAdminScreen(
        navController = rememberNavController(),
    )
}