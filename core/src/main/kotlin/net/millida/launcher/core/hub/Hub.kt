package net.millida.launcher.core.hub

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Duration
import java.time.Instant

enum class HealthTone { OK, WARN }

data class PackHealth(
    val slug: String,
    val status: String,
    val launches: Int,
    val successPct: Int?,
    val lastOkAt: String?,
)

data class HealthLine(
    val tone: HealthTone,
    val head: String,
    val whenText: String,
    val pct: Int?,
    val report: Boolean,
)

enum class PresetLoader(val id: String) {
    VANILLA("vanilla"),
    FABRIC("fabric"),
    FORGE("forge"),
    NEOFORGE("neoforge"),
    QUILT("quilt");

    companion object {
        fun of(raw: String?): PresetLoader? = entries.firstOrNull { it.id == raw }
    }
}

data class PopularVersion(val mc: String, val loader: PresetLoader)

object Hub {
    private val VERSION = Regex("^\\d+(\\.\\d+)+$")
    private const val POPULAR_LIMIT = 3

    /** Fallback when the popular-version endpoint is unavailable. */
    val POPULAR_FALLBACK = listOf(
        PopularVersion("1.20.1", PresetLoader.FORGE),
        PopularVersion("26.2", PresetLoader.FABRIC),
        PopularVersion("1.21.11", PresetLoader.FABRIC),
    )

    fun health(body: JsonElement?, slug: String): PackHealth? {
        val key = slug.trim().lowercase()
        val packs = ((body as? JsonObject)?.get("packs") as? JsonArray).orEmpty()
        return packs.mapNotNull { it as? JsonObject }
            .firstOrNull { it.text("slug") == key }
            ?.let { pack ->
                PackHealth(
                    slug = key,
                    status = pack.text("status"),
                    launches = pack.int("launches") ?: 0,
                    successPct = pack.int("successPct"),
                    lastOkAt = pack.text("lastOkAt").ifEmpty { null },
                )
            }
    }

    /** Null when there is nothing to say: no line on the page at all. */
    fun healthLine(health: PackHealth?, now: Instant = Instant.now()): HealthLine? {
        if (health == null || health.status == "unknown") return null
        val since = health.lastOkAt?.let { ago(it, now) }.orEmpty()
        val whenText = if (since.isNotEmpty()) "Last active $since" else ""
        if (health.status == "warn") return HealthLine(HealthTone.WARN, "Issues reported", whenText, health.successPct, report = true)
        if (whenText.isEmpty() && health.successPct == null) return null
        return HealthLine(HealthTone.OK, "Operational", whenText, health.successPct, report = false)
    }

    fun ago(iso: String, now: Instant): String {
        val at = runCatching { Instant.parse(iso.trim()) }.getOrNull() ?: return ""
        val minutes = Duration.between(at, now).toMillis() / 60_000.0
        return when {
            minutes < 5 -> "just now"
            minutes < 60 -> "${Math.round(minutes)} min ago"
            minutes < 1440 -> "${Math.round(minutes / 60)} h ago"
            else -> "${Math.round(minutes / 1440)} d ago"
        }
    }

    fun popularVersions(body: JsonElement?): List<PopularVersion> {
        val list = ((body as? JsonObject)?.get("versions") as? JsonArray).orEmpty()
        val parsed = list.mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            val mc = (row["mc"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.trim() ?: return@mapNotNull null
            if (!VERSION.matches(mc)) return@mapNotNull null
            val loader = PresetLoader.of((row["loader"] as? JsonPrimitive)?.contentOrNull) ?: return@mapNotNull null
            PopularVersion(mc, loader)
        }.take(POPULAR_LIMIT)
        return parsed.ifEmpty { POPULAR_FALLBACK }
    }

    private fun JsonObject.text(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

    private fun JsonObject.int(key: String): Int? {
        val value = this[key] as? JsonPrimitive ?: return null
        if (value.isString) return null
        return value.longOrNull?.toInt() ?: value.doubleOrNull?.let { Math.round(it).toInt() }
    }

    private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()
}
