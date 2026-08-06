package com.example.todoaccesible.navigation

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Register : Routes("register")

    // Cliente
    object ClienteDashboard : Routes("cliente/dashboard")
    object ProjectInfo : Routes("cliente/diagnostic/new")
    object Questionnaire : Routes("cliente/diagnostic/{diagnosticId}/questionnaire") {
        fun build(diagnosticId: Long) = "cliente/diagnostic/$diagnosticId/questionnaire"
    }
    object DiagnosticDetail : Routes("cliente/diagnostic/{diagnosticId}/detail") {
        fun build(diagnosticId: Long) = "cliente/diagnostic/$diagnosticId/detail"
    }
    object DiagnosticResult : Routes("cliente/diagnostic/{diagnosticId}/result") {
        fun build(diagnosticId: Long) = "cliente/diagnostic/$diagnosticId/result"
    }
    object ResponderInfoAdicional : Routes("cliente/diagnostic/{diagnosticId}/responder") {
        fun build(diagnosticId: Long) = "cliente/diagnostic/$diagnosticId/responder"
    }
    object Notifications : Routes("cliente/notifications")

    // Admin
    object AdminDashboard : Routes("admin/dashboard")
    object AdminUsers : Routes("admin/users")
    object AdminQuestions : Routes("admin/questions")
    object AdminPending : Routes("admin/pending")
    object AdminReview : Routes("admin/review/{diagnosticId}") {
        fun build(diagnosticId: Long) = "admin/review/$diagnosticId"
    }
    object AdminCompare : Routes("admin/compare")

    companion object {
        const val ARG_DIAGNOSTIC_ID = "diagnosticId"
    }
}
