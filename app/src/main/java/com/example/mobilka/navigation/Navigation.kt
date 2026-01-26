package com.example.mobilka.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mobilka.ui.screens.AdminScreen
import com.example.mobilka.ui.screens.AuthScreen
import com.example.mobilka.ui.screens.SubscriptionsScreen
import com.example.mobilka.ui.screens.TrainerMainScreen

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Subscriptions : Screen("subscriptions")
    object Admin : Screen("admin")
    object Trainer : Screen("trainer")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Auth.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Subscriptions.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen(
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Subscriptions.route) { inclusive = true }
                    }
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route)
                },
                onNavigateToTrainer = {
                    navController.navigate(Screen.Trainer.route)
                }
            )
        }
        
        composable(Screen.Admin.route) {
            AdminScreen(
                onBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Trainer.route) {
            TrainerMainScreen(
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
