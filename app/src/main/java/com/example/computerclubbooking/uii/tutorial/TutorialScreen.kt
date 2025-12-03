package com.example.computerclubbooking.uii.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun TutorialScreen(
    onFinish: () -> Unit
) {
    val pages = remember { tutorialPages() }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF02091A),
                        Color(0xFF001F3F),
                        Color(0xFF7A00FF)
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(32.dp))

        // ---------- ПАРАЛЛАКС + 3D ----------
        HorizontalPager(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            state = pagerState
        ) { page ->

            val pageOffset =
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

            val scale = 1f - 0.18f * abs(pageOffset)
            val alpha = 1f - 0.35f * abs(pageOffset)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        translationX = pageOffset * size.width * 0.25f
                    },
                contentAlignment = Alignment.Center
            ) {
                TutorialPage(page = pages[page])
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------- ТОЧКИ-ИНДИКАТОРЫ ----------
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .height(8.dp)
                        .width(if (selected) 22.dp else 8.dp)
                        .background(
                            color = if (selected) Color(0xFF00C6FF) else Color(0xFF445577),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---------- КНОПКИ ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // "Пропустить" — сразу завершить
            TextButton(onClick = { onFinish() }) {
                Text(text = "Пропустить", color = Color(0xFFB0C4FF), fontSize = 14.sp)
            }

            val isLastPage = pagerState.currentPage == pages.lastIndex

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C6FF),
                    contentColor = Color(0xFF020814)
                )
            ) {
                Text(
                    text = if (isLastPage) "Начать" else "Далее",
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
