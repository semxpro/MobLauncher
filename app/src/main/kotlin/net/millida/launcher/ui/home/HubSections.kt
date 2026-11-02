package net.millida.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.millida.launcher.core.hub.PopularVersion
import net.millida.launcher.core.hub.PresetLoader
import net.millida.launcher.ui.MButtonRow
import net.millida.launcher.ui.MChip
import net.millida.launcher.ui.MSectionHeader
import net.millida.launcher.ui.MillidaType
import net.millida.launcher.ui.millida

internal fun LazyListScope.hubSections(
    hub: HubState,
    createVersion: (String) -> Unit,
    hasBuilds: Boolean,
    party: (@Composable () -> Unit)? = null,
) {
    if (party != null) {
        item(key = "hub:party", contentType = "hub-party") { party() }
    }
    if (!hasBuilds && hub.popular.isNotEmpty()) {
        item(key = "hub:popular", contentType = "hub-popular") {
            PopularVersions(hub.popular, createVersion)
        }
    }
}

@Composable
private fun PopularVersions(versions: List<PopularVersion>, onCreate: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        MSectionHeader("Popular versions")
        MButtonRow {
            versions.forEach { version ->
                MChip(
                    text = version.mc + if (version.loader == PresetLoader.FABRIC) "" else " " + presetLoaderName(version.loader),
                    selected = false,
                    onClick = { onCreate(version.mc) },
                )
            }
        }
    }
}

private fun presetLoaderName(loader: PresetLoader): String = when (loader) {
    PresetLoader.VANILLA -> "Vanilla"
    PresetLoader.FABRIC -> "Fabric"
    PresetLoader.FORGE -> "Forge"
    PresetLoader.NEOFORGE -> "NeoForge"
    PresetLoader.QUILT -> "Quilt"
}
