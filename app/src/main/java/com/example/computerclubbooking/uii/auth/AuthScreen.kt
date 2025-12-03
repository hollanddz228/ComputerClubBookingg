package com.example.computerclubbooking.uii.auth

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.computerclubbooking.data.models.SavedAccount
import com.example.computerclubbooking.data.models.managers.SavedAccountsManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

// -------------------------
// 🔹 Экран авторизации
// -------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthScreen(
    onLoginClick: (String, String, Boolean) -> Unit,   // email, pass, remember
    onNavigateToRegister: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // 🔹 Загружаем сохранённые аккаунты
    var savedAccounts by remember {
        mutableStateOf<List<SavedAccount>>(emptyList())
    }

    LaunchedEffect(Unit) {
        savedAccounts = SavedAccountsManager.getAccounts(context)
    }

    // 🔹 Фон в стиле PS5
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A1A),
                        Color(0xFF0D1B3D),
                        Color(0xFF050A16)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Вход в клуб 🎮",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color(0xFF3D8BFF),
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Карусель сохранённых аккаунтов
            if (savedAccounts.isNotEmpty()) {
                SavedAccountsCarousel(
                    accounts = savedAccounts,
                    onAccountClick = { account ->
                        // авто-подстановка + мгновенный логин
                        email = account.email
                        password = account.password
                        onLoginClick(account.email, account.password, false)
                        isLoading = true
                    },
                    onAccountDelete = { account ->
                        SavedAccountsManager.removeAccount(context, account.email)
                        savedAccounts = SavedAccountsManager.getAccounts(context)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 🔹 Поле Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White) },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3D8BFF),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF3D8BFF),
                    focusedLabelColor = Color(0xFF3D8BFF),
                    unfocusedLabelColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Пароль
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль", color = Color.White) },
                visualTransformation = PasswordVisualTransformation(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3D8BFF),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF3D8BFF),
                    focusedLabelColor = Color(0xFF3D8BFF),
                    unfocusedLabelColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 Чекбокс "Сохранить вход"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF3D8BFF),
                        uncheckedColor = Color.White
                    )
                )
                Text(
                    text = "Сохранить вход",
                    color = Color.White,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Забыли пароль?",
                    color = Color(0xFF4AA8FF),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onForgotPasswordClick() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Ошибка
            errorText?.let { msg ->
                Text(
                    text = msg,
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 🔹 Кнопка входа
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorText = "Заполни email и пароль"
                    } else {
                        isLoading = true
                        errorText = null
                        onLoginClick(email.trim(), password, rememberMe)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3D8BFF)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Войти",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 🔹 Ссылка "Нет аккаунта?"
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    ) {
                        append("Нет аккаунта? ")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF3D8BFF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Зарегистрируйся")
                    }
                },
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToRegister() }
            )
        }
    }
}

// -------------------------
// 🔹 Карусель сохранённых аккаунтов
// -------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedAccountsCarousel(
    accounts: List<SavedAccount>,
    onAccountClick: (SavedAccount) -> Unit,
    onAccountDelete: (SavedAccount) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { accounts.size })

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Сохранённые аккаунты",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) { page ->
            val account = accounts[page]
            val pageOffset =
                (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).absoluteValue

            val scale = lerp(0.85f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(0.4f, 1f, 1f - pageOffset.coerceIn(0f, 1f))

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .padding(horizontal = 24.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable { onAccountClick(account) },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF101826)
                    ),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Аватар
                        if (account.avatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(account.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )

                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = Color(0xFF3D8BFF),
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = account.displayName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = account.email,
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Нажми, чтобы войти",
                                color = Color(0xFF4AA8FF),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { onAccountDelete(account) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                        .size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить аккаунт",
                        tint = Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 🔹 Точки-индикаторы
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(accounts.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .size(if (selected) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) Color(0xFF3D8BFF) else Color.Gray
                        )
                )
            }
        }
    }
}

// -------------------------
// 🔹 VerifyScreen (оставил как было)
// -------------------------
@Composable
fun VerifyScreen(
    onVerified: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    var timeLeft by remember { mutableStateOf(60) }
    var isResendEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000)
            timeLeft--
        } else {
            isResendEnabled = true
        }
    }

    Scaffold(containerColor = Color(0xFF101820)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Подтверждение почты 📧",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Мы отправили письмо на ${user?.email}\nПроверь свою почту и подтверди аккаунт.",
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    user?.reload()
                    if (user?.isEmailVerified == true) {
                        onVerified()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
            ) {
                Text("Проверить подтверждение", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    user?.sendEmailVerification()
                    timeLeft = 60
                    isResendEnabled = false
                },
                enabled = isResendEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFFF))
            ) {
                Text(
                    if (isResendEnabled) "Отправить снова"
                    else "Отправить снова через $timeLeft сек",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { onBackToLogin() }) {
                Text("Вернуться ко входу", color = Color.Gray)
            }
        }
    }
}
