package com.mgkct.diplom.SudoAdmin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import io.ktor.websocket.Frame
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAgencyScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<ApiService.MedicalCenter?>(null) }
    var centerToEdit by remember { mutableStateOf<ApiService.MedicalCenter?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val centersList = remember { mutableStateListOf<ApiService.MedicalCenter>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(refreshTrigger) {
        try {
            val centers = RetrofitInstance.api.getMedicalCenters()
            centersList.clear()
            centersList.addAll(centers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val filteredCenters = centersList.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.address.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Управление учреждениями") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить учреждение")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.background_image),
                contentDescription = "Background Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск учреждений") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (filteredCenters.isEmpty() && searchQuery.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            "Ничего не найдено",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Image(
                            painter = rememberImagePainter(R.drawable.undefined),
                            contentDescription = "Animated GIF",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(300.dp)
                        )
                    }
                } else {
                    LazyColumn {
                        items(filteredCenters) { center ->
                            MedicalCenterItem(
                                center = center,
                                onDelete = { showDeleteDialog = center },
                                onEdit = { centerToEdit = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDialog || centerToEdit != null) {
        AddOrEditCenterDialog(
            center = centerToEdit,
            onDismiss = {
                showDialog = false
                centerToEdit = null
            },
            onSaveCenter = { center ->
                showDialog = false
                centerToEdit = null
                coroutineScope.launch {
                    try {
                        if (center.id == 0) {
                            val newCenter = RetrofitInstance.api.createMedicalCenter(center)
                            centersList.add(newCenter)
                            snackbarHostState.showSnackbar("Учреждение ${center.name} успешно добавлено")
                        } else {
                            RetrofitInstance.api.updateMedicalCenter(center.id, center)
                            val index = centersList.indexOfFirst { it.id == center.id }
                            if (index != -1) {
                                centersList[index] = center
                            }
                            snackbarHostState.showSnackbar("Учреждение ${center.name} успешно обновлено")
                        }
                        refreshTrigger++
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Ошибка при сохранении учреждения: ${e.message}")
                    }
                }
            }
        )
    }

    if (showDeleteDialog != null) {
        ConfirmDeleteCenterDialog(
            center = showDeleteDialog!!,
            onDismiss = { showDeleteDialog = null },
            onConfirmDelete = {
                coroutineScope.launch {
                    try {
                        RetrofitInstance.api.deleteMedicalCenter(it.id)
                        centersList.remove(it)
                        snackbarHostState.showSnackbar("Учреждение ${it.name} удалено")
                        refreshTrigger++
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Ошибка при удалении учреждения")
                    }
                }
            }
        )
    }
}

@Composable
fun MedicalCenterItem(center: ApiService.MedicalCenter, onDelete: () -> Unit, onEdit: (ApiService.MedicalCenter) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text("Название: ${center.name}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text("Адрес: ${center.address}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis)
                Text("Телефон: ${center.phone}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5)
                center.description?.let {
                    Text("Описание: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis)
                }
            }

            Column {
                IconButton(
                    onClick = { onEdit(center) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        modifier = Modifier.size(35.dp)
                    )
                }
                Spacer(modifier = Modifier.height(25.dp)) // Добавил отступ между иконками
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        modifier = Modifier.size(35.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddOrEditCenterDialog(
    center: ApiService.MedicalCenter?,
    onDismiss: () -> Unit,
    onSaveCenter: (ApiService.MedicalCenter) -> Unit
) {
    var name by remember { mutableStateOf(center?.name ?: "") }
    var address by remember { mutableStateOf(center?.address ?: "") }
    var phone by remember { mutableStateOf(center?.phone ?: "") }
    var description by remember { mutableStateOf(center?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (center == null) "Добавить учреждение" else "Редактировать учреждение") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название учреждения") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val newCenter = ApiService.MedicalCenter(
                    id = center?.id ?: 0,
                    name = name,
                    address = address,
                    phone = phone,
                    description = description.ifEmpty { null }
                )
                onSaveCenter(newCenter)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun ConfirmDeleteCenterDialog(
    center: ApiService.MedicalCenter,
    onDismiss: () -> Unit,
    onConfirmDelete: (ApiService.MedicalCenter) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Подтверждение удаления") },
        text = { Text("Вы уверены, что хотите удалить учреждение ${center.name}?") },
        confirmButton = {
            Button(onClick = {
                onConfirmDelete(center)
                onDismiss()
            }) {
                Text("Удалить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}