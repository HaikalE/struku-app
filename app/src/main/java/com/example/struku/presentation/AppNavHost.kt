package com.example.struku.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * Main navigation host for the app
 * This file contains the main NavHost while the actual navigation graph is in Navigation.kt
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.RECEIPTS,
        modifier = modifier
    ) {
        // Use the mainGraph extension function from Navigation.kt
        mainGraph(navController)
    }
}