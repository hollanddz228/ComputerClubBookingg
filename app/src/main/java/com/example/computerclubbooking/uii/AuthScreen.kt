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
fun AuthScreen(
    onLoginClick: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 🔹 PS4 фон — тёмный с неоновым градиентом
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
                text = "Вход в клуб 🎮",
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

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 Кнопка входа
            Button(
                onClick = { onLoginClick(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3D8BFF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Войти", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 "Забыли пароль?"
            Text(
                text = "Забыли пароль?",
                color = Color(0xFF3D8BFF),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onForgotPasswordClick() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 Ссылка "Нет аккаунта?"
            Text(
                buildAnnotatedString {
                    append("Нет аккаунта? ")
                    withStyle(
                        style = SpanStyle(
                            color = Color(0xFF3D8BFF),
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
