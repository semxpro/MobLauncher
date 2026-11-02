package net.millida.launcher.ui.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import net.millida.launcher.ui.LocalReducedMotion
import net.millida.launcher.ui.LocalWideLayout
import net.millida.launcher.ui.LocalTour
import net.millida.launcher.ui.TourOverlay
import net.millida.launcher.ui.TourRevealer
import net.millida.launcher.ui.TourScrollMargin
import net.millida.launcher.ui.TourStep
import net.millida.launcher.ui.TourTarget
import net.millida.launcher.ui.TourViewport
import net.millida.launcher.ui.bringIntoTour

/** The desktop guide's steps, in its order, pointed at the phone lobby's own elements. */
private val LOBBY_STEPS = listOf(
    TourStep(TourTarget.PLAY, "Play button", "If you have no builds, this creates one, then downloads and launches the game."),
    TourStep(TourTarget.BUILDS, "Your builds", "Choose what to play from the shelf below Play."),
    TourStep(TourTarget.CHARACTER, "Your character", "See what you are wearing. Select it to manage skins, capes, and cosmetics."),
    TourStep(TourTarget.WARDROBE, "Skins and capes", "Choose a skin, cape, or cosmetic. Visible on servers when signed in to Millida."),
    TourStep(TourTarget.SERVER, "Your server", "Create a free server for friends."),
    TourStep(TourTarget.FRIENDS, "Friends", "See who is online and what they are playing. Chat and invite with one button."),
    TourStep(TourTarget.ACCOUNT, "Account", "Switch accounts and check your Minecraft license."),
    TourStep(TourTarget.SETTINGS, "Settings", "Adjust button colors, sound, and memory. Restart the guide here."),
)

private val LOBBY_STEPS_NO_BUILDS = LOBBY_STEPS.filter { it.target != TourTarget.BUILDS }

/**
 * The landscape lobby is the desktop one, so the guide speaks the desktop's
 * words there: the builds sit behind the «Сегодня играем» plate, which is
 * always on the scene, with or without builds.
 */
private val WIDE_LOBBY_STEPS = LOBBY_STEPS.map { step ->
    when (step.target) {
        TourTarget.BUILDS -> TourStep(
            TourTarget.BUILDS,
            "What to play today",
            "The panel to the left of Play opens your library of builds, versions, modes, and servers.",
        )
        TourTarget.WARDROBE -> TourStep(TourTarget.WARDROBE, "My skin", step.text)
        TourTarget.SERVER -> TourStep(TourTarget.SERVER, "Play with friends", step.text)
        else -> step
    }
}

/** The lobby guide; without builds the phone has no shelf to show, so its step is left out. */
@Composable
internal fun LobbyTour(step: Int, hasBuilds: Boolean, onStep: (Int) -> Unit, onClose: () -> Unit) {
    TourOverlay(
        steps = when {
            LocalWideLayout.current -> WIDE_LOBBY_STEPS
            hasBuilds -> LOBBY_STEPS
            else -> LOBBY_STEPS_NO_BUILDS
        },
        index = step,
        onStep = onStep,
        onClose = onClose,
    )
}

/** The lobby's scroll area the guide may need to move: the phone feed. */
@Stable
internal class LobbyTourScroll(val listState: LazyListState) {
    val listView = TourViewport()
}

/**
 * Owns the lobby's scroll state and lets the guide scroll it: a lazy row is
 * scrolled to first, because an off-screen item is not composed and has no
 * bounds. The landscape lobby is one unscrolled scene, the library list there
 * keeps a state of its own.
 */
@Composable
internal fun rememberLobbyTourScroll(wide: Boolean, shelfCount: Int): LobbyTourScroll {
    val registry = LocalTour.current
    val portraitList = rememberLazyListState()
    val wideList = rememberLazyListState()
    val scroll = remember(wide) { LobbyTourScroll(if (wide) wideList else portraitList) }
    val currentWide by rememberUpdatedState(wide)
    val currentShelves by rememberUpdatedState(shelfCount)
    val animate by rememberUpdatedState(!LocalReducedMotion.current)
    val margin by rememberUpdatedState(with(LocalDensity.current) { TourScrollMargin.toPx() })

    DisposableEffect(registry, scroll) {
        val revealer: TourRevealer = { target, room ->
            val item = lobbyItemIndex(target, currentWide, currentShelves)
            if (item != null) scroll.listState.showItem(item, animate)
            val bounds = registry.awaitWhole(target)
            if (bounds != null && item != null) {
                scroll.listState.bringIntoTour(scroll.listView, bounds, room(bounds), margin, animate)
            }
        }
        registry.revealer = revealer
        onDispose { if (registry.revealer === revealer) registry.revealer = null }
    }
    return scroll
}

private suspend fun LazyListState.showItem(index: Int, animate: Boolean) {
    if (layoutInfo.visibleItemsInfo.any { it.index == index }) return
    if (animate) animateScrollToItem(index) else scrollToItem(index)
}

/**
 * The phone lobby list's item order from HomeScreen: top row, stage, hero, the
 * build shelves, then the tiles. The landscape scene shows every mark at once
 * and is no list: null.
 */
private fun lobbyItemIndex(target: TourTarget, wide: Boolean, shelves: Int): Int? = if (wide) {
    null
} else {
    when (target) {
        TourTarget.FRIENDS, TourTarget.ACCOUNT, TourTarget.SETTINGS -> 0
        TourTarget.CHARACTER -> 1
        TourTarget.PLAY -> 2
        TourTarget.BUILDS -> 3
        TourTarget.WARDROBE, TourTarget.SERVER -> 3 + shelves
    }
}
