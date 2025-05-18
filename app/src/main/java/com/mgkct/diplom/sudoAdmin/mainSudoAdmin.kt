package com.mgkct.diplom.SudoAdmin

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mgkct.diplom.LoginScreen
import com.mgkct.diplom.R
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.mgkct.diplom.SudoAdmin.EditAccountsScreen

class MainSudoAdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "main_sudo_admin") {
                composable("main_sudo_admin") { MainSudoAdminScreen(navController) }
                composable("edit_accounts") { EditAccountsScreen(navController) }
                composable("edit_agencies") { EditAgencyScreen(navController) }
                composable("login_screen") { LoginScreen(navController) }
            }
        }
    }
}

@Composable
fun MainSudoAdminScreen(navController: NavController) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val chartEntries = listOf(
        FloatEntry(0f, 10f), FloatEntry(1f, 15f), FloatEntry(2f, 20f),
        FloatEntry(3f, 18f), FloatEntry(4f, 25f), FloatEntry(5f, 30f)
    )
    val chartData = ChartEntryModelProducer(listOf(chartEntries))

    val weeklyEntries = listOf(
        FloatEntry(0f, 50f), FloatEntry(1f, 70f), FloatEntry(2f, 80f),
        FloatEntry(3f, 60f), FloatEntry(4f, 90f), FloatEntry(5f, 110f), FloatEntry(6f, 100f)
    )
    val weeklyData = ChartEntryModelProducer(listOf(weeklyEntries))

    val avgAppointments = 146
    val totalAppointments = 2302
    val growthPercentage = 12

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = "Фон",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        val context = LocalContext.current
        val sharedPref = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        val fullName = remember { mutableStateOf(sharedPref.getString("fullName", "Имя не найдено")) }

        Column(modifier = Modifier.fillMaxSize()) {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.mipmap.medical),
                            contentDescription = "Иконка приложения",
                            modifier = Modifier
                                .height(32.dp)
                                .padding(end = 10.dp)
                        )
                        Text(
                            text = fullName.value ?: "Имя не найдено",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Главная") },
                                onClick = { navController.navigate("main_sudo_admin") },
                                leadingIcon = {
                                    Icon(Icons.Default.Home, contentDescription = "Главная")
                                }
                            )
//                            DropdownMenuItem(
//                                text = { Text("Управление Мед центрами") },
//                                onClick = { navController.navigate("add_med_center") },
//                                leadingIcon = {
//                                    Icon(Icons.Default.AddBusiness, contentDescription = "Управление Мед центрами")
//                                }
//                            )
                            DropdownMenuItem(
                                text = { Text("Модерация аккаунтов") },
                                onClick = { navController.navigate("edit_accounts") },
                                leadingIcon = {
                                    Icon(Icons.Default.People, contentDescription = "Модерация аккаунтов")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Модерация учреждений") },
                                onClick = { navController.navigate("edit_agencies") },
                                leadingIcon = {
                                    Icon(Icons.Default.AddBusiness, contentDescription = "Модерация учреждений")
                                }
                            )
//                            DropdownMenuItem(
//                                text = { Text("Отчёты") },
//                                onClick = { menuExpanded = false },
//                                leadingIcon = {
//                                    Icon(Icons.Default.Assessment, contentDescription = "Отчёты")
//                                }
//                            )
                            val context = LocalContext.current // Объявляем контекст один раз в Composable

                            DropdownMenuItem(
                                text = { Text("Выйти из аккаунта") },
                                onClick = {
                                    // Очищаем SharedPreferences
                                    val sharedPref = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                                    with(sharedPref.edit()) {
                                        clear() // Очищаем все данные
                                        apply() // Применяем изменения
                                    }
                                    // Переходим на экран входа
                                    navController.navigate("login_screen") {
                                        // Очищаем back stack, чтобы нельзя было вернуться назад
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = true
                                        }
                                    }
                                    menuExpanded = false // Закрываем меню
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Выйти из аккаунта")
                                }
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Аналитика приёмов", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AnalyticsCard(title = "В текущий день", value = "$avgAppointments", modifier = Modifier.width(120.dp))
                    AnalyticsCard(title = "За месяц", value = "$totalAppointments", modifier = Modifier.width(120.dp))
                    AnalyticsCard("Рост заболеваемости", "$growthPercentage%")
                }

                Spacer(modifier = Modifier.height(16.dp))
                LineChartSection(chartData, "График посещений за месяц")
                Spacer(modifier = Modifier.height(16.dp))
                LineChartSection(weeklyData, "График посещений за неделю")
            }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun LineChartSection(chartData: ChartEntryModelProducer, title: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Chart(
                chart = lineChart(),
                model = chartData.getModel()!!,
                modifier = Modifier.height(200.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainSudoAdminScreenPreview() {
    val navController = rememberNavController()
    MainSudoAdminScreen(navController)
}