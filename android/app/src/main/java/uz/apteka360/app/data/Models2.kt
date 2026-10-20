package uz.apteka360.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

// ---------- JSON yordamchilari (analitik/katta obyektlar uchun) ----------
val JsonElement?.obj: JsonObject? get() = (this as? JsonObject)
val JsonElement?.arr: List<JsonElement> get() = runCatching { this?.jsonArray?.toList() }.getOrNull() ?: emptyList()
fun JsonElement?.str(key: String): String? = this.obj?.get(key)?.let { (it as? JsonPrimitive)?.contentOrNull }
fun JsonElement?.long(key: String): Long? = this.obj?.get(key)?.let { (it as? JsonPrimitive)?.longOrNull ?: (it as? JsonPrimitive)?.doubleOrNull?.toLong() }
fun JsonElement?.dbl(key: String): Double? = this.obj?.get(key)?.let { (it as? JsonPrimitive)?.doubleOrNull }
fun JsonElement?.bool(key: String): Boolean? = this.obj?.get(key)?.let { (it as? JsonPrimitive)?.booleanOrNull }
fun JsonElement?.list(key: String): List<JsonElement> = this.obj?.get(key).arr
fun JsonElement?.child(key: String): JsonElement? = this.obj?.get(key)
fun JsonElement?.txt(key: String): String = this.child(key).text
val JsonElement?.text: String get() = when (this) { null -> "—"; is JsonPrimitive -> this.contentOrNull ?: "—"; else -> this.toString() }

// ---------- Dashboard ----------
@Serializable data class Kpi(val key: String, val value: Long = 0, val prev: Long? = null, val deltaPct: Double? = null, val spark: List<Long> = emptyList(), val source: String = "")
@Serializable data class DashAlert(val level: String = "info", val title: String = "", val text: String = "", val link: String? = null)
@Serializable data class RevenuePoint(val date: String = "", val actual: Long? = null, val plan: Long? = null)
@Serializable data class RunwayLowest(val date: String = "", val value: Long = 0)
@Serializable data class Runway(val d30: Long = 0, val d60: Long = 0, val d90: Long = 0, val lowest: RunwayLowest? = null)
@Serializable data class Dashboard(val period: String = "", val connected: Boolean = false, val kpis: List<Kpi> = emptyList(), val revenueSeries: List<RevenuePoint> = emptyList(), val alerts: List<DashAlert> = emptyList(), val runway: Runway? = null)

// ---------- Filial ----------
@Serializable data class FomInfo(val status: String = "", val lag: String? = null, val last: String? = null)
@Serializable data class Branch(
    val id: String, val name: String = "", val short: String = "", val address: String = "", val hours: String = "", val manager: String = "",
    val health: Int = 0, val revenueToday: Long = 0, val revenueYesterday: Long = 0, val grossToday: Long = 0, val margin: Double = 0.0,
    val closing: String = "", val expensesToday: Long = 0, val expenseLimit: Long = 0, val employees: Int = 0, val onShift: Int = 0, val desks: Int = 0,
    val cameras: Int = 0, val camerasOnline: Int = 0, val edge: String = "", val fom: FomInfo? = null, val traffic: Long = 0, val conversion: Double = 0.0,
    val queue: Int = 0, val revenue30: List<Long> = emptyList(), val queueNow: Int? = null, val aiToday: Int? = null,
) { val label get() = short.ifBlank { name } }

// ---------- Tasdiqlar ----------
@Serializable data class Approval(
    val id: String, val type: String = "", val title: String = "", val amount: Long? = null, val requester: String = "", val branchId: String? = null,
    val risk: String = "", val time: String = "", val level: String = "", val evidence: String = "", val recommended: String = "",
    val history: List<String> = emptyList(), val status: String = "pending", val createdAt: String? = null,
)

// ---------- Audit ----------
@Serializable data class AuditEntry(
    val id: String, val time: String = "", val user: String = "", val action: String = "", val `object`: String = "", val branchId: String? = null,
    val before: String = "", val after: String = "", val reason: String = "", val device: String = "", val sensitive: Boolean = false, val at: String? = null,
)

// ---------- Kassa ----------
@Serializable data class Settings(val taxRate: Double = 0.0, val cashThreshold: Long = 50000, val approvalX: Long = 0, val approvalY: Long = 0, val limitCoef: Double = 0.0, val limitBase: String = "", val limitMode: String = "")
@Serializable data class CashChannels(val naqd: Long = 0, val karta: Long = 0, val qr: Long = 0)
@Serializable data class DeskClosing(val queueId: String = "", val kassa: String = "", val naqd: Long = 0, val karta: Long = 0, val qr: Long = 0, val reason: String? = null, val by: String = "", val status: String = "")
@Serializable data class DeskServerEdit(val naqd: Long = 0, val by: String = "", val note: String = "", val time: String = "", val source: String = "", val version: Int = 0)
@Serializable data class DeskState(val branchId: String = "", val version: Int? = null, val closings: List<DeskClosing> = emptyList(), val serverEdit: DeskServerEdit? = null)

// ---------- Xarajat ----------
@Serializable data class Expense(
    val id: String, val date: String = "", val branchId: String = "", val cat: String = "", val title: String = "", val who: String = "", val amount: Long = 0,
    val source: String = "", val status: String = "", val receipt: Boolean = false, val anomaly: String = "", val note: String = "", val recurring: Boolean = false, val createdAt: String? = null,
)

// ---------- Firmalar / vrachlar ----------
@Serializable data class LedgerRow(val date: String = "", val doc: String = "", val type: String = "", val debit: Long = 0, val credit: Long = 0)
@Serializable data class Supplier(
    val id: String, val name: String = "", val inn: String = "", val manager: String = "", val phone: String = "", val debt: Long = 0, val overdue: Long = 0, val dueSoon: Long = 0,
    val creditLimit: Long = 0, val purchases30: Long = 0, val terms: String = "", val earlyRate: Double = 0.0, val earlyBase: Long = 0, val volumeRate: Double = 0.0, val volumeFrom: Long = 0,
    val penaltyDaily: Double = 0.0, val overdueDays: Int = 0, val aging: List<Long> = emptyList(), val status: String = "", val ledger: List<LedgerRow> = emptyList(),
)
@Serializable data class Doctor(
    val id: String, val name: String = "", val clinic: String = "", val spec: String = "", val region: String = "", val owner: String = "", val branches: String = "", val model: String = "",
    val meds: String = "", val plan: Long = 0, val fact: Long = 0, val cost: Long = 0, val profit: Long = 0, val roi: Long = 0, val months: List<Long> = emptyList(), val method: String = "",
    val status: String = "", val startDate: String = "", val closedAt: String? = null, val closeReason: String? = null,
)
@Serializable data class DoctorSummary(val name: String = "", val clinic: String = "", val roi: Long = 0, val cost: Long = 0, val profit: Long = 0, val trend: String = "", val negMonths: Int = 0)

// ---------- Tizim ----------
@Serializable data class Account(
    val id: String, val name: String = "", val role: String = "", val title: String = "", val phone: String = "", val email: String = "", val branchId: String? = null,
    val employeeId: String? = null, val status: String = "", val demo: Boolean = false, val lastLoginAt: String? = null, val telegram: Boolean = false, val createdAt: String = "",
)
@Serializable data class EdgeDevice(val edgeId: String = "", val branchId: String = "", val version: String = "", val lastSeen: String = "", val online: Boolean = false, val tempC: Double? = null, val mediaUrl: String? = null, val gpu: JsonElement? = null, val disk: JsonElement? = null, val nvr: JsonElement? = null)
@Serializable data class RolePermissions(val code: String, val label: String = "", val actions: Map<String, Boolean> = emptyMap(), val sensitive: Map<String, Boolean> = emptyMap(), val locked: Boolean = false, val updatedAt: String? = null, val updatedBy: String? = null)
@Serializable data class ModuleFlags(val ops: Boolean = true, val expiry: Boolean = true, val hr: Boolean = true, val suppliers: Boolean = true, val doctors: Boolean = true, val vision: Boolean = true, val analytics: Boolean = true, val tax: Boolean = true, val admin: Boolean = true, val cameras: Boolean = true, val aiVerify: Boolean = true)
@Serializable data class ReportJob(val id: String, val kind: String = "", val format: String = "", val from: String = "", val to: String = "", val branchId: String? = null, val status: String = "", val createdAt: String = "", val createdBy: String = "", val fileName: String = "", val size: Long = 0, val error: String? = null, val url: String? = null, val urlExpiresAt: String? = null)
@Serializable data class LoginAttempt(val at: String = "", val time: String = "", val device: String = "", val ip: String = "", val result: String = "", val status: String = "")
@Serializable data class SwapRow(val id: String, val from: String = "", val to: String = "", val day: String = "", val shift: String = "", val status: String = "", val note: String = "")
@Serializable data class FomAgent(val id: String, val name: String = "", val createdAt: String = "", val status: String = "", val lastSyncAt: String? = null, val lastError: String? = null, val agentVersion: String? = null, val today: String? = null, val otdels: List<JsonElement> = emptyList(), val db: JsonElement? = null)
@Serializable data class FomMapRow(val fom: String = "", val name: String = "", val guess: String = "", val conf: Int = 0, val done: Boolean = false)

// ---------- AI hodisa ----------
@Serializable data class AiEvent(
    val id: String, val code: String = "", val label: String = "", val empId: String? = null, val branchId: String = "", val camera: String = "", val zone: String = "",
    val date: String = "", val time: String = "", val dur: String = "", val conf: Int = 0, val priority: String = "", val status: String = "", val idleImpact: Int = 0,
    val note: String = "", val cameraId: String? = null, val keyframes: List<String> = emptyList(), val aiVerdict: String? = null, val aiVerdictNote: String? = null,
)

// ---------- HR ----------
@Serializable data class EmployeeFull(
    val id: String, val name: String = "", val ini: String = "", val position: String = "", val branchId: String = "", val status: String = "", val schedule: String = "",
    val oklad: Long? = null, val coef: Double? = null, val kpi: Long? = null, val bonus: Long? = null, val premium: Long? = null, val penalties: Long? = null, val advance: Long? = null,
    val debt: Long? = null, val gross: Long? = null, val tax: Long? = null, val net: Long? = null, val contract: String = "", val phone: String = "", val hired: String = "",
    val attendance: Double? = null, val late: Int = 0, val absent: Int = 0, val idleUsed: Int = 0, val idleLimit: Int = 0, val fomLogin: String = "", val docs: String = "",
    val risk: String? = null, val kpiPlan: Double? = null, val kpiFact: Double? = null,
) { val dismissed get() = status == "dismissed" }
@Serializable data class PayrollRun(val period: String = "", val stage: Int = 0, val reviewed: Map<String, Boolean> = emptyMap())
@Serializable data class FormulaToken(val kind: String, val value: String)
@Serializable data class RuleVersion(val version: Int = 0, val formula: List<FormulaToken> = emptyList(), val note: String? = null, val active: Boolean = false, val createdBy: String? = null, val createdAt: String? = null)
@Serializable data class KpiTier(val minPct: Double = 0.0, val maxPct: Double? = null, val ratePct: Double = 0.0)
@Serializable data class PenaltyRule(val code: String = "", val name: String = "", val basis: String? = null, val amount: Long? = null, val source: String? = null)
@Serializable data class PayrollTemplate(val id: String, val name: String = "", val active: Boolean = false, val versions: List<RuleVersion> = emptyList(), val kpiTiers: List<KpiTier> = emptyList(), val penaltyRules: List<PenaltyRule> = emptyList())
@Serializable data class PayrollRulesDoc(val templates: List<PayrollTemplate> = emptyList())
@Serializable data class PayrollTestResult(val ok: Boolean = false, val gross: Long = 0, val tax: Long = 0, val net: Long = 0, val error: String? = null)
