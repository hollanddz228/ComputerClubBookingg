package com.example.computerclubbooking.uii

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onChangePassword: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userEmail = auth.currentUser?.email ?: "guest_user"

    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showNameDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Cloudinary config — замени на свои данные
    val cloudName = "dpnybijnf"
    val uploadPreset = "unsigned_preset"

    // PlayStation-подобный градиент
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0A1A), Color(0xFF0D1B3D), Color(0xFF050A16))
    )

    // Пульсающее свечение
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Лаунчер выбора изображения
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("upload_preset", uploadPreset)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                coroutineScope.launch {
                    Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val imageUrl = JSONObject(body ?: "{}").optString("secure_url")
                if (imageUrl.isNotBlank()) {
                    firestore.collection("users").document(userEmail)
                        .set(mapOf("avatarUrl" to imageUrl), SetOptions.merge())
                        .addOnSuccessListener {
                            avatarUrl = imageUrl
                            Toast.makeText(context, "Аватар обновлён ✅", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Ошибка сохранения URL", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    coroutineScope.launch {
                        Toast.makeText(context, "Cloudinary: не получили ссылку", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Загрузка данных из Firestore
    LaunchedEffect(Unit) {
        firestore.collection("users").document(userEmail).get()
            .addOnSuccessListener { doc ->
                avatarUrl = doc.getString("avatarUrl")
                userName = doc.getString("name") ?: ""
                isLoading = false
            }
            .addOnFailureListener {
                // создаём пустой документ (если нужно)
                firestore.collection("users").document(userEmail)
                    .set(mapOf("avatarUrl" to "", "name" to ""), SetOptions.merge())
                isLoading = false
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // AVATAR
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF00C6FF))),
                        shape = CircleShape
                    )
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color(0xFF00C6FF))
                } else {
                    val image = avatarUrl ?: "https://cdn-icons-png.flaticon.com/512/149/149071.png"
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(image)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Имя (нажатие открывает диалог)
            Text(
                text = userName.ifBlank { "Нажмите, чтобы установить имя" },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showNameDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = userEmail, color = Color(0xFFB0C4DE), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(28.dp))

            // Кнопка "Изменить пароль" с пульсацией
            Button(
                onClick = onChangePassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .graphicsLayer { alpha = glowAlpha },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Text("Изменить пароль", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    auth.signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E2F))
            ) {
                Text("Выйти из аккаунта", color = Color.White)
            }
        }

        // Диалог изменения имени с размытым фоном
        if (showNameDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            AlertDialog(
                onDismissRequest = { showNameDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val trimmed = userName.trim()
                        if (trimmed.isBlank()) {
                            Toast.makeText(context, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                        } else {
                            firestore.collection("users").document(userEmail)
                                .set(mapOf("name" to trimmed), SetOptions.merge())
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Имя обновлено ✅", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Ошибка сохранения: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            showNameDialog = false
                        }
                    }) {
                        Text("Сохранить", color = Color(0xFF00C6FF))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNameDialog = false }) {
                        Text("Отмена", color = Color.Gray)
                    }
                },
                title = { Text("Изменение имени", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { input ->
                            // ✅ Разрешаем буквы любых языков, цифры и пробелы
                            userName = input.filter { ch ->
                                Character.isLetter(ch) || ch.isWhitespace() || ch.isDigit()
                            }
                        },
                        label = { Text("Введите новое имя", color = Color.White) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        visualTransformation = VisualTransformation.None,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color(0xFF00C6FF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                containerColor = Color(0xFF0D1B3D),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
