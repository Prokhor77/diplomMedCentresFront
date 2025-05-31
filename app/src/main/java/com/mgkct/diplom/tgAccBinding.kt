package com.mgkct.diplom.tg

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mgkct.diplom.ApiService
import com.mgkct.diplom.R
import com.mgkct.diplom.RetrofitInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TgAccBindingScreen(navController: NavHostController) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    val userId = sharedPref.getInt("userId", 0)
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tgId by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    LaunchedEffect(key1 = tgId) {
        if (tgId == null) {
            while (tgId == null) {
                delay(2000) // опрашиваем каждые 3 секунды
                try {
                    val resp = RetrofitInstance.api.getUserTgId(userId)
                    if (resp.tgId != null) {
                        tgId = resp.tgId
                        isLoading = false
                        break
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Получаем текущий Telegram ID пользователя
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val resp = RetrofitInstance.api.getUserTgId(userId)
                tgId = resp.tgId
            } catch (e: Exception) {
                // ignore
            } finally {
                isLoading = false
            }
        }
    }

    // Получаем код для привязки, если Telegram не привязан
    LaunchedEffect(tgId) {
        if (tgId == null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val resp = RetrofitInstance.api.tgBindStart(userId)
                    code = resp.code
                    error = null
                } catch (e: Exception) {
                    error = "Ошибка получения кода: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Привязка Telegram", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
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
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (error != null) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Text(
                            text = error ?: "Ошибка",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                } else if (tgId != null) {
                    // Telegram уже привязан
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                        elevation = CardDefaults.cardElevation(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(28.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Ваш Telegram уже привязан",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF1E88E5)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Ваш Telegram ID: $tgId",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF333333)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            RetrofitInstance.api.tgUnlink(
                                                ApiService.TgUnlinkRequest(
                                                    userId
                                                )
                                            )
                                            tgId = null
                                            Toast.makeText(context, "Telegram отвязан", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Ошибка: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Text("Отвязать Telegram")
                            }
                        }
                    }
                } else {
                    // Привязка Telegram
                    Card(
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                        elevation = CardDefaults.cardElevation(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(28.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Привязка Telegram",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF1E88E5)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "1. Перейдите в Telegram-бота по ссылке ниже.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF333333)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Кликабельная ссылка на бота
                            val uriHandler = LocalUriHandler.current
                            val annotatedString = buildAnnotatedString {
                                pushStringAnnotation(
                                    tag = "URL",
                                    annotation = "https://t.me/medCentres_bot"
                                )
                                withStyle(
                                    style = SpanStyle(
                                        color = Color(0xFF1E88E5),
                                        textDecoration = TextDecoration.Underline,
                                        fontSize = 18.sp
                                    )
                                ) {
                                    append("Открыть Telegram-бота")
                                }
                                pop()
                            }
                            ClickableText(
                                text = annotatedString,
                                onClick = { offset ->
                                    annotatedString.getStringAnnotations("URL", offset, offset)
                                        .firstOrNull()?.let { annotation ->
                                            uriHandler.openUri(annotation.item)
                                        }
                                }
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "2. Отправьте боту этот код:",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF333333)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Код и кнопка копирования
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    code,
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color(0xFF1E88E5)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                                        Toast.makeText(context, "Код скопирован", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Скопировать код",
                                        tint = Color(0xFF1E88E5)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "После отправки кода в бота ваш аккаунт будет привязан, и вы получите уведомление.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(horizontal = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}