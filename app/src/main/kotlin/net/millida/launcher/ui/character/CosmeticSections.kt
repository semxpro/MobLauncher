package net.millida.launcher.ui.character

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.millida.launcher.core.cosmetics.CosmeticItem
import net.millida.launcher.core.cosmetics.CosmeticSections
import net.millida.launcher.core.cosmetics.Outfit
import net.millida.launcher.core.cosmetics.Outfits
import net.millida.launcher.core.rubies.RubyItem
import net.millida.launcher.core.skins.ShowcaseKind
import net.millida.launcher.core.skins.SkinShowcase
import net.millida.launcher.ui.MChip
import net.millida.launcher.ui.MDangerButton
import net.millida.launcher.ui.MDialog
import net.millida.launcher.ui.MEmptyState
import net.millida.launcher.ui.MErrorCard
import net.millida.launcher.ui.MField
import net.millida.launcher.ui.MIcon
import net.millida.launcher.ui.MIconButton
import net.millida.launcher.ui.MIcons
import net.millida.launcher.ui.MPrimaryButton
import net.millida.launcher.ui.MSignInGate
import net.millida.launcher.ui.MTextButton
import net.millida.launcher.ui.MillidaType
import net.millida.launcher.ui.Rarity
import net.millida.launcher.ui.SkinFigure
import net.millida.launcher.ui.millida
import net.millida.launcher.ui.shop.ItemArt

private val ART_SIDE = 112.dp

/** The shop's art reads the same preview and repaints a colourway by hue, so both screens show one picture. */
internal fun CosmeticItem.art(): RubyItem = RubyItem(
    code = id,
    name = name,
    slot = slot,
    rarity = rarity,
    preview = preview.ifEmpty { null },
    color = tint,
    tintFrom = tintFrom,
)

internal fun LazyGridScope.cosmeticItemsSection(p: CharacterPanel, sectionKey: String) {
    val emotes = sectionKey == CosmeticSections.EMOTES
    val state = p.cosmetics
    val model = p.wardrobe ?: return
    val catalog = state.catalog.items.filter(CosmeticItem::free)
    val error = state.error
    if (!state.catalogReady) {
        if (error != null) {
            full("cos-error") { MErrorCard(error, onRetry = { p.reloadCosmetics() }) }
        } else {
            skeletons("cos", 4)
        }
        return
    }
    val sections = CosmeticSections.of(catalog.map { it.slot }.toSet())
        .filter { it.key == sectionKey }
    if (sections.isEmpty()) {
        full("cos-empty") { MEmptyState(if (emotes) MIcons.smile else MIcons.shirt, "No items yet") }
        return
    }
    val section = sections.firstOrNull { it.key == p.itemSection } ?: sections.first()
    val chip = section.chips.firstOrNull { it.key == p.itemChip } ?: section.chips.first()
    if (!p.state.millidaSignedIn) {
        full("cos-sign-in") {
            MSignInGate(onSignIn = { p.signIn() }, title = "Sign in to wear items")
        }
    }
    if (error != null) {
        full("cos-error") { MErrorCard(error, onRetry = { p.reloadCosmetics() }) }
    }
    if (sections.size > 1) {
        full("cos-sections-${section.key}") {
            ChipRow {
                sections.forEach { option ->
                    MChip(option.name, selected = option.key == section.key, onClick = { p.onItemSection(option.key) })
                }
            }
        }
    }
    if (section.chips.size > 1) {
        full("cos-chips-${section.key}") {
            ChipRow {
                section.chips.forEach { option ->
                    MChip(option.name, selected = option.key == chip.key, onClick = { p.onItemChip(option.key) })
                }
            }
        }
    }
    val shelf = catalog.filter { it.slot in chip.slots }
    val wornHere = state.worn.any { it.slot in chip.slots }
    full("cos-title-${chip.key}") {
        ShelfTitle(chip.name) {
            if (wornHere || state.fitting.any { it.slot in chip.slots }) {
                MTextButton(
                    "Remove",
                    onClick = { model.takeOff(chip.slots) },
                    enabled = state.busy.isEmpty(),
                )
            }
            MIconButton(
                MIcons.refresh,
                onClick = { p.reloadCosmetics() },
                label = "Refresh",
                enabled = !state.loading,
            )
        }
    }
    if (shelf.isEmpty()) {
        full("cos-shelf-empty-${chip.key}") { MEmptyState(MIcons.shirt, "Nothing here yet") }
        return
    }
    items(shelf, key = { "cos:" + it.id }, contentType = { "tile" }) { item ->
        CosmeticTile(p, item, Modifier.animateItem())
    }
}

/** Cape-slot items of the mod hang next to account capes, as the desktop's cape section shows them. */
internal fun LazyGridScope.cosmeticCapes(p: CharacterPanel) {
    if (p.wardrobe == null) return
    val capes = p.cosmetics.catalog.items.filter { it.slot == "CAPE" && it.free }
    if (capes.isEmpty()) return
    full("cos-cape-title") {
        ShelfTitle("Cosmetic capes") {
            if (p.cosmetics.worn.any { it.slot == "CAPE" }) {
                MTextButton("Remove", onClick = { p.wardrobe?.takeOff(listOf("CAPE")) }, enabled = p.cosmetics.busy.isEmpty())
            }
        }
    }
    items(capes, key = { "cos-cape:" + it.id }, contentType = { "tile" }) { item ->
        CosmeticTile(p, item, Modifier.animateItem())
    }
}

@Composable
private fun CosmeticTile(p: CharacterPanel, item: CosmeticItem, modifier: Modifier) {
    val state = p.cosmetics
    val locked = state.locked(item)
    ItemTile(
        name = item.name,
        onClick = { p.wardrobe?.tap(item) },
        modifier = modifier,
        rarity = Rarity.of(item.rarity),
        mark = when {
            state.isFitting(item) -> TileMark.Trying
            state.isWorn(item) -> TileMark.Worn
            else -> TileMark.None
        },
        note = null,
        locked = locked,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ItemArt(item.art(), side = ART_SIDE)
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

internal fun LazyGridScope.showcaseSection(p: CharacterPanel, kind: ShowcaseKind) {
    val state = p.cosmetics
    val cards = state.showcase[kind]
    val failed = state.showcaseFailed[kind]
    when {
        failed != null -> full("showcase-error-${kind.wire}") {
            MErrorCard(failed, onRetry = { p.wardrobe?.loadShowcase(kind, force = true) })
        }
        cards == null -> skeletons("showcase-${kind.wire}", 4)
        cards.isEmpty() -> full("showcase-empty-${kind.wire}") { MEmptyState(MIcons.catalog, "The catalog is empty") }
        else -> items(cards, key = { "sc:" + kind.wire + ":" + it.name }, contentType = { "tile" }) { card ->
            CatalogTile(p, SkinShowcase.catalogSkin(card), Modifier.animateItem())
        }
    }
}

private fun things(count: Int): String {
    return "$count items"
}

internal fun LazyGridScope.looksSection(p: CharacterPanel) {
    val state = p.cosmetics
    if (p.target != WearTarget.Millida) {
        full("looks-gate") {
            if (p.target == WearTarget.SignedOut) {
                MSignInGate(onSignIn = { p.signIn() }, title = "Sign in to save outfits")
            } else {
                MEmptyState(MIcons.looks, "Outfits are saved to your Millida account")
            }
        }
        return
    }
    val look = p.currentLook
    if (state.outfits.size < Outfits.LIMIT && look != null) {
        item(key = "look-save", contentType = "tile") {
            ItemTile(
                name = if (state.outfits.isEmpty()) "Save outfit" else "New outfit",
                onClick = { p.wardrobe?.saveOutfit(look) },
                modifier = Modifier.animateItem(),
                note = "${state.outfits.size} of ${Outfits.LIMIT}",
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MIcon(MIcons.plus, size = 40.dp)
                }
            }
        }
    }
    if (look == null) {
        skeletons("looks", 2)
        return
    }
    items(state.outfits, key = { "look:" + it.id }, contentType = { "tile" }) { outfit ->
        val active = Outfits.same(outfit, look)
        ItemTile(
            name = outfit.name,
            onClick = { if (state.outfitBusy.isEmpty()) p.onWearOutfit(outfit) },
            modifier = Modifier.animateItem(),
            mark = if (active) TileMark.Worn else TileMark.None,
            note = if (state.outfitBusy == outfit.id) null else things(outfit.cosmetics.size + 1 + if (outfit.cape.url.isNotEmpty()) 1 else 0),
            onRemove = { p.onEditOutfit(outfit) },
            toolIcon = MIcons.edit,
            toolLabel = "Edit",
        ) {
            SkinFigure(url = outfit.skin.url, slim = outfit.skin.slim, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Rename or remove a look; removing asks once more, since a look was put together by hand. */
@Composable
internal fun OutfitEditDialog(outfit: Outfit, onRename: (String) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    var name by remember(outfit.id) { mutableStateOf(outfit.name) }
    var removing by remember(outfit.id) { mutableStateOf(false) }
    MDialog(
        title = if (removing) "Delete outfit?" else "Outfit",
        onDismiss = onDismiss,
        body = {
            if (removing) {
                Text("“${outfit.name}” will be removed from this device", style = MillidaType.body, color = millida.fgMuted)
            } else {
                MField(
                    value = name,
                    onValueChange = { name = it.take(Outfits.NAME_MAX) },
                    placeholder = "Name",
                    onImeAction = { onRename(name) },
                )
            }
        },
        actions = {
            if (removing) {
                MTextButton("Cancel", onClick = { removing = false })
                MDangerButton("Delete", onClick = onRemove)
            } else {
                MTextButton("Delete", onClick = { removing = true })
                MPrimaryButton("Save", onClick = { onRename(name) }, enabled = name.isNotBlank())
            }
        },
    )
}
