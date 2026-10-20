package uz.apteka360.app.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Desktop `A.money / moneyShort / num / pct` bilan bir xil formatlar. */
object Fmt {
    fun group(n: Long): String {
        val s = abs(n).toString(); val sb = StringBuilder()
        s.forEachIndexed { i, ch -> if (i > 0 && (s.length - i) % 3 == 0) sb.append(' '); sb.append(ch) }
        return sb.toString()
    }
    fun num(n: Long?): String = if (n == null) "—" else (if (n < 0) "−" else "") + group(n)
    fun num(n: Double?): String = if (n == null) "—" else num(Math.round(n))
    fun money(n: Long?): String = if (n == null) "—" else (if (n < 0) "−" else "") + group(n) + " so‘m"
    fun money(n: Double?): String = money(n?.let { Math.round(it) })
    /** (qiymat, birlik): 1,2 mln so‘m */
    fun moneyShort(n: Long?): Pair<String, String> {
        if (n == null) return "—" to ""
        val a = abs(n); val sg = if (n < 0) "−" else ""
        return when {
            a >= 1_000_000_000 -> (sg + String.format(Locale.US, "%.1f", a / 1e9).replace('.', ',')) to "mlrd so‘m"
            a >= 1_000_000 -> (sg + String.format(Locale.US, "%.1f", a / 1e6).replace('.', ',')) to "mln so‘m"
            else -> (sg + group(a)) to "so‘m"
        }
    }
    fun moneyShortText(n: Long?): String { val (v, u) = moneyShort(n); return if (u.isEmpty()) v else "$v $u" }
    fun pct(n: Double?): String = if (n == null) "—" else (if (n > 0) "+" else "") + (if (n == Math.floor(n)) n.toLong().toString() else String.format(Locale.US, "%.1f", n).replace('.', ',')) + "%"
    fun initials(name: String?): String = (name ?: "?").trim().split(Regex("\\s+")).filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()

    private val MONTHS = listOf("yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr")
    private val DAYS = listOf("yakshanba", "dushanba", "seshanba", "chorshanba", "payshanba", "juma", "shanba")
    fun longDate(d: Date = Date()): String { val c = Calendar.getInstance().apply { time = d }; return "${c.get(Calendar.DAY_OF_MONTH)}-${MONTHS[c.get(Calendar.MONTH)]}, ${DAYS[c.get(Calendar.DAY_OF_WEEK) - 1]}" }
    fun greeting(name: String): String { val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); return (if (h < 11) "Xayrli tong" else if (h < 18) "Xayrli kun" else "Xayrli kech") + ", " + name.split(' ').firstOrNull().orEmpty() }

    private val isoIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val dayIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fun parse(v: String?): Date? {
        if (v.isNullOrBlank()) return null
        return runCatching { isoIn.parse(v.take(19).replace("Z", "")) }.getOrNull() ?: runCatching { dayIn.parse(v.take(10)) }.getOrNull()
    }
    /** kk.oo.yyyy */
    fun date(v: String?): String { val d = parse(v) ?: return v?.takeIf { it.isNotBlank() } ?: "—"; return SimpleDateFormat("dd.MM.yyyy", Locale.US).format(d) }
    fun dateTime(v: String?): String { val d = parse(v) ?: return v?.takeIf { it.isNotBlank() } ?: "—"; return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(d) }
    fun dayMonthTime(v: String?): String { val d = parse(v) ?: return v ?: "—"; return SimpleDateFormat("dd.MM HH:mm", Locale.US).format(d) }
    fun isoDay(offsetDays: Int = 0): String = dayIn.format(Date(System.currentTimeMillis() - offsetDays * 86_400_000L))
}
