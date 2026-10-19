package uz.apteka360.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TalkProtocolTest {
    private val url = "https://edge.example.uz/media/cam-c1/talk?tk=1700000000.u1.abc"

    @Test fun `op url appends to existing query`() {
        assertEquals("$url&op=start", TalkProtocol.opUrl(url, "start"))
        assertEquals("$url&op=stop", TalkProtocol.opUrl(url, "stop"))
    }

    @Test fun `op url starts a query when there is none`() {
        assertEquals("https://e/cam-c1/talk?op=start", TalkProtocol.opUrl("https://e/cam-c1/talk", "start"))
    }

    @Test fun `chunk is 200 ms of pcm16 mono at 8 kHz`() {
        assertEquals(3200, TalkProtocol.CHUNK_BYTES)
        assertEquals(8000, TalkProtocol.SAMPLE_RATE)
    }

    @Test fun `edge error text is shown as is for busy camera`() {
        val msg = TalkProtocol.errorMessage(409, """{"error":"Bu kameraga hozir boshqa foydalanuvchi gapiryapti","sec":12}""")
        assertEquals("Bu kameraga hozir boshqa foydalanuvchi gapiryapti", msg)
    }

    @Test fun `expired token and ended session map to uzbek messages`() {
        assertEquals("Gapirish havolasi muddati o‘tgan. Qayta urinib ko‘ring.", TalkProtocol.errorMessage(401, """{"error":"Token noto‘g‘ri"}"""))
        assertEquals("Seans tugadi (stop).", TalkProtocol.errorMessage(410, """{"error":"Seans tugadi","reason":"stop"}""".replace("\"Seans tugadi\"", "\"stop\"")))
        assertEquals("Seans topilmadi. Qayta bosib gapiring.", TalkProtocol.errorMessage(404, "not json"))
    }

    @Test fun `unknown code falls back to body error or generic`() {
        assertEquals("Kamera manzilida login/parol yo‘q", TalkProtocol.errorMessage(503, """{"error":"Kamera manzilida login/parol yo‘q"}"""))
        assertEquals("Gapirishda xato (500)", TalkProtocol.errorMessage(500, ""))
    }

    @Test fun `maxSec is read from start response`() {
        assertEquals(45, TalkProtocol.maxSecFrom("""{"ok":true,"maxSec":45,"codec":"ulaw"}""", 60))
        assertEquals(60, TalkProtocol.maxSecFrom("""{"ok":true}""", 60))
    }
}
