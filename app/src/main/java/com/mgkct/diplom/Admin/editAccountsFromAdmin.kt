package com.mgkct.diplom.Admin

import MainAdminScreen
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberImagePainter
import com.mgkct.diplom.*
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.mgkct.diplom.SudoAdmin.ConfirmDeleteDialog
import com.mgkct.diplom.SudoAdmin.UserItem
import kotlinx.coroutines.delay

class EditAccountsFromAdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "edit_accounts_admin") {
                composable("edit_accounts_admin") { EditAccountsFromAdminScreen(navController) }
                composable("login_screen") { LoginScreen(navController) }
                composable("main_admin") { MainAdminScreen(navController) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountsFromAdminScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    val currentMedCenterId = sharedPreferences.getInt("medCenterId", -1)

    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<ApiService.User?>(null) }
    var userToEdit by remember { mutableStateOf<ApiService.User?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val usersList = remember { mutableStateListOf<ApiService.User>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(refreshTrigger) {
        try {
            val users = RetrofitInstance.api.getUsers()
            usersList.clear()
            // Фильтруем пользователей по medCenterId и исключаем админов
            usersList.addAll(users.filter {
                it.medCenterId == currentMedCenterId && it.role != "admin"
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val filteredUsers = usersList.filter {
        (it.fullName ?: "").contains(searchQuery, ignoreCase = true) ||
                (it.email ?: "").contains(searchQuery, ignoreCase = true) ||
                (it.centerName ?: "").contains(searchQuery, ignoreCase = true) ||
                (it.key ?: "").contains(searchQuery, ignoreCase = true)
    }

    // Остальной код остается без изменений
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Управление аккаунтами центра") },
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
                Icon(Icons.Default.Add, contentDescription = "Добавить пользователя")
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
                    label = { Text("Поиск пользователей") },
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

                if (filteredUsers.isEmpty() && searchQuery.isNotEmpty()) {
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
                        items(filteredUsers) { user ->
                            UserItem(
                                user = user,
                                onDelete = { showDeleteDialog = user },
                                onEdit = { userToEdit = it }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDialog || userToEdit != null) {
        AddOrEditUserDialog(
            user = userToEdit,
            currentMedCenterId = currentMedCenterId, // Передаем currentMedCenterId в диалог
            onDismiss = {
                showDialog = false
                userToEdit = null
            },
            onSaveUser = { user ->
                showDialog = false
                userToEdit = null
                coroutineScope.launch {
                    try {
                        if (user.id == 0) {
                            val newUser = RetrofitInstance.api.addUser(user)
                            usersList.add(newUser)
                            snackbarHostState.showSnackbar("Пользователь ${user.fullName} успешно добавлен")
                        } else {
                            val updatedUser = RetrofitInstance.api.updateUser(user.id, user)
                            val index = usersList.indexOfFirst { it.id == user.id }
                            if (index != -1) {
                                usersList[index] = updatedUser
                            }
                            snackbarHostState.showSnackbar("Информация о пользователе ${user.fullName} обновлена")
                        }
                        refreshTrigger++
                    } catch (e: Exception) {
                        e.printStackTrace()
                        snackbarHostState.showSnackbar("Ошибка при сохранении данных пользователя")
                    }
                }
            }
        )
    }

    if (showDeleteDialog != null) {
        ConfirmDeleteDialog(
            user = showDeleteDialog!!,
            onDismiss = { showDeleteDialog = null },
            onConfirmDelete = {
                coroutineScope.launch {
                    try {
                        RetrofitInstance.api.deleteUser(it.id)
                        usersList.remove(it)
                        snackbarHostState.showSnackbar("Пользователь ${it.fullName} был удален")
                        refreshTrigger++
                    } catch (e: Exception) {
                        e.printStackTrace()
                        snackbarHostState.showSnackbar("Ошибка при удалении пользователя")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditUserDialog(
    user: ApiService.User?,
    currentMedCenterId: Int,
    onDismiss: () -> Unit,
    onSaveUser: (ApiService.User) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var isKeyDecrypted by remember { mutableStateOf(user == null) }
    var isLoading by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("user") }
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("user", "doctor")
    var workType by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    // Сброс значений при открытии диалога для нового пользователя или редактирования
    LaunchedEffect(user) {
        key = user?.key ?: ""
        fullName = user?.fullName ?: ""
        email = user?.email ?: ""
        address = user?.address ?: ""
        role = user?.role ?: "user"
        workType = user?.work_type ?: ""
        experience = user?.experience ?: ""
        category = user?.category ?: ""
        // Расшифровка ключа
        if (user != null && user.key != null && !isKeyDecrypted) {
            isLoading = true
            try {
                val response = RetrofitInstance.api.decryptKey(ApiService.DecryptKeyRequest(user.key))
                key = response.plain_key
                isKeyDecrypted = true
            } catch (e: Exception) {
                key = "Ошибка расшифровки"
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (user == null) "Добавить пользователя" else "Редактировать пользователя") },
            text = {
                if (isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = key,
                                onValueChange = { key = it },
                                label = { Text("Ключ лицензии") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("ФИО") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Адрес") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    modifier = Modifier.menuAnchor(),
                                    readOnly = true,
                                    value = role,
                                    onValueChange = {},
                                    label = { Text("Роль") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    roles.forEach { roleOption ->
                                        DropdownMenuItem(
                                            text = { Text(roleOption) },
                                            onClick = {
                                                role = roleOption
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            if (role == "doctor") {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = workType,
                                    onValueChange = { workType = it },
                                    label = { Text("Профилизация") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = experience,
                                    onValueChange = { experience = it },
                                    label = { Text("Опыт работы") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { category = it },
                                    label = { Text("Категория") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            var encryptedKey = key
                            if (!isLoading) {
                                try {
                                    // Если это новый пользователь или ключ был изменён, шифруем
                                    if (user == null || user.key != key) {
                                        val response = RetrofitInstance.api.encryptKey(ApiService.EncryptKeyRequest(key))
                                        encryptedKey = response.encrypted_key
                                    }
                                } catch (e: Exception) {
                                    // Можно показать ошибку, если нужно
                                }
                                val newUser = ApiService.User(
                                    id = user?.id ?: 0,
                                    key = encryptedKey,
                                    fullName = fullName,
                                    email = email,
                                    address = address,
                                    role = role,
                                    medCenterId = currentMedCenterId,
                                    tgId = user?.tgId,
                                    work_type = if (role == "doctor") workType else null,
                                    experience = if (role == "doctor") experience else null,
                                    category = if (role == "doctor") category else null
                                )
                                onSaveUser(newUser)
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
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
}

