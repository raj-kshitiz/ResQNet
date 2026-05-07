package com.example.resqnet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.example.resqnet.ui.screens.admin.AdminDashboardScreen
import com.example.resqnet.ui.screens.auth.AuthViewModel
import com.example.resqnet.ui.screens.auth.LoginScreen
import com.example.resqnet.ui.screens.auth.PermissionScreen
import com.example.resqnet.ui.screens.auth.RegisterScreen
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
    startDestination: String = Routes.Onboarding.route,
    initialSosId: String? = null
) {
    // Single shared AuthViewModel so checkAuthState shares state with Login/Register
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(context)
    )

    // Navigate to IncomingSosScreen when app is opened from a notification
    val currentSosId by rememberUpdatedState(initialSosId)
    LaunchedEffect(currentSosId) {
        val sosId = currentSosId ?: return@LaunchedEffect
        navController.navigate(Routes.VolunteerHome.route) {
            popUpTo(0) { inclusive = true }
        }
        navController.navigate(Routes.IncomingSos.createRoute(sosId))
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ── Onboarding ───────────────────────────────────────────────────────
        composable(Routes.Onboarding.route) {
            // On first launch, show onboarding; then check auth state
            LaunchedEffect(Unit) {
                // no-op here — OnboardingScreen calls onGetStarted itself
            }
            OnboardingScreen(
                onGetStarted = {
                    // After onboarding, check if user is already signed in
                    authViewModel.checkAuthState(
                        onHome = { role ->
                            val dest = roleToRoute(role)
                            navController.navigate(dest) {
                                popUpTo(Routes.Onboarding.route) { inclusive = true }
                            }
                        },
                        onLogin = {
                            navController.navigate(Routes.Login.route) {
                                popUpTo(Routes.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
            )
        }

        // ── Auth ─────────────────────────────────────────────────────────────
        composable(Routes.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoggedIn = { role ->
                    navController.navigate(roleToRoute(role)) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.Register.route)
                }
            )
        }

        composable(Routes.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegistered = { _ ->
                    // After successful registration, request permissions then go home
                    navController.navigate(Routes.Permissions.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.Permissions.route) {
            PermissionScreen(
                onPermissionsHandled = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Permissions.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Home (Requester) ─────────────────────────────────────────────────
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

        // ── Volunteer ────────────────────────────────────────────────────────
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

        // ── History ──────────────────────────────────────────────────────────
        composable(Routes.History.route) {
            HistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Profile ──────────────────────────────────────────────────────────
        composable(Routes.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Admin ────────────────────────────────────────────────────────────
        composable(Routes.AdminDashboard.route) {
            AdminDashboardScreen(
                onNavigateToProfile = {
                    navController.navigate(Routes.Profile.route)
                }
            )
        }
    }
}

/** Maps a role string to its home route. */
private fun roleToRoute(role: String): String = when (role) {
    "volunteer" -> Routes.VolunteerHome.route
    "admin"     -> Routes.AdminDashboard.route
    else        -> Routes.Home.route
}
