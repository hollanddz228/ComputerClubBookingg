package com.example.computerclubbooking.uii

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerifyScreen(
    onVerified: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val coroutineScope = rememberCoroutineScope()
    var timeLeft by remember { mutableStateOf(60) }
    var isResendEnabled by remember { mutableStateOf(false) }

    // Таймер
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