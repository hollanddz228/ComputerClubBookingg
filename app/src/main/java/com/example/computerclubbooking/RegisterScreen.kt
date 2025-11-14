package com.example.computerclubbooking.uii

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // 🔹 Неоновый градиентный фон
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
            // 🔹 Заголовок
            Text(
                text = "Регистрация 👾",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color(0xFF3D8BFF),
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 🔹 Email
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

            Spacer(modifier = Modifier.height(12.dp))

            // 🔹 Подтверждение пароля
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Повторите пароль", color = Color.White) },
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

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 Кнопка регистрации
            Button(
                onClick = {
                    if (password == confirmPassword) {
                        onRegisterClick(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3D8BFF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Зарегистрироваться", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Ссылка "Уже есть аккаунт?"
            Text(
                buildAnnotatedString {
                    append("Уже есть аккаунт? ")
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF3D8BFF),
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Войти")
                    }
                },
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLogin() }
            )
        }
    }
}
