package com.russoviller.cryogenic.live

import android.app.Application
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LiveConfigTest {
    @Test
    fun urlAndReadiness() {
        val c = LiveConfig(serverUrl = " rtmp://push.example.com/stage/ ", streamKey = " /abc123 ")
        assertTrue(c.isReady)
        assertEquals("rtmp://push.example.com/stage/abc123", c.fullUrl())
        assertFalse(LiveConfig(serverUrl = "https://x", streamKey = "k").isReady)
        assertFalse(LiveConfig(serverUrl = "rtmp://x", streamKey = " ").isReady)
        assertTrue(LiveConfig(serverUrl = "RTMPS://x", streamKey = "k").isReady)
    }

    @Test
    fun videoSizeAndBitrate() {
        assertEquals(1280 to 720, LiveConfig(resolution = 720).videoSize())
        assertEquals(720 to 1280, LiveConfig(resolution = 720, orientation = LiveOrientation.PORTRAIT).videoSize())
        assertEquals(960 to 540, LiveConfig(resolution = 540).videoSize())
        assertEquals(854 to 480, LiveConfig(resolution = 480).videoSize())
        assertEquals(5250, LiveConfig(resolution = 720, fps = 60).effectiveBitrateKbps())
        assertEquals(3500, LiveConfig(resolution = 720, fps = 30).effectiveBitrateKbps())
        assertEquals(3000, LiveConfig(bitrateKbps = 3000).effectiveBitrateKbps())
    }

    @Test
    fun jsonRoundTripAndClamping() {
        val c = LiveConfig(
            serverUrl = "rtmp://a/b", streamKey = "k", resolution = 540, fps = 30, bitrateKbps = 4500,
            orientation = LiveOrientation.PORTRAIT, micEnabled = false, micVolume = 40, internalVolume = 70,
            privacyEnabled = true, privacyImagePath = "/x.png", tiktokUser = "russo", chatInGame = false,
            chatSize = 120, chatOpacity = 50, chatWidth = 40, chatCorner = LiveChatCorner.BOTTOM_RIGHT, floatingBubble = false
        )
        assertEquals(c, LiveConfig.fromJson(JSONObject(c.toJson().toString())))
        val bad = LiveConfig.fromJson(JSONObject("""{"resolution":1080,"fps":120,"micVolume":500,"chatSize":5,"orientation":"X"}"""))
        assertEquals(720, bad.resolution)
        assertEquals(60, bad.fps)
        assertEquals(100, bad.micVolume)
        assertEquals(70, bad.chatSize)
        assertEquals(LiveOrientation.LANDSCAPE, bad.orientation)
    }

    @Test
    fun normalizeUser() {
        assertEquals("russoviller.mc", LiveConfig.normalizeUser(" @russoviller.mc "))
        assertEquals("russoviller.mc", LiveConfig.normalizeUser("https://www.tiktok.com/@russoviller.mc/live?lang=pt"))
        assertEquals("", LiveConfig.normalizeUser("  "))
    }

    @Test
    fun blocksOnlyLiveMedia() {
        assertTrue(TikTokChatReader.isMediaStream("https://pull-f5-sg01.tiktokcdn.com/stage/stream-123.flv?expire=1"))
        assertTrue(TikTokChatReader.isMediaStream("https://pull-hls-f16.tiktokcdn.com/game/stream-1/index.m3u8"))
        assertFalse(TikTokChatReader.isMediaStream("https://www.tiktok.com/@user/live"))
        assertFalse(TikTokChatReader.isMediaStream("https://webcast.tiktok.com/webcast/im/fetch/?room_id=1"))
        assertFalse(TikTokChatReader.isMediaStream("https://sf16-website-login.neutral.ttwstatic.com/obj/main.js"))
    }

    @Test
    fun roomInfoViewersAndStatus() {
        val live = TikTokChatReader.parseRoomInfo(
            """{"data":{"liveRoom":{"status":2,"liveRoomStats":{"enterCount":900,"userCount":57}},
               "user":{"status":2,"avatarThumb":"https://p16.tiktokcdn.com/a.webp","roomId":"7"}},"statusCode":0}"""
        )
        assertEquals(57L, live.viewers)
        assertTrue(live.live)
        assertEquals("https://p16.tiktokcdn.com/a.webp", live.avatar)
        val ended = TikTokChatReader.parseRoomInfo("""{"data":{"liveRoom":{"status":4},"user":{"status":4}},"statusCode":0}""")
        assertFalse(ended.live)
        val err = TikTokChatReader.parseRoomInfo("""{"statusCode":19881007,"data":{}}""")
        assertEquals(19881007, err.statusCode)
        assertTrue(TikTokChatReader.isHeavyResource("https://p16-sign.tiktokcdn.com/x/y.webp?x-expires=1"))
        assertTrue(TikTokChatReader.isHeavyResource("https://sf16.tiktokcdn.com/obj/font.woff2"))
        assertFalse(TikTokChatReader.isHeavyResource("https://www.tiktok.com/webcast/im/fetch/?room_id=1"))
        assertFalse(TikTokChatReader.isHeavyResource("https://sf16-website-login.neutral.ttwstatic.com/main/app.js"))
    }
}
