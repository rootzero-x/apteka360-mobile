package uz.apteka360.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ApiErrorBody(
    val error: String = "Xatolik",
    val code: String = "unknown",
    val details: JsonElement? = null,
    val requestId: String? = null,
)

class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
    val details: JsonElement? = null,
    val requestId: String? = null,
) : Exception(message)

class NetworkException(cause: Throwable) : Exception("Internet bilan aloqa yo‘q", cause)

// ---------- Auth ----------

@Serializable
data class LoginChallenge(
    val challengeId: String,
    val otpTtlSec: Int = 300,
    val resendInSec: Int = 60,
    val channels: List<String> = emptyList(),
)

@Serializable
data class NewDevice(val confirmId: String, val device: String = "", val location: String = "", val ip: String = "")

@Serializable
data class User(
    val role: String,
    val name: String,
    val ini: String = "",
    val title: String = "",
    val scope: String = "",
)

@Serializable
data class Session(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Int = 900,
    val user: User,
)

/** /v1/auth/otp javobi: yo sessiya, yo yangi qurilma tasdig'i. */
@Serializable
data class OtpResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Int? = null,
    val user: User? = null,
    val newDevice: NewDevice? = null,
)

@Serializable
data class Me(
    val id: String = "",
    val role: String,
    val name: String,
    val ini: String = "",
    val title: String = "",
    val scope: String = "",
    val phone: String? = null,
    val email: String? = null,
    val branchId: String? = null,
    val employeeId: String? = null,
    val branchIds: List<String> = emptyList(),
    val position: String? = null,
) {
    val isManager get() = role == "MANAGER" || role == "OWNER" || role == "SUPER_ADMIN"
    val isEmployee get() = role == "EMPLOYEE"
    val canDecideAi get() = isManager || role == "OPERATOR"
    val readOnly get() = role == "AUDITOR"
}

@Serializable
data class AuthSession(
    val sid: String,
    val device: String = "",
    val ip: String = "",
    val createdAt: String = "",
    val lastSeen: String = "",
    val current: Boolean = false,
)

// ---------- Mobile state ----------

@Serializable
data class Penalty(
    val id: String = "",
    val empId: String = "",
    val ruleCode: String = "",
    val amount: Long = 0,
    val aiEventId: String = "",
    val status: String = "none",
    val reason: String = "",
    val text: String = "",
    val file: Boolean = false,
    val reduced: Long = 0,
    val note: String = "",
    val appealDeadline: String = "",
    val requesterId: String? = null,
    val requesterName: String? = null,
)

@Serializable
data class Shift(val d: Int, val time: String = "", val kind: String = "day", val status: String = "normal")

@Serializable
data class SwapReq(
    val d: Int,
    val mateId: String = "",
    val mateName: String = "",
    val backTitle: String = "",
    val backD: Int? = null,
    val reason: String = "",
    val status: String = "mate",
    val note: String = "",
    val requesterId: String? = null,
    val requesterName: String? = null,
)

@Serializable
data class LeaveReq(val type: String = "", val label: String = "", val days: Int = 0, val cover: String = "", val note: String = "", val status: String = "pending")

@Serializable
data class AdvanceReq(val amount: Long = 0, val note: String = "", val status: String = "pending")

@Serializable
data class BasicReq(
    val id: String,
    val type: String = "",
    val title: String = "",
    val sub: String = "",
    val rows: List<List<String>> = emptyList(),
    val status: String = "pending",
)

@Serializable
data class AiQueueItem(
    val id: String,
    val emp: String = "",
    val label: String = "",
    val code: String = "",
    val conf: Int = 0,
    val cam: String = "",
    val time: String = "",
    val dur: String = "",
    val status: String = "pending",
)

@Serializable
data class Push(val id: String, val to: String = "", val text: String = "", val channel: String = "", val time: String = "", val read: Boolean = false)

@Serializable
data class MateRequest(
    val requesterId: String,
    val requesterName: String? = null,
    val d: Int = 0,
    val backD: Int? = null,
    val backTitle: String = "",
    val reason: String = "",
)

@Serializable
data class MobileState(
    val me: String = "",
    val advanceLimit: Long = 0,
    val leaveBalance: Int = 0,
    val lvUsed: Int = 0,
    val penalty: Penalty = Penalty(),
    val shifts: List<Shift> = emptyList(),
    val swapReq: SwapReq? = null,
    val lvReq: LeaveReq? = null,
    val avReq: AdvanceReq? = null,
    val basic: List<BasicReq> = emptyList(),
    val aiQueue: List<AiQueueItem> = emptyList(),
    val pushes: List<Push> = emptyList(),
    val mateRequests: List<MateRequest> = emptyList(),
)

// ---------- Boshqa o'qishlar ----------

@Serializable
data class Notification(
    val id: String,
    val cat: String = "",
    val pr: String = "past",
    val title: String = "",
    val branchId: String? = null,
    val time: String = "",
    val source: String = "",
    val action: String = "",
    val read: Boolean = false,
    val readByMe: Boolean? = null,
    val at: String? = null,
) {
    val unread get() = !(readByMe ?: read)
}

@Serializable
data class Employee(
    val id: String,
    val name: String = "",
    val ini: String = "",
    val position: String = "",
    val branchId: String = "",
    val status: String = "active",
    val schedule: String = "",
    val phone: String = "",
)

@Serializable
data class Camera(
    val id: String,
    val name: String = "",
    val branchId: String = "",
    val zones: String = "",
    val ai: String = "",
    val fps: Double = 0.0,
    val last: String = "",
    val health: String = "",
    val kind: String? = null,
    val host: String? = null,
    val privacy: Boolean = false,
    val recording: Boolean = true,
    val status: String? = null,
    val bitrateKbps: Double? = null,
)

/** 0.6.1: Edge heartbeat'dan — `in`: oqimda kamera mikrofoni (HLS'da AAC trek), `out`: karnay bor (cameras.talk). */
@Serializable
data class CameraAudio(@SerialName("in") val input: Boolean = false, @SerialName("out") val output: Boolean = false)

@Serializable
data class StreamUrl(
    val whepUrl: String? = null,
    val hlsUrl: String? = null,
    val snapshotUrl: String? = null,
    val expiresAt: String? = null,
    val audio: CameraAudio = CameraAudio(),
    val privacy: Boolean = false,
)

/** cameras.talk javobi: `{mediaUrl}/cam-{id}/talk?tk=…`, 5 daqiqa amal qiladi. */
@Serializable
data class TalkUrl(val talkUrl: String, val expiresAt: String = "", val maxSec: Int = 60)


typealias Json = JsonObject
