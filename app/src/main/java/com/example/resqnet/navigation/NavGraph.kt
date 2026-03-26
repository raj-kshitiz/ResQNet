package com.example.resqnet.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.resqnet.ui.screens.admin.AdminDashboardScreen
import com.example.resqnet.ui.screens.auth.LoginScreen
import com.example.resqnet.ui.screens.auth.OtpScreen
import com.example.resqnet.ui.screens.history.HistoryScreen
import com.example.resqnet.ui.screens.home.HomeScreen
import com.example.resqnet.ui.screens.home.SOSActiveScreen
import com.example.resqnet.ui.screens.home.SOSTriggerSheet
import com.example.resqnet.ui.screens.onboarding.OnboardingScreen
import com.example.resqnet.ui.screens.profile.ProfileScreen
import com.example.resqnet.ui.screens.volunteer.ActiveResponseScreen
import com.example.resqnet.ui.screens.volunteer.IncomingSosScreen
import com.example.resqnet.ui.screens.volunteer.VolunteerHomeScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Routes.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ── Onboarding ──
        composable(Routes.Onboarding.route) {
            OnboardingScreen(
                onGetStarted = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Auth ──
        composable(Routes.Login.route) {
            LoginScreen(
                onOtpSent = { phone ->
                    navController.navigate(Routes.Otp.createRoute(phone))
                }
            )
        }

        composable(
            route = Routes.Otp.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phone = phone,
                onVerified = { role ->
                    val destination = when (role) {
                        "volunteer" -> Routes.VolunteerHome.route
                        "admin" -> Routes.AdminDashboard.route
                        else -> Routes.Home.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Home (Requester) ──
        composable(Routes.Home.route) {
            HomeScreen(
                onTriggerSos = {
                    navController.navigate(Routes.SosTrigger.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.History.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.Profile.route)
                },
                onNavigateToVolunteerHome = {
                    navController.navigate(Routes.VolunteerHome.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SosTrigger.route) {
            SOSTriggerSheet(
                onSosCreated = { sosId ->
                    navController.navigate(Routes.SosActive.createRoute(sosId)) {
                        popUpTo(Routes.Home.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.SosActive.route,
            arguments = listOf(navArgument("sosId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sosId = backStackEntry.arguments?.getString("sosId") ?: ""
            SOSActiveScreen(
                sosId = sosId,
                onDone = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Volunteer ──
        composable(Routes.VolunteerHome.route) {
            VolunteerHomeScreen(
                onSosTapped = { sosId ->
                    navController.navigate(Routes.IncomingSos.createRoute(sosId))
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.History.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.Profile.route)
                },
                onNavigateToRequesterHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.VolunteerHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.IncomingSos.route,
            arguments = listOf(navArgument("sosId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sosId = backStackEntry.arguments?.getString("sosId") ?: ""
            IncomingSosScreen(
                sosId = sosId,
                onAccepted = {
                    navController.navigate(Routes.ActiveResponse.createRoute(sosId)) {
                        popUpTo(Routes.VolunteerHome.route)
                    }
                },
                onDeclined = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ActiveResponse.route,
            arguments = listOf(navArgument("sosId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sosId = backStackEntry.arguments?.getString("sosId") ?: ""
            ActiveResponseScreen(
                sosId = sosId,
                onDone = {
                    navController.navigate(Routes.VolunteerHome.route) {
                        popUpTo(Routes.VolunteerHome.route) { inclusive = true }
                    }
                }
            )
        }

        // ── History ──
        composable(Routes.History.route) {
            HistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Profile ──
        composable(Routes.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Admin ──
        composable(Routes.AdminDashboard.route) {
            AdminDashboardScreen(
                onNavigateToProfile = {
                    navController.navigate(Routes.Profile.route)
                }
            )
        }
    }
}
