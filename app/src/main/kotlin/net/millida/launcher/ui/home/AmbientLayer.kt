package net.millida.launcher.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.millida.launcher.LauncherViewModel
import net.millida.launcher.Screen
import net.millida.launcher.UiState

/** The lobby guide above the home screen and below the first-run pages. */
@Composable
fun AmbientLayer(state: UiState, model: LauncherViewModel) {
    val ambient by model.ambient.state.collectAsStateWithLifecycle()
    if (state.onboarding) return
    val step = ambient.tourStep
    if (step != null && state.screen == Screen.HOME) {
        LobbyTour(
            step = step,
            hasBuilds = state.profiles.isNotEmpty(),
            onStep = model.ambient::tourStep,
            onClose = model.ambient::closeTour,
        )
    }
}
