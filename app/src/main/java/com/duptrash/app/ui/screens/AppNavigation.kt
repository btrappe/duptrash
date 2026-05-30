package com.duptrash.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.duptrash.app.MainViewModel

object Routes {
    const val HOME = "home"
    const val DUPLICATES = "duplicates"
    const val PATTERNS = "patterns"
    const val REVIEW = "review"
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(viewModel, nav) }
        composable(Routes.DUPLICATES) { DuplicatesScreen(viewModel, nav) }
        composable(Routes.PATTERNS) { PatternsScreen(viewModel, nav) }
        composable(Routes.REVIEW) { ReviewSkippedScreen(viewModel, nav) }
    }
}
