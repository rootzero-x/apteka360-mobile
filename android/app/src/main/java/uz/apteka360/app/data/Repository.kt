package uz.apteka360.app.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class Repository(val api: ApiClient, private val tokens: TokenStore) {

    // ---------- Auth ----------
    suspend fun login(login: String, password: String): LoginChallenge =
        json.decodeFromString(api.post("/v1/auth/login", buildJsonObject { put("login", login); put("password", password) }.toString(), auth = false))

    suspend fun otp(challengeId: String, code: String): OtpResponse {
        val r: OtpResponse = json.decodeFromString(api.post("/v1/auth/otp", buildJsonObject {
            put("challengeId", challengeId); put("code", code); put("deviceId", tokens.deviceId)
        }.toString(), auth = false))
        if (r.accessToken != null && r.user != null) api.applySession(Session(r.accessToken, r.refreshToken, r.expiresIn ?: 900, r.user))
        return r
    }

    suspend fun resendOtp(challengeId: String): LoginChallenge =
        json.decodeFromString(api.post("/v1/auth/otp/resend", buildJsonObject { put("challengeId", challengeId) }.toString(), auth = false))

    suspend fun deviceDecision(confirmId: String, trust: Boolean): Session? {
        val body = api.post("/v1/auth/device", buildJsonObject { put("confirmId", confirmId); put("decision", if (trust) "trust" else "block") }.toString(), auth = false)
        if (!trust) return null
        val s: Session = json.decodeFromString(body)
        api.applySession(s)
        return s
    }

    suspend fun forgotPassword(login: String): LoginChallenge =
        json.decodeFromString(api.post("/v1/auth/password/forgot", buildJsonObject { put("login", login) }.toString(), auth = false))

    suspend fun resetPassword(challengeId: String, code: String, password: String) {
        api.post("/v1/auth/password/reset", buildJsonObject { put("challengeId", challengeId); put("code", code); put("password", password) }.toString(), auth = false)
    }

    suspend fun logout() {
        try { api.post("/v1/auth/logout", buildJsonObject { put("refreshToken", tokens.refreshToken ?: "") }.toString()) } catch (_: Exception) {}
        tokens.clear()
    }

    // ---------- O'qish ----------
    suspend fun me(): Me = api.getAs("/v1/me")
    suspend fun mobile(): MobileState = api.getAs("/v1/mobile")
    suspend fun notifications(): List<Notification> = api.getAs("/v1/notifications")
    suspend fun employees(): List<Employee> = api.getAs("/v1/employees")
    suspend fun cameras(): List<Camera> = api.getAs("/v1/cameras")
    suspend fun sessions(): List<AuthSession> = api.getAs("/v1/auth/sessions")
    suspend fun branches(): List<BranchLite> = api.getAs("/v1/branches")

    // ---------- Mobil buyruqlar (har biri yangilangan MobileState qaytaradi) ----------
    suspend fun mobileCommand(name: String, body: JsonObject = buildJsonObject {}): MobileState = api.commandAs("m.$name", body)

    suspend fun advanceRequest(amount: Long, note: String) = mobileCommand("advanceRequest", buildJsonObject { put("amount", amount); put("note", note) })
    suspend fun leaveRequest(type: String, label: String, days: Int, cover: String, note: String) =
        mobileCommand("leaveRequest", buildJsonObject { put("type", type); put("label", label); put("days", days); put("cover", cover); put("note", note) })
    suspend fun swapRequest(d: Int, mateId: String, backD: Int?, reason: String) =
        mobileCommand("swapRequest", buildJsonObject { put("d", d); put("mateId", mateId); if (backD != null) put("backD", backD) else put("backD", null as String?); put("reason", reason) })
    suspend fun swapMateAccept(requesterId: String) = mobileCommand("swapMateAccept", buildJsonObject { put("empId", requesterId) })
    suspend fun appeal(reason: String, text: String) = mobileCommand("appeal", buildJsonObject { put("reason", reason); put("text", text); put("file", false) })

    /** id — `basic` elementining to'liq id'si: "lv:<empId>" yoki "av:<empId>". */
    suspend fun requestDecide(id: String, approve: Boolean, comment: String) =
        mobileCommand("requestDecide", buildJsonObject { put("id", id); put("approve", approve); put("comment", comment) })
    suspend fun swapDecide(approve: Boolean, comment: String, empId: String?) =
        mobileCommand("swapDecide", buildJsonObject { put("approve", approve); put("comment", comment); if (empId != null) put("empId", empId) })
    suspend fun appealDecide(decision: String, amount: Long?, comment: String, empId: String?) =
        mobileCommand("appealDecide", buildJsonObject { put("decision", decision); if (amount != null) put("amount", amount); put("comment", comment); if (empId != null) put("empId", empId) })
    suspend fun aiDecide(id: String, confirmed: Boolean) =
        mobileCommand("aiDecide", buildJsonObject { put("id", id); put("status", if (confirmed) "confirmed" else "rejected") })
    suspend fun pushesRead(to: String) = mobileCommand("pushesRead", buildJsonObject { put("to", to) })

    // ---------- Boshqa buyruqlar ----------
    suspend fun markRead(id: String) { api.command("notifications.markRead", buildJsonObject { put("id", id) }) }
    suspend fun markAllRead() { api.command("notifications.markAllRead", buildJsonObject {}) }
    suspend fun streamUrl(cameraId: String): StreamUrl = api.commandAs("cameras.streamUrl", buildJsonObject { put("id", cameraId); put("quality", "sub") })
    suspend fun talkUrl(cameraId: String): TalkUrl = api.commandAs("cameras.talk", buildJsonObject { put("id", cameraId) })
    suspend fun revokeSession(sid: String) { api.command("auth.revokeSession", buildJsonObject { put("sid", sid) }) }
    suspend fun revokeOthers() { api.command("auth.revokeOthers", buildJsonObject {}) }
    suspend fun changePassword(current: String, next: String) { api.command("profile.changePassword", buildJsonObject { put("current", current); put("next", next) }) }
}
