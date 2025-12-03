package com.example.computerclubbooking.uii.tutorial

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.computerclubbooking.R

data class TutorialPageData(
    val resId: Int,
    val title: String,
    val description: String
)

@Composable
fun TutorialPage(page: TutorialPageData) {
    // Без `by`, чтобы не было ошибок getValue
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.RawRes(page.resId)
    )
    val progressState = animateLottieCompositionAsState(
        composition = compositionResult.value,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Lottie
        LottieAnimation(
            composition = compositionResult.value,
            progress = { progressState.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = page.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.description,
            fontSize = 16.sp,
            color = Color(0xFFB0C4FF),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Удобный список страниц под твои JSON’ы
fun tutorialPages(): List<TutorialPageData> = listOf(
    TutorialPageData(
        resId = R.raw.lottie_welcome,
        title = "Добро пожаловать!",
        description = "Лучший компьютерный клуб теперь в твоём кармане. Бронируй ПК за пару тапов."
    ),
    TutorialPageData(
        resId = R.raw.lottie_pc,
        title = "Выбирай мощные ПК",
        description = "Смотри категории, статус занятости и выбирай подходящую машину."
    ),
    TutorialPageData(
        resId = R.raw.lottie_timer,
        title = "Точное время",
        description = "Мы считаем время по серверу Астаны. Никаких читов с часами телефона."
    ),
    TutorialPageData(
        resId = R.raw.lottie_profile,
        title = "Профиль и вход в 1 клик",
        description = "Аватарка из Cloudinary, сохранённые аккаунты и быстрый вход."
    )
)
