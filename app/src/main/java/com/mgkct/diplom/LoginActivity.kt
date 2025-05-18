package com.mgkct.diplom

import MainAdminScreen
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mgkct.diplom.SudoAdmin.MainSudoAdminScreen
//import com.mgkct.diplom.SudoAdmin.AddAdmSudoScreen
//import com.mgkct.diplom.SudoAdmin.AddMainDoctorScreen
//import com.mgkct.diplom.SudoAdmin.AddMedCenterScreen
//import com.mgkct.diplom.SudoAdmin.MainSudoAdminScreen
//import com.mgkct.diplom.admin.AddDoctorScreen
//import com.mgkct.diplom.admin.MainAdminScreen
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.mgkct.diplom.Admin.EditAccountsFromAdminScreen
import com.mgkct.diplom.Admin.FeedBackEdit
import com.mgkct.diplom.Admin.ManageInpatientCareScreen
import com.mgkct.diplom.SudoAdmin.EditAccountsScreen
import com.mgkct.diplom.SudoAdmin.EditAgencyScreen
import com.mgkct.diplom.doctor.MainDoctorScreen


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "login_screen"
            ) {
                composable("login_screen") { LoginScreen(navController) }
                composable("edit_accounts") { EditAccountsScreen(navController) }
                composable("main_sudo_admin") { MainSudoAdminScreen(navController) }
                composable("edit_feedback") { FeedBackEdit(navController) }
                composable("doctor") {MainDoctorScreen(navController = navController)}
                composable("edit_agencies") { EditAgencyScreen(navController) }
                composable("manage_inp_care") { ManageInpatientCareScreen(navController) }
                composable("edit_accounts_admin") { EditAccountsFromAdminScreen(navController) }
                composable("main_admin") { MainAdminScreen(navController) }
                composable("doctor_screen") { MainDoctorScreen(navController) }
            }

        }
    }

    companion object {
        const val PREFS_NAME = "AuthPrefs"
    }
}


@Composable
fun LoginScreen(navController: NavController) {
    var licenseKey by remember { mutableStateOf("") } // Состояние для лицензионного ключа
    var errorMessage by remember { mutableStateOf<String?>(null) } // Сообщение об ошибке
    var isLoading by remember { mutableStateOf(false) } // Состояние загрузки
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Фоновое изображение
        Image(
            painter = painterResource(id = R.drawable.background_image),
            contentDescription = "Фон",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Основной контент
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.mipmap.medical),
                contentDescription = "Логотип медицинской платформы",
                modifier = Modifier
                    .size(70.dp)
                    .padding(bottom = 8.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = "Единая платформа для всех медицинских учреждений, обеспечивающая удобное управление, безопасность данных и эффективное взаимодействие между сотрудниками и пациентами.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 20.sp
            )

            // Карточка для формы входа
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Вход",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Поле для ввода лицензионного ключа
                    OutlinedTextField(
                        value = licenseKey,
                        onValueChange = { licenseKey = it },
                        label = { Text("Введите ключ доступа") },
                        placeholder = { Text("Введите ваш лицензионный ключ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Отображение сообщения об ошибке
                    errorMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val context = LocalContext.current

                    Button(
                        onClick = {
                            if (licenseKey.isBlank()) {
                                errorMessage = "Пожалуйста, введите лицензионный ключ"
                                return@Button
                            }

                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val response = RetrofitInstance.api.loginWithKey(
                                        ApiService.LicenseKeyRequest(licenseKey)
                                    )

                                    Log.d("BackendResponse", "Response: $response")

                                    val sharedPref = context.getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                                    with(sharedPref.edit()) {
                                        putString("role", response.role ?: "")
                                        putInt("userId", response.userId ?: 0)
                                        putInt("medCenterId", response.medCenterId ?: 0)
                                        putString("fullName", response.full_name ?: "")
                                        putString("centerName", response.center_name ?: "")
                                        commit()
                                    }

                                    // Добавим логирование сразу после сохранения
                                    Log.d("LoginScreen", "Saved medCenterId: ${response.medCenterId}")
                                    Log.d("LoginScreen", "Saved fullName: ${response.full_name}")
                                    Log.d("LoginScreen", "Saved centerName: ${response.center_name}")
                                    when (response.role) {
                                        "sudo-admin" -> navController.navigate("main_sudo_admin")
                                        "admin" -> navController.navigate("main_admin")
                                        "doctor" -> navController.navigate("doctor")
                                        else -> errorMessage = "Неизвестная роль: ${response.role}"
                                    }
                                } catch (e: HttpException) {
                                    val errorBody = e.response()?.errorBody()?.string()
                                    Log.e("BackendError", "HTTP error: ${e.code()}, $errorBody")
                                    errorMessage = "Ошибка сервера: ${e.code()}"
                                } catch (e: IOException) {
                                    Log.e("BackendError", "Network error: ${e.message}")
                                    errorMessage = "Ошибка сети: ${e.localizedMessage ?: "Проверьте подключение"}"
                                } catch (e: Exception) {
                                    Log.e("BackendError", "Unexpected error: ${e.stackTraceToString()}")
                                    errorMessage = "Ошибка: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text("Войти")
                        }
                    }

                }
            }
        }
    }
}

// Предварительный просмотр экрана входа
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    val navController = rememberNavController()
    LoginScreen(navController)
}