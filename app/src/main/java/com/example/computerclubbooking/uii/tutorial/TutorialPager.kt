package com.example.computerclubbooking.uii.tutorial

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*

@Composable
fun TutorialPager(
    pages: List<@Composable () -> Unit>,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    HorizontalPager(state = pagerState) { page ->
        pages[page]()

        if (pagerState.currentPage == pages.size - 1) {
            LaunchedEffect(Unit) { onFinish() }
        }
    }
}
