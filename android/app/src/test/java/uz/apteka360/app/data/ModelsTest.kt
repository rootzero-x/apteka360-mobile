package uz.apteka360.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test fun `streamUrl parses audio flags from 0_6_1 response`() {
        val s: StreamUrl = json.decodeFromString("""{"whepUrl":"https://e/w","hlsUrl":"https://e/index.m3u8?jwt=x","snapshotUrl":"https://e/s.jpg",
            "expiresAt":"2026-10-19T10:00:00Z","audio":{"in":true,"out":false},"privacy":false}""")
        assertTrue(s.audio.input); assertFalse(s.audio.output); assertEquals("https://e/index.m3u8?jwt=x", s.hlsUrl)
    }

    @Test fun `streamUrl without audio field defaults to no audio (older backend)`() {
        val s: StreamUrl = json.decodeFromString("""{"hlsUrl":"https://e/index.m3u8"}""")
        assertFalse(s.audio.input); assertFalse(s.audio.output)
    }

    @Test fun `talkUrl parses`() {
        val t: TalkUrl = json.decodeFromString("""{"talkUrl":"https://e/cam-c1/talk?tk=1.u.s","expiresAt":"2026-10-19T10:05:00Z","maxSec":60}""")
        assertEquals(60, t.maxSec); assertTrue(t.talkUrl.endsWith("tk=1.u.s"))
    }
}
