package com.example.computerclubbooking.uii

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SbrosScreen(
    onContinueToParol: () -> Unit, // нажали "Перейти к смене пароля"
    onBackToLogin: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf(0) }
    var isResendEnabled by remember { mutableStateOf(true) }
    var emailSent by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF101820)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(padding),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Сброс пароля 🔑",
                color = Color.White,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White) },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0066FF),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF00BFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Письмо отправлено ✉️")
                                }
                                emailSent = true
                                timeLeft = 60
                                isResendEnabled = false
                            }
                            .addOnFailureListener {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Ошибка: ${it.message}")
                                }
                            }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Введите email")
                        }
                    }
                },
                enabled = isResendEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isResendEnabled) "Отправить письмо"
                    else "Отправить снова через $timeLeft сек",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // После отправки показываем кнопку — пользователь должен подтвердить письмо в почте,
            // и затем нажать эту кнопку, чтобы перейти к экрану смены пароля.
            if (emailSent) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Письмо отправлено на $email. Проверьте почту и нажмите кнопку ниже после подтверждения.",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onContinueToParol() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Я подтвердил — перейти к смене пароля", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { onBackToLogin() }) {
                Text("Вернуться ко входу", color = Color.Gray, textAlign = TextAlign.Center)
            }
        }

        // Таймер для повторной отправки
        LaunchedEffect(timeLeft) {
            if (timeLeft > 0) {
                delay(1000)
                timeLeft--
                if (timeLeft == 0) isResendEnabled = true
            }
        }
    }
}
