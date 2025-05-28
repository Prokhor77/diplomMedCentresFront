package com.mgkct.diplom.SudoAdmin

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.Assessment
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
import androidx.compose.runtime.produceState
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
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.LoginScreen
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance.api
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.mgkct.diplom.SudoAdmin.EditAccountsScreen
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

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

    val appointmentsWeek by produceState<List<ApiService.DayCount>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { api.getAppointmentsWeek() }
    }
    val inpatientWeek by produceState<List<ApiService.DayCount>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { api.getInpatientWeek() }
    }

    val todayAppointments by produceState<Int?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { getTotalAppointmentsToday(api) }
    }
    val monthAppointments by produceState<Int?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { getTotalAppointmentsMonth(api) }
    }
    val growthData by produceState<Pair<Int, Boolean>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { getGrowthPercentage(api) }
    }

    fun toChartEntries(data: List<ApiService.DayCount>?): List<FloatEntry> {
        return data?.mapIndexed { idx, day -> FloatEntry(idx.toFloat(), day.count.toFloat()) } ?: emptyList()
    }
    fun toLabels(data: List<ApiService.DayCount>?): List<String> {
        return data?.map { it.date.substring(5) } ?: emptyList() // "06-01"
    }




    val appointmentsEntries = toChartEntries(appointmentsWeek)
    val appointmentsLabels = toLabels(appointmentsWeek)
    val appointmentsChartData = ChartEntryModelProducer(listOf(appointmentsEntries))

    val inpatientEntries = toChartEntries(inpatientWeek)
    val inpatientLabels = toLabels(inpatientWeek)
    val inpatientChartData = ChartEntryModelProducer(listOf(inpatientEntries))

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
                            DropdownMenuItem(
                                text = { Text("Управление аккаунтами") },
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
                            DropdownMenuItem(
                                text = { Text("Отчёты") },
                                onClick = { navController.navigate("reports_from_sudo") },
                                leadingIcon = {
                                    Icon(Icons.Default.Assessment, contentDescription = "Отчёты")
                                }
                            )
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
                    AnalyticsCard(
                        title = "В текущий день",
                        value = todayAppointments?.toString() ?: "..."
                    )
                    AnalyticsCard(
                        title = "За месяц",
                        value = monthAppointments?.toString() ?: "..."
                    )
                    AnalyticsCard(
                        title = "Рост посещений",
                        value = growthData?.let { (percent, isGrowth) ->
                            val arrow = if (isGrowth) "↑" else "↓"
                            "$percent% $arrow"
                        } ?: "..."
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                LineChartSection(appointmentsChartData, "График посещений за неделю", appointmentsLabels)
                Spacer(modifier = Modifier.height(16.dp))
                LineChartSection(inpatientChartData, "График стационарного лечения за неделю", inpatientLabels)
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
fun LineChartSection(chartData: ChartEntryModelProducer, title: String, labels: List<String>) {
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
                modifier = Modifier.height(200.dp),
                startAxis = startAxis(),
                bottomAxis = bottomAxis(
                    valueFormatter = { value, _ ->
                        val idx = value.toInt()
                        if (idx in labels.indices) labels[idx] else ""
                    }
                )
            )
        }
    }
}

suspend fun getTotalAppointmentsToday(api: ApiService): Int {
    val centers = api.getMedicalCenters()
    var total = 0
    for (center in centers) {
        val count = api.getAppointmentsToday(center.id).count
        total += count
    }
    return total
}

suspend fun getTotalAppointmentsMonth(api: ApiService): Int {
    val centers = api.getMedicalCenters()
    var total = 0
    for (center in centers) {
        val count = api.getAppointmentsMonth(center.id).count // реализуй этот эндпоинт!
        total += count
    }
    return total
}

@RequiresApi(Build.VERSION_CODES.O)
suspend fun getGrowthPercentage(api: ApiService): Pair<Int, Boolean> {
    val centers = api.getMedicalCenters()
    var today = 0
    var yesterday = 0
    val todayStr = LocalDate.now().toString()
    val yesterdayStr = LocalDate.now().minusDays(1).toString()
    for (center in centers) {
        today += api.getAppointmentsByDate(center.id, todayStr).count
        yesterday += api.getAppointmentsByDate(center.id, yesterdayStr).count
    }
    val percent = if (yesterday == 0) 100 else ((today - yesterday) * 100 / yesterday)
    val isGrowth = percent >= 0
    return Pair(percent, isGrowth)
}

@Preview(showBackground = true)
@Composable
fun MainSudoAdminScreenPreview() {
    val navController = rememberNavController()
    MainSudoAdminScreen(navController)
}