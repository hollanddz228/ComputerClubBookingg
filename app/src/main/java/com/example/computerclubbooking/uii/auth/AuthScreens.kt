//package com.example.computerclubbooking
//
//import android.widget.Toast
//import androidx.compose.animation.Crossfade
//import androidx.compose.animation.core.animateColorAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Email
//import androidx.compose.material.icons.filled.Lock
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.graphics.Brush
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.input.KeyboardOptions
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.text.input.PasswordVisualTransformation
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavHostController
//import com.google.firebase.auth.FirebaseAuth
//import kotlinx.coroutines.delay
//
//@Composable
//fun AuthScreen(navController: NavHostController, auth: FirebaseAuth) {
//    var showLogin by remember { mutableStateOf(true) }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(
//                Brush.verticalGradient(listOf(Color(0xFF1E3C72), Color(0xFF2A5298)))
//            ),
//        contentAlignment = Alignment.Center
//    ) {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth(0.92f)
//                .wrapContentHeight()
//                .shadow(12.dp, RoundedCornerShape(16.dp)),
//            shape = RoundedCornerShape(16.dp),
//            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724).copy(alpha = 0.75f))
//        ) {
//            Column(modifier = Modifier.padding(20.dp)) {
//                // Header
//                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
//                    Text("Computer Club", color = Color(0xFF00E5FF), style = MaterialTheme.typography.headlineMedium)
//                    Spacer(modifier = Modifier.height(6.dp))
//                    Text(if (showLogin) "Добро пожаловать — войдите" else "Создайте аккаунт", color = Color.LightGray, fontSize = 14.sp)
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Switch
//                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
//                    FilterChip(selected = showLogin, onClick = { showLogin = true }, label = { Text("Вход") })
//                    FilterChip(selected = !showLogin, onClick = { showLogin = false }, label = { Text("Регистрация") })
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Content switch — простой Crossfade (надёжно и компилируется)
//                Crossfade(targetState = showLogin) { isLogin ->
//                    if (isLogin) {
//                        LoginForm(onLogin = { email, pass ->
//                            val ctx = LocalContext.current
//                            if (email.isBlank() || pass.isBlank()) {
//                                Toast.makeText(ctx, "Заполните поля", Toast.LENGTH_SHORT).show()
//                                return@LoginForm
//                            }
//                            auth.signInWithEmailAndPassword(email.trim(), pass)
//                                .addOnCompleteListener { task ->
//                                    if (task.isSuccessful) {
//                                        Toast.makeText(ctx, "Успешный вход", Toast.LENGTH_SHORT).show()
//                                        navController.navigate("main") { popUpTo("auth") { inclusive = true } }
//                                    } else {
//                                        Toast.makeText(ctx, "Ошибка: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
//                                    }
//                                }
//                        }, onGoToRegister = { showLogin = false })
//                    } else {
//                        RegisterForm(onRegister = { email, pass ->
//                            val ctx = LocalContext.current
//                            if (email.isBlank() || pass.isBlank()) {
//                                Toast.makeText(ctx, "Заполните поля", Toast.LENGTH_SHORT).show()
//                                return@RegisterForm
//                            }
//                            auth.createUserWithEmailAndPassword(email.trim(), pass)
//                                .addOnCompleteListener { task ->
//                                    if (task.isSuccessful) {
//                                        Toast.makeText(ctx, "Регистрация успешна", Toast.LENGTH_SHORT).show()
//                                        navController.navigate("main") { popUpTo("auth") { inclusive = true } }
//                                    } else {
//                                        Toast.makeText(ctx, "Ошибка: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
//                                    }
//                                }
//                        }, onGoToLogin = { showLogin = true })
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun LoginForm(onLogin: (String, String) -> Unit, onGoToRegister: () -> Unit) {
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//
//    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
//        Text("Вход", color = Color.White, fontSize = 22.sp)
//        Spacer(modifier = Modifier.height(14.dp))
//
//        TextField(
//            value = email,
//            onValueChange = { email = it },
//            label = { Text("Email") },
//            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth(),
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//
//        TextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Пароль") },
//            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth(),
//            visualTransformation = PasswordVisualTransformation(),
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//
//        NeonButton(text = "Войти", onClick = { onLogin(email, password) })
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        TextButton(onClick = onGoToRegister) {
//            Text("Нет аккаунта? Зарегистрироваться", color = Color.White)
//        }
//    }
//}
//
//@Composable
//private fun RegisterForm(onRegister: (String, String) -> Unit, onGoToLogin: () -> Unit) {
//    var email by remember { mutableStateOf("") }
//    var password by remember { mutableStateOf("") }
//
//    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
//        Text("Регистрация", color = Color.White, fontSize = 22.sp)
//        Spacer(modifier = Modifier.height(14.dp))
//
//        TextField(
//            value = email,
//            onValueChange = { email = it },
//            label = { Text("Email") },
//            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth(),
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//
//        TextField(
//            value = password,
//            onValueChange = { password = it },
//            label = { Text("Пароль") },
//            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
//            singleLine = true,
//            modifier = Modifier.fillMaxWidth(),
//            visualTransformation = PasswordVisualTransformation(),
//            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//
//        NeonButton(text = "Зарегистрироваться", onClick = { onRegister(email, password) })
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        TextButton(onClick = onGoToLogin) {
//            Text("Уже есть аккаунт? Войти", color = Color.White)
//        }
//    }
//}
//
//@Composable
//fun NeonButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
//    var pressed by remember { mutableStateOf(false) }
//
//    val borderColor by animateColorAsState(
//        targetValue = if (pressed) Color(0xFF00E5FF) else Color(0xFF00B7C2),
//        animationSpec = tween(220)
//    )
//
//    Box(
//        modifier = modifier
//            .fillMaxWidth()
//            .height(52.dp)
//            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
//            .shadow(8.dp, RoundedCornerShape(12.dp))
//    ) {
//        Button(
//            onClick = {
//                pressed = true
//                onClick()
//            },
//            modifier = Modifier.fillMaxSize(),
//            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
//            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
//            shape = RoundedCornerShape(12.dp)
//        ) {
//            Text(text = text, color = Color.White)
//        }
//    }
//
//    LaunchedEffect(pressed) {
//        if (pressed) {
//            delay(180)
//            pressed = false
//        }
//    }
//}
