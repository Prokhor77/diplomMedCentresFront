package com.mgkct.diplom.Admin

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EditDocWorkTimeScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    val medCenterId = sharedPref.getInt("medCenterId", 0)
    val scope = rememberCoroutineScope()

    var doctors by remember { mutableStateOf<List<DoctorListItem>>(emptyList()) }
    var filteredDoctors by remember { mutableStateOf<List<DoctorListItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDoctor by remember { mutableStateOf<DoctorListItem?>(null) }
    var schedule by remember { mutableStateOf<List<ReceptionSlot>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Загрузка врачей при старте
    LaunchedEffect(medCenterId) {
        loading = true
        try {
            val users = RetrofitInstance.api.getUsers()
            doctors = users.filter { it.role == "doctor" && it.medCenterId == medCenterId }
                .map { DoctorListItem(it.id, it.fullName ?: "Без имени", it.work_type ?: "") }
            filteredDoctors = doctors
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка загрузки врачей", Toast.LENGTH_SHORT).show()
        }
        loading = false
    }

    // Фильтрация врачей при изменении поискового запроса
    LaunchedEffect(searchQuery, doctors) {
        filteredDoctors = if (searchQuery.isEmpty()) {
            doctors
        } else {
            doctors.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.workType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Загрузка расписания выбранного врача
    fun loadSchedule(doctorId: Int) {
        scope.launch {
            loading = true
            try {
                val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                schedule = RetrofitInstance.api.getDoctorAppointments(doctorId, today, null)
                    .map {
                        ReceptionSlot(
                            id = it.id,
                            date = it.time.substringBefore(" "),
                            time = it.time.substringAfter(" "),
                            reason = it.reason ?: "",
                            active = it.active,
                            userId = if (it.userId == 0) null else it.userId
                        )
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка загрузки расписания", Toast.LENGTH_SHORT).show()
            }
            loading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Фон
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = "Фон",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.mipmap.medical),
                                contentDescription = "Иконка",
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Рабочее время врачей")
                        }
                    },
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
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Поисковая строка
                if (selectedDoctor == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        placeholder = { Text("Поиск врача") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Поиск") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                } else if (selectedDoctor == null) {
                    // Список врачей
                    if (filteredDoctors.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Врачи не найдены", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(filteredDoctors) { doctor ->
                                DoctorCard(
                                    doctor = doctor,
                                    onClick = {
                                        selectedDoctor = doctor
                                        loadSchedule(doctor.id)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Отображение расписания выбранного врача
                    DoctorScheduleView(
                        doctor = selectedDoctor!!,
                        schedule = schedule,
                        onBackClick = { selectedDoctor = null },
                        onAddClick = { showAddDialog = true }
                    )
                }
            }
        }

        // Диалог добавления дня приёма
        if (showAddDialog && selectedDoctor != null) {
            AddReceptionDayDialog(
                doctor = selectedDoctor!!,
                onDismiss = { showAddDialog = false },
                onAdd = { date, startTime, endTime, interval ->
                    scope.launch {
                        loading = true
                        try {
                            val slots = generateTimeSlots(startTime, endTime, interval)
                            slots.forEach { time ->
                                RetrofitInstance.api.createAppointment(
                                    doctorId = selectedDoctor!!.id,
                                    userId = 0, // 0 = свободно
                                    date = date,
                                    time = time,
                                    reason = null
                                )
                            }
                            loadSchedule(selectedDoctor!!.id)
                            Toast.makeText(context, "День приёма добавлен", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ошибка добавления", Toast.LENGTH_SHORT).show()
                        }
                        loading = false
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
fun DoctorCard(doctor: DoctorListItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {


            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = doctor.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = doctor.workType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DoctorScheduleView(
    doctor: DoctorListItem,
    schedule: List<ReceptionSlot>,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Заголовок с информацией о враче
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = doctor.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = doctor.workType,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Кнопка добавления дня приёма
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Добавить день приёма")
        }

        // Расписание
        Text(
            text = "График работы",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (schedule.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Расписание не найдено")
                }
            }
        } else {
            // Группируем по дате
            val grouped = schedule.groupBy { it.date }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                grouped.forEach { (date, slots) ->
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(slots) { slot ->
                        AppointmentSlotCard(slot = slot, showDate = false)
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentSlotCard(slot: ReceptionSlot, showDate: Boolean = true) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (slot.userId != null)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (showDate) {
                    Text(
                        text = slot.date,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (slot.userId != null)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = slot.time,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (slot.userId != null)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            AssistChip(
                onClick = { /* handle click if needed */ },
                label = {
                    Text(
                        text = if (slot.userId != null) "Занято" else "Свободно",
                        color = if (slot.userId != null)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (slot.userId != null)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
        }
    }
}

data class DoctorListItem(val id: Int, val fullName: String, val workType: String)
data class ReceptionSlot(val id: Int, val date: String, val time: String, val reason: String, val active: String, val userId: Int?)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddReceptionDayDialog(
    doctor: DoctorListItem,
    onDismiss: () -> Unit,
    onAdd: (date: String, startTime: String, endTime: String, interval: Int) -> Unit
) {
    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("17:00") }
    var interval by remember { mutableStateOf(15) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить день приёма") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Дата (дд.мм.гггг)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Время начала (чч:мм)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Время конца (чч:мм)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = interval.toString(),
                    onValueChange = { interval = it.toIntOrNull() ?: 15 },
                    label = { Text("Интервал (мин)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(date, startTime, endTime, interval) }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

fun generateTimeSlots(start: String, end: String, interval: Int): List<String> {
    val result = mutableListOf<String>()
    var current = start
    while (current < end) {
        result.add(current)
        val (h, m) = current.split(":").map { it.toInt() }
        val total = h * 60 + m + interval
        val nh = total / 60
        val nm = total % 60
        current = "%02d:%02d".format(nh, nm)
        if (current >= end) break
    }
    return result
}