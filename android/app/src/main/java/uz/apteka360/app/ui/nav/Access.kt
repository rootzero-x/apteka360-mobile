package uz.apteka360.app.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.ui.graphics.vector.ImageVector

/** Desktop `ui/app.js` NAV + ACCESS bilan bir xil: bo'lim id'lari, guruhlar, rollar. */
data class NavItem(val id: String, val label: String, val group: String, val icon: ImageVector)

object Access {
    val NAV: List<NavItem> = listOf(
        NavItem("dashboard", "Bosh sahifa", "Asosiy", Icons.Outlined.Home),
        NavItem("mobile", "Mening ishim", "Asosiy", Icons.Outlined.Smartphone),
        NavItem("branches", "Filiallar", "Operatsiyalar", Icons.Outlined.Store),
        NavItem("expiry", "Sroklar", "Operatsiyalar", Icons.Outlined.HourglassBottom),
        NavItem("cash", "Kassa yopilishi", "Operatsiyalar", Icons.Outlined.PointOfSale),
        NavItem("expenses", "Harajatlar", "Operatsiyalar", Icons.Outlined.AccountBalanceWallet),
        NavItem("cashflow", "Pul oqimi", "Operatsiyalar", Icons.Outlined.Waves),
        NavItem("employees", "Xodimlar", "HR", Icons.Outlined.Groups),
        NavItem("schedule", "Grafiklar", "HR", Icons.Outlined.CalendarMonth),
        NavItem("attendance", "Davomat", "HR", Icons.Outlined.Schedule),
        NavItem("live", "Jonli filial", "HR", Icons.Outlined.Sensors),
        NavItem("payroll", "Oylik (Payroll)", "HR", Icons.Outlined.Badge),
        NavItem("payrollRules", "Oylik shablon va formula", "HR", Icons.Outlined.FactCheck),
        NavItem("suppliers", "Firmalar va to‘lovlar", "Firmalar", Icons.Outlined.LocalShipping),
        NavItem("payables", "To‘lov kalendari", "Firmalar", Icons.Outlined.Payments),
        NavItem("doctors", "Vrachlar va ROI", "Vrachlar", Icons.Outlined.LocalHospital),
        NavItem("ai", "Review navbati", "AI nazorat", Icons.Outlined.Psychology),
        NavItem("videowall", "Video devor", "AI nazorat", Icons.Outlined.ViewModule),
        NavItem("cameras", "Kameralar va zonalar", "AI nazorat", Icons.Outlined.Videocam),
        NavItem("pnl", "P&L", "Analitika", Icons.Outlined.ShowChart),
        NavItem("analytics", "Analitika", "Analitika", Icons.Outlined.Analytics),
        NavItem("forecast", "Prognoz", "Analitika", Icons.Outlined.TrendingUp),
        NavItem("reports", "Hisobotlar", "Analitika", Icons.Outlined.Assessment),
        NavItem("tax", "Soliq", "Analitika", Icons.Outlined.Receipt),
        NavItem("approvals", "Tasdiqlar", "Boshqaruv", Icons.Outlined.CheckCircle),
        NavItem("notifications", "Bildirishlar", "Boshqaruv", Icons.Outlined.Notifications),
        NavItem("audit", "Audit jurnali", "Boshqaruv", Icons.Outlined.History),
        NavItem("fom", "FOM integratsiya", "Tizim", Icons.Outlined.Sync),
        NavItem("health", "Tizim holati", "Tizim", Icons.Outlined.MonitorHeart),
        NavItem("users", "Foydalanuvchilar", "Tizim", Icons.Outlined.Person),
        NavItem("roles", "Rollar va ruxsatlar", "Tizim", Icons.Outlined.Key),
        NavItem("modules", "Modullar", "Tizim", Icons.Outlined.Extension),
        NavItem("aiSettings", "AI sozlamalari", "Tizim", Icons.Outlined.AutoAwesome),
        NavItem("settings", "Sozlamalar", "Tizim", Icons.Outlined.Settings),
        NavItem("profile", "Profil", "Hisob", Icons.Outlined.Person),
    )
    val byId: Map<String, NavItem> = NAV.associateBy { it.id }

    private val ACCESS: Map<String, List<String>?> = mapOf(
        "OWNER" to null, // hammasi
        "SUPER_ADMIN" to listOf("branches", "employees", "ai", "videowall", "cameras", "notifications", "audit", "fom", "health", "users", "roles", "settings", "profile", "modules", "aiSettings", "tax", "expiry", "payables"),
        "FINANCE" to listOf("payrollRules", "dashboard", "branches", "cash", "expenses", "cashflow", "payroll", "suppliers", "doctors", "pnl", "analytics", "forecast", "approvals", "notifications", "reports", "profile", "employees", "tax", "expiry", "payables"),
        "MANAGER" to listOf("dashboard", "branches", "cash", "expenses", "employees", "schedule", "attendance", "live", "payroll", "ai", "videowall", "approvals", "notifications", "profile", "expiry", "mobile"),
        "HR" to listOf("employees", "schedule", "attendance", "approvals", "notifications", "profile", "branches"),
        "OPERATOR" to listOf("ai", "videowall", "cameras", "notifications", "profile", "mobile"),
        "EMPLOYEE" to listOf("profile", "mobile", "notifications"),
        "AUDITOR" to listOf("payrollRules", "cashflow", "forecast", "fom", "health", "users", "roles", "attendance", "schedule", "dashboard", "branches", "employees", "cash", "expenses", "payroll", "suppliers", "ai", "approvals", "notifications", "audit", "pnl", "analytics", "reports", "profile", "doctors", "modules", "aiSettings", "tax", "expiry", "payables"),
    )
    val HOME = mapOf("OWNER" to "dashboard", "SUPER_ADMIN" to "branches", "FINANCE" to "dashboard", "MANAGER" to "dashboard", "HR" to "employees", "OPERATOR" to "ai", "EMPLOYEE" to "mobile", "AUDITOR" to "audit")

    fun can(id: String, role: String): Boolean {
        if (id == "settings") return role == "OWNER" || role == "SUPER_ADMIN" || role == "AUDITOR"
        if (id == "mobile") return role == "EMPLOYEE" || role == "MANAGER" || role == "OWNER" || role == "OPERATOR"
        val key = if (id == "employee") "employees" else id
        if (!ACCESS.containsKey(role)) return false
        val a = ACCESS[role] ?: return true
        return key in a
    }
    /** Pastki panel uchun qisqa nom. */
    fun short(id: String): String = mapOf("cameras" to "Kameralar", "notifications" to "Xabarlar", "dashboard" to "Asosiy", "payroll" to "Oylik", "videowall" to "Video devor", "ai" to "AI navbat", "mobile" to "Mening ishim", "employees" to "Xodimlar", "schedule" to "Grafik", "approvals" to "Tasdiqlar", "branches" to "Filiallar", "users" to "Foydalanuvchi", "audit" to "Audit", "expenses" to "Harajatlar", "profile" to "Profil")[id] ?: (byId[id]?.label ?: id)
    fun home(role: String): String = HOME[role] ?: "profile"
    fun readOnly(role: String) = role == "AUDITOR"

    /** Pastki panel: rolga qarab 4 ta asosiy bo'lim + «Yana». */
    fun bottomTabs(role: String): List<String> {
        val pref = when (role) {
            "EMPLOYEE" -> listOf("mobile", "notifications", "profile")
            "OPERATOR" -> listOf("ai", "videowall", "cameras", "notifications")
            "SUPER_ADMIN" -> listOf("branches", "cameras", "users", "notifications")
            "HR" -> listOf("employees", "schedule", "approvals", "notifications")
            "AUDITOR" -> listOf("audit", "dashboard", "approvals", "notifications")
            "FINANCE" -> listOf("dashboard", "approvals", "expenses", "notifications")
            "MANAGER" -> listOf("dashboard", "approvals", "mobile", "notifications")
            else -> listOf("dashboard", "approvals", "cameras", "notifications")
        }
        return pref.filter { can(it, role) }.take(4)
    }

    /** «Yana» ekrani: qolgan bo'limlar guruh bo'yicha. */
    fun moreGroups(role: String, tabs: List<String>): List<Pair<String, List<NavItem>>> =
        NAV.filter { it.id !in tabs && can(it.id, role) }.groupBy { it.group }.map { it.key to it.value }
}
