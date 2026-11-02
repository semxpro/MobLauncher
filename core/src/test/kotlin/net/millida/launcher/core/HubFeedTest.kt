package net.millida.launcher.core

import net.millida.launcher.core.hub.HealthTone
import net.millida.launcher.core.hub.Hub
import net.millida.launcher.core.hub.PackHealth
import net.millida.launcher.core.hub.PopularVersion
import net.millida.launcher.core.hub.PresetLoader
import net.millida.launcher.core.media.LobbyMusic
import net.millida.launcher.core.mojang.MojangJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HubFeedTest {

    @Test
    fun `строка здоровья сборки говорит только то, что знает`() {
        val now = Instant.parse("2026-10-02T12:00:00Z")
        fun health(status: String, pct: Int?, lastOk: String?) = PackHealth("pack", status, 10, pct, lastOk)
        val cases = listOf(
            health("unknown", 99, "2026-10-02T11:59:00Z") to null,
            health("ok", null, null) to null,
            health("ok", 97, "2026-10-02T10:00:00Z") to "OK|Operational|Last active 2 h ago|97|false",
            health("ok", null, "2026-10-02T11:58:00Z") to "OK|Operational|Last active just now|null|false",
            health("warn", 61, null) to "WARN|Issues reported||61|true",
            health("warn", null, "2026-09-29T12:00:00Z") to "WARN|Issues reported|Last active 3 d ago|null|true",
        )
        cases.forEach { (input, expected) ->
            val line = Hub.healthLine(input, now)
            assertEquals("здоровье $input", expected, line?.let { "${it.tone}|${it.head}|${it.whenText}|${it.pct}|${it.report}" })
        }
        assertEquals("сломанная дата не превращается в «давно»", "", Hub.ago("вчера", now))
        assertEquals(HealthTone.WARN, Hub.healthLine(health("warn", 50, null), now)?.tone)
    }

    @Test
    fun `здоровье берётся из ответа по своему слагу`() {
        val body = MojangJson.parseToJsonElement(
            """{"packs": [{"slug": "other", "status": "warn"}, {"slug": "arcania", "status": "ok", "launches": 40, "successPct": 97, "lastOkAt": "2026-10-02T10:00:00Z"}]}""",
        )
        val health = Hub.health(body, " Arcania ")
        assertEquals("ok", health?.status)
        assertEquals(97, health?.successPct)
        assertNull("сборки нет в ответе — строки нет", Hub.health(body, "missing"))
    }

    @Test
    fun `популярные версии — проверенные строки или зашитый топ`() {
        val body = MojangJson.parseToJsonElement(
            """{"versions": [
                {"mc": "1.21.1", "loader": "neoforge"},
                {"mc": "latest", "loader": "fabric"},
                {"mc": "1.20.1", "loader": "optifine"},
                {"mc": 1.19, "loader": "fabric"},
                {"mc": "1.20.4", "loader": "vanilla"},
                {"mc": "1.19.2", "loader": "forge"},
                {"mc": "1.18.2", "loader": "quilt"}
            ]}""",
        )
        assertEquals(
            "чужой загрузчик, не-версия и число вместо строки отброшены, не больше трёх",
            listOf(PopularVersion("1.21.1", PresetLoader.NEOFORGE), PopularVersion("1.20.4", PresetLoader.VANILLA), PopularVersion("1.19.2", PresetLoader.FORGE)),
            Hub.popularVersions(body),
        )
        assertEquals("пустой ответ — топ по телеметрии", Hub.POPULAR_FALLBACK, Hub.popularVersions(MojangJson.parseToJsonElement("{}")))
    }

    @Test
    fun `музыка лобби — треки игры из индекса, как на компьютере`() {
        val hash = "0123456789abcdef0123456789abcdef01234567"
        val objects = buildMap {
            put("minecraft/sounds/music/game/sweden.ogg", hash to 3_000_000L)
            put("minecraft/sounds/music/menu/mutation.ogg", hash.uppercase() to 2_000_000L)
            put("minecraft/sounds/music/game/broken.ogg", "nothex" to 1L)
            put("minecraft/sounds/ambient/cave/cave1.ogg", hash to 1L)
            put("minecraft/sounds/music/game/notes.txt", hash to 1L)
            for (i in 0 until 30) put("minecraft/sounds/music/game/nether/track_$i.ogg", hash to 1L)
        }
        val tracks = LobbyMusic.pickTracks(objects)
        assertEquals("не больше лимита компьютера", LobbyMusic.LIMIT, tracks.size)
        assertTrue("только музыка в ogg с настоящим хешем", tracks.all { it.key.startsWith(LobbyMusic.PREFIX) && it.key.endsWith(".ogg") && it.key != "minecraft/sounds/music/game/broken.ogg" })
        assertTrue("хеш приводится к нижнему регистру — так лежат объекты", tracks.all { it.hash == hash })
        assertEquals("Sweden", LobbyMusic.title("minecraft/sounds/music/game/sweden.ogg"))
        assertEquals("Wet Hands", LobbyMusic.title("minecraft/sounds/music/game/wet_hands.ogg"))
    }

}
