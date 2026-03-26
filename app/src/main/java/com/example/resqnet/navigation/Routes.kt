package com.example.resqnet.navigation

sealed class Routes(val route: String) {
    // Onboarding
    data object Onboarding : Routes("onboarding")

    // Auth
    data object Login : Routes("login")
    data object Otp : Routes("otp/{phone}") {
        fun createRoute(phone: String) = "otp/$phone"
    }

    // Main (Requester)
    data object Home : Routes("home")
    data object SosTrigger : Routes("sos_trigger")
    data object SosActive : Routes("sos_active/{sosId}") {
        fun createRoute(sosId: String) = "sos_active/$sosId"
    }

    // Volunteer
    data object VolunteerHome : Routes("volunteer_home")
    data object IncomingSos : Routes("incoming_sos/{sosId}") {
        fun createRoute(sosId: String) = "incoming_sos/$sosId"
    }
    data object ActiveResponse : Routes("active_response/{sosId}") {
        fun createRoute(sosId: String) = "active_response/$sosId"
    }

    // History
    data object History : Routes("history")

    // Profile
    data object Profile : Routes("profile")

    // Admin
    data object AdminDashboard : Routes("admin_dashboard")
}
