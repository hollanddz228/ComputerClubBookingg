package com.example.computerclubbooking

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.computerclubbooking.uii.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // 🔹 Фон в стиле PS5 (тёмный + неоновый градиент)
            val neonGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF001F3F),
                    Color(0xFF0040FF),
                    Color(0xFF7A00FF)
                )
            )

            MaterialTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(neonGradient), // Убираем белую полоску сверху
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    bottomBar = {
                        if (currentRoute in listOf("main", "bookings", "profile")) {
                            BottomNavigationBar(navController, currentRoute)
                        }
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "auth",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        // 🔹 Авторизация
                        composable("auth") {
                            AuthScreen(
                                onLoginClick = { email, password ->
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Добро пожаловать!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                navController.navigate("main") {
                                                    popUpTo("auth") { inclusive = true }
                                                }
                                            } else {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Ошибка входа: ${task.exception?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                },
                                onNavigateToRegister = { navController.navigate("register") },
                                onForgotPasswordClick = { navController.navigate("sbros") }
                            )
                        }

                        // 🔹 Регистрация
                        composable("register") {
                            RegisterScreen(
                                onRegisterClick = { email, password ->
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                auth.currentUser?.sendEmailVerification()
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Аккаунт создан! Проверь почту для подтверждения.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                navController.navigate("verify")
                                            } else {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Ошибка: ${task.exception?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                },
                                onNavigateToLogin = { navController.navigate("auth") }
                            )
                        }

                        // 🔹 Подтверждение почты
                        composable("verify") {
                            VerifyScreen(
                                onVerified = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Почта подтверждена! Добро пожаловать!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigate("main")
                                },
                                onBackToLogin = { navController.navigate("auth") }
                            )
                        }

                        // 🔹 Сброс пароля
                        composable("sbros") {
                            SbrosScreen(
                                onContinueToParol = { navController.navigate("parol") },
                                onBackToLogin = { navController.navigate("auth") }
                            )
                        }

                        // 🔹 Смена пароля
                        composable("parol") {
                            ParolScreen(
                                onPasswordChanged = {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Пароль успешно изменён!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate("auth")
                                },
                                onBackToLogin = { navController.navigate("auth") }
                            )
                        }

                        // 🔹 Главные вкладки
                        composable("main") { MainScreen(navController) }
                        composable("bookings") { BookingsScreen() }
                        composable("profile") {
                            ProfileScreen(
                                onLogout = {
                                    auth.signOut()
                                    navController.navigate("auth") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = true
                                        }
                                    }
                                },
                                onChangePassword = { navController.navigate("parol") }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------
// 🔹 Нижнее меню с glow-эффектом
// ------------------------
@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    val items = listOf(
        NavItem("main", "Главная", Icons.Default.Home),
        NavItem("bookings", "Брони", Icons.Default.List),
        NavItem("profile", "Профиль", Icons.Default.Person)
    )

    NavigationBar(
        containerColor = Color(0xFF0A0F1F),
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (selected) Color(0xFF00C6FF) else Color(0xFFB0B0B0),
                        modifier = Modifier
                    )
                },
                label = {
                    Text(
                        item.label,
                        style = TextStyle(
                            color = if (selected) Color(0xFF00C6FF) else Color(0xFFB0B0B0),
                            shadow = Shadow(
                                color = if (selected) Color(0xFF00C6FF) else Color.Transparent,
                                blurRadius = if (selected) 16f else 0f
                            )
                        )
                    )
                }
            )
        }
    }
}

// ------------------------
// 🔹 Модель нижнего пункта
// ------------------------
data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
