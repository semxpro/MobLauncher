package net.millida.launcher.ui.home

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.millida.launcher.core.hub.Hub
import net.millida.launcher.core.hub.HubClient
import net.millida.launcher.core.hub.PopularVersion

data class HubState(
    val popular: List<PopularVersion> = emptyList(),
)

class HubModel(
    private val hub: HubClient,
    private val scope: CoroutineScope,
) {
    private val mutable = MutableStateFlow(HubState())
    val state: StateFlow<HubState> = mutable.asStateFlow()

    private var loaded = false
    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        load()
    }

    private fun load() {
        scope.launch {
            val popular = try {
                withContext(Dispatchers.IO) { hub.popularVersions() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Hub.POPULAR_FALLBACK
            }
            mutable.update { it.copy(popular = popular) }
        }
    }
}
