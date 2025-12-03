package com.example.computerclubbooking

import com.example.computerclubbooking.data.models.SavedAccount
import com.example.computerclubbooking.data.models.managers.SavedAccountsManager
import com.google.firebase.firestore.FirebaseFirestore
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
import com.example.computerclubbooking.uii.auth.AuthScreen
import com.example.computerclubbooking.uii.auth.VerifyScreen
import com.example.computerclubbooking.uii.booking.BookingsScreen
import com.example.computerclubbooking.uii.home.MainScreen
import com.example.computerclubbooking.uii.profile.ProfileScreen

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

import com.example.computerclubbooking.uii.tutorial.AppPreferences
import com.example.computerclubbooking.uii.tutorial.TutorialScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        setContent {

            val navController = rememberNavController()

            // Смотрим, был ли показан туториал
            val tutorialShown by AppPreferences
                .isTutorialShown(this)
                .collectAsState(initial = false)

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val neonGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF001F3F),
                    Color(0xFF0040FF),
                    Color(0xFF7A00FF)
                )
            )

            val appScope = rememberCoroutineScope()
            val context = this@MainActivity

            MaterialTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(neonGradient),
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
                        // ВСЕГДА начинаем с auth, а не tutorial
                        startDestination = "auth",
                        modifier = Modifier.padding(paddingValues)
                    ) {

                        // ---------- TUTORIAL ----------
                        composable("tutorial") {
                            TutorialScreen(
                                onFinish = {
                                    appScope.launch {
                                        AppPreferences.setTutorialShown(context, true)
                                        navController.navigate("main") {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // ---------- AUTH ----------
                        composable("auth") {

                            val db = FirebaseFirestore.getInstance()

                            AuthScreen(
                                onLoginClick = { email, password, remember ->

                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->

                                            if (task.isSuccessful) {
                                                val user = auth.currentUser
                                                if (user != null) {

                                                    val docId = user.email ?: user.uid

                                                    db.collection("users")
                                                        .document(docId)
                                                        .get()
                                                        .addOnSuccessListener { doc ->

                                                            val displayName =
                                                                doc.getString("name")
                                                                    ?: user.email?.substringBefore("@")
                                                                    ?: email

                                                            val avatarUrl =
                                                                doc.getString("avatarUrl")

                                                            if (remember) {
                                                                val account = SavedAccount(
                                                                    email = user.email ?: email,
                                                                    displayName = displayName,
                                                                    avatarUrl = avatarUrl,
                                                                    password = password
                                                                )

                                                                SavedAccountsManager.saveOrUpdateAccount(
                                                                    context,
                                                                    account
                                                                )
                                                            }

                                                            Toast.makeText(
                                                                context,
                                                                "Добро пожаловать, $displayName!",
                                                                Toast.LENGTH_SHORT
                                                            ).show()

                                                            // ➜ после логина решаем: tutorial или main
                                                            if (!tutorialShown) {
                                                                navController.navigate("tutorial") {
                                                                    popUpTo("auth") { inclusive = true }
                                                                }
                                                            } else {
                                                                navController.navigate("main") {
                                                                    popUpTo("auth") { inclusive = true }
                                                                }
                                                            }
                                                        }
                                                        .addOnFailureListener {

                                                            val displayName =
                                                                user.email?.substringBefore("@")
                                                                    ?: email

                                                            if (remember) {
                                                                val account = SavedAccount(
                                                                    email = user.email ?: email,
                                                                    displayName = displayName,
                                                                    avatarUrl = null,
                                                                    password = password
                                                                )

                                                                SavedAccountsManager.saveOrUpdateAccount(
                                                                    context,
                                                                    account
                                                                )
                                                            }

                                                            if (!tutorialShown) {
                                                                navController.navigate("tutorial") {
                                                                    popUpTo("auth") { inclusive = true }
                                                                }
                                                            } else {
                                                                navController.navigate("main") {
                                                                    popUpTo("auth") { inclusive = true }
                                                                }
                                                            }
                                                        }
                                                }

                                            } else {
                                                Toast.makeText(
                                                    context,
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

                        // ---------- REGISTER ----------
                        composable("register") {
                            RegisterScreen(
                                onRegisterClick = { email, password ->
                                    auth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                auth.currentUser?.sendEmailVerification()

                                                Toast.makeText(
                                                    context,
                                                    "Аккаунт создан! Подтверди почту.",
                                                    Toast.LENGTH_LONG
                                                ).show()

                                                navController.navigate("verify")
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Ошибка: ${task.exception?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                },
                                onNavigateToLogin = { navController.navigate("auth") }
                            )
                        }

                        composable("verify") {
                            VerifyScreen(
                                onVerified = {
                                    Toast.makeText(
                                        context,
                                        "Почта подтверждена!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigate("main")
                                },
                                onBackToLogin = { navController.navigate("auth") }
                            )
                        }

                        composable("sbros") {
                            SbrosScreen(
                                onContinueToParol = { navController.navigate("parol") },
                                onBackToLogin = { navController.navigate("auth") }
                            )
                        }

                        composable("parol") {
                            ParolScreen(
                                onPasswordChanged = {
                                    Toast.makeText(
                                        context,
                                        "Пароль успешно изменён!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate("auth")
                                },
                                onBackToLogin = { navController.navigate("auth") }
                            )
                        }

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

// ------------------------------------------------------
// 🔥 НИЖНЕЕ МЕНЮ
// ------------------------------------------------------
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

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
