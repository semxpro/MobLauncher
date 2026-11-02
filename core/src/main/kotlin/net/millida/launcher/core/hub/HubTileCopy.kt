package net.millida.launcher.core.hub

/**
 * Words on the lobby's character and multiplayer tiles.
 */
object HubTileCopy {
    enum class Kind { WARDROBE, SERVER }

    data class Copy(val title: String, val subtitle: String?, val ribbon: String?, val alert: Boolean)

    /**
     * [skinSeen] - the skin tile was opened once; [hostedCount] - the player's own servers.
     */
    fun of(kind: Kind, skinSeen: Boolean, hostedCount: Int): Copy = when (kind) {
        Kind.WARDROBE -> Copy("My skins", if (skinSeen) null else "Upload a skin", ribbon = null, alert = !skinSeen)
        Kind.SERVER -> Copy("Play with friends", null, ribbon = if (hostedCount > 0) null else "Free plan", alert = false)
    }
}
