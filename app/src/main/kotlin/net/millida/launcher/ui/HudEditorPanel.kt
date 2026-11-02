package net.millida.launcher.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.millida.launcher.core.hud.HudAction
import net.millida.launcher.core.hud.HudControl
import net.millida.launcher.core.hud.HudIcons
import net.millida.launcher.core.hud.HudKeys
import net.millida.launcher.core.hud.HudKind
import net.millida.launcher.core.hud.HudLayout
import net.millida.launcher.core.hud.HudPresets
import net.millida.launcher.core.hud.HudProfiles
import net.millida.launcher.core.hud.HudTransfer
import net.millida.launcher.core.hud.HudVisibility
import java.util.Locale
import kotlin.math.roundToInt

private enum class EditorTab(val label: String) {
    CONTROL("Controls"),
    PROFILES("Layouts"),
    TOUCH("Touch"),
}

private enum class PressMode(val label: String) {
    HOLD("Hold"),
    TOGGLE("Toggle"),
    LATCH("Double tap"),
}

private enum class KeyPick { REPLACE, ADD }

private val VISIBILITIES = listOf(
    HudVisibility.LOOK,
    HudVisibility.WALKING,
    HudVisibility.FLYING,
    HudVisibility.CURSOR,
    HudVisibility.ALWAYS,
)

private fun visibilityLabel(visibility: HudVisibility): String = when (visibility) {
    HudVisibility.LOOK -> "In world"
    HudVisibility.WALKING -> "On ground"
    HudVisibility.FLYING -> "While flying"
    HudVisibility.CURSOR -> "In menus"
    HudVisibility.ALWAYS -> "Always"
}

private const val HOLD_STEP_MS = 25

@Composable
fun HudEditorPanel(
    profiles: HudProfiles,
    selected: HudControl?,
    sensitivity: Float,
    onProfiles: (HudProfiles) -> Unit,
    onPreset: (HudLayout) -> Unit,
    onChange: (HudControl) -> Unit,
    onLayout: (HudLayout) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: (HudControl) -> Unit,
    onSensitivity: (Float) -> Unit,
    onNotice: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hidden by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(EditorTab.CONTROL) }
    val colors = millida
    val sheetFrame = colors.plateFrame().copy(outline = colors.dockOutline)
    val layout = profiles.layout

    // The panel stands over the controls, and the one that has to be moved may
    // be exactly underneath. Hiding it leaves the screen to the player.
    if (hidden) {
        Row(
            modifier
                .pixelSurface(colors.raised, MillidaCut.control, sheetFrame)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MSecondaryButton("Show", onClick = { hidden = false }, icon = MIcons.chevronUp, size = MButtonSize.Small)
            MPrimaryButton("Done", onClick = onDone, icon = MIcons.check, size = MButtonSize.Small)
        }
        return
    }

    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .pixelSurface(colors.raised, MillidaCut.plate, sheetFrame, corners = PixelCorners.Top)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Controls",
                modifier = Modifier.weight(1f),
                style = MillidaType.dialogTitle,
                color = colors.fg,
                maxLines = 1,
            )
            MTextButton("Hide", onClick = { hidden = true }, icon = MIcons.chevronDown)
            Spacer(Modifier.width(8.dp))
            MPrimaryButton("Done", onClick = onDone, icon = MIcons.check, size = MButtonSize.Small)
        }

        MSegmented(
            options = EditorTab.entries.toList(),
            selected = tab,
            onSelect = { tab = it },
            label = { it.label },
        )

        when (tab) {
            EditorTab.CONTROL -> ControlTab(layout, selected, onChange, onRemove, onAdd)
            EditorTab.PROFILES -> ProfilesTab(profiles, onProfiles, onPreset, onNotice)
            EditorTab.TOUCH -> TouchTab(layout, sensitivity, onLayout, onSensitivity)
        }
    }
}

@Composable
private fun ControlTab(
    layout: HudLayout,
    selected: HudControl?,
    onChange: (HudControl) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: (HudControl) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    if (selected == null) {
        Text(
            "Drag a control to move it. Tap it to configure.",
            style = MillidaType.small,
            color = millida.fgSubtle,
        )
    } else {
        SelectedControl(layout, selected, onChange, onRemove)
    }

    MSecondaryButton(
        if (adding) "Hide list" else "Add control",
        onClick = { adding = !adding },
        icon = if (adding) MIcons.chevronUp else MIcons.plus,
        size = MButtonSize.Small,
    )

    if (adding) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            addable.forEach { (label, control) ->
                MChip(label, selected = false, onClick = { onAdd(control()) }, icon = MIcons.plus)
            }
        }
    }
}

@Composable
private fun SelectedControl(
    layout: HudLayout,
    selected: HudControl,
    onChange: (HudControl) -> Unit,
    onRemove: (String) -> Unit,
) {
    val colors = millida
    val canReset = (HudPresets.byName(layout.name) ?: HudPresets.bedrock()).controls.any { it.id == selected.id }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Control: ${selected.label}",
                modifier = Modifier.weight(1f),
                style = MillidaType.rowTitle,
                color = colors.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (canReset) {
                MTextButton(
                    "Reset",
                    onClick = { layout.reset(selected.id).controls.firstOrNull { it.id == selected.id }?.let(onChange) },
                    icon = MIcons.refresh,
                )
                Spacer(Modifier.width(6.dp))
            }
            MDangerButton("Remove", onClick = { onRemove(selected.id) }, icon = MIcons.trash)
        }
        EditorRow("Size") {
            MSlider(
                value = selected.scale,
                onValueChange = { onChange(selected.withSize(it)) },
                valueRange = HudControl.MIN_SCALE..HudControl.MAX_SCALE,
            )
        }
        EditorRow("Opacity") {
            MSlider(
                value = selected.shownOpacity(layout.shownOpacity),
                onValueChange = { onChange(selected.withOpacity(it)) },
                valueRange = HudLayout.MIN_OPACITY..1f,
            )
        }
        if (selected.opacity != null) {
            MTextButton("Default", onClick = { onChange(selected.withOpacity(null)) }, icon = MIcons.refresh)
        }
        MSegmented(
            options = listOf(false, true),
            selected = selected.hidden,
            onSelect = { onChange(selected.copy(hidden = it)) },
            label = { if (it) "Hidden" else "Visible" },
        )
        if (!selected.anchored && selected.kind != HudKind.FLOAT_STICK) {
            MSectionTitle("VISIBILITY")
            MSegmented(
                options = VISIBILITIES,
                selected = selected.visibility,
                onSelect = { onChange(selected.copy(visibility = it)) },
                label = ::visibilityLabel,
            )
        }
        if (selected.kind == HudKind.KEY || selected.kind == HudKind.MOUSE) {
            MSegmented(
                options = PressMode.entries.toList(),
                selected = when {
                    selected.toggle -> PressMode.TOGGLE
                    selected.latch -> PressMode.LATCH
                    else -> PressMode.HOLD
                },
                onSelect = { onChange(selected.copy(toggle = it == PressMode.TOGGLE, latch = it == PressMode.LATCH)) },
                label = { it.label },
            )
        }
        if (selected.rebindable) {
            KeyBinding(selected, onChange)
        }
    }
}

@Composable
private fun KeyBinding(selected: HudControl, onChange: (HudControl) -> Unit) {
    var pick by remember(selected.id) { mutableStateOf<KeyPick?>(null) }
    val keys = selected.keys
    MSectionTitle("KEYS")
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { code ->
            MChip(
                HudKeys.name(code),
                selected = true,
                onClick = { if (keys.size > 1) onChange(selected.withKeys(keys - code)) },
                icon = if (keys.size > 1) MIcons.close else null,
            )
        }
        MTextButton(
            "Change",
            onClick = { pick = if (pick == KeyPick.REPLACE) null else KeyPick.REPLACE },
            icon = MIcons.edit,
        )
        if (keys.size < HudControl.MAX_KEYS) {
            MTextButton(
                "Key combination",
                onClick = { pick = if (pick == KeyPick.ADD) null else KeyPick.ADD },
                icon = MIcons.plus,
            )
        }
    }
    val mode = pick ?: return
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HudKeys.bindable.forEach { (code, name) ->
            MChip(
                name,
                selected = code in keys,
                onClick = {
                    onChange(selected.withKeys(if (mode == KeyPick.REPLACE) listOf(code) else keys + code))
                    pick = null
                },
            )
        }
    }
}

@Composable
private fun ProfilesTab(
    profiles: HudProfiles,
    onProfiles: (HudProfiles) -> Unit,
    onPreset: (HudLayout) -> Unit,
    onNotice: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var renaming by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val active = profiles.activeName

    fun create(next: HudProfiles) {
        if (next == profiles) onNotice("You already have ${HudProfiles.MAX_PROFILES} layouts. Remove one first.") else onProfiles(next)
    }

    fun commitRename() {
        onProfiles(profiles.rename(active, draft))
        renaming = false
    }

    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        profiles.names.forEach { name ->
            MChip(name, selected = name == active, onClick = {
                confirmDelete = false
                renaming = false
                onProfiles(profiles.switchTo(name))
            })
        }
    }

    when {
        renaming -> Row(verticalAlignment = Alignment.CenterVertically) {
            MField(
                value = draft,
                onValueChange = { draft = it.take(HudProfiles.MAX_NAME) },
                placeholder = "Name",
                modifier = Modifier.weight(1f),
                onImeAction = ::commitRename,
            )
            Spacer(Modifier.width(8.dp))
            MPrimaryButton("Save", onClick = ::commitRename, icon = MIcons.check, size = MButtonSize.Small)
            MTextButton("Cancel", onClick = { renaming = false })
        }
        confirmDelete -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Delete \"$active\"?",
                modifier = Modifier.weight(1f),
                style = MillidaType.body,
                color = millida.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MTextButton("Cancel", onClick = { confirmDelete = false })
            MDangerButton("Delete", onClick = {
                confirmDelete = false
                onProfiles(profiles.delete(active))
            }, icon = MIcons.trash)
        }
        else -> Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MSecondaryButton("New", onClick = { create(profiles.create(HudProfiles.NEW_NAME)) }, icon = MIcons.plus, size = MButtonSize.Small)
            MSecondaryButton("Duplicate", onClick = { create(profiles.duplicate()) }, icon = MIcons.copy, size = MButtonSize.Small)
            MSecondaryButton("Rename", onClick = {
                draft = active
                renaming = true
            }, icon = MIcons.edit, size = MButtonSize.Small)
            MDangerButton("Delete", onClick = { confirmDelete = true }, enabled = profiles.names.size > 1, icon = MIcons.trash)
        }
    }

    MSectionTitle("PRESET")
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HudPresets.all().forEach { preset ->
            MChip(
                preset.name,
                selected = preset.name == profiles.layout.name,
                onClick = { onPreset(preset) },
            )
        }
    }

    MSectionTitle("IMPORT / EXPORT")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MSecondaryButton(
            "Copy",
            onClick = {
                clipboard.setText(AnnotatedString(HudTransfer.encode(profiles.layout)))
                onNotice("Layout copied")
            },
            icon = MIcons.copy,
            size = MButtonSize.Small,
        )
        MSecondaryButton(
            "Paste",
            onClick = {
                val imported = clipboard.getText()?.text?.let(HudTransfer::decode)
                if (imported == null) {
                    onNotice("No layout found in the clipboard. Copy one first.")
                } else {
                    create(profiles.create(imported.name, imported))
                }
            },
            icon = MIcons.download,
            size = MButtonSize.Small,
        )
    }
}

@Composable
private fun TouchTab(
    layout: HudLayout,
    sensitivity: Float,
    onLayout: (HudLayout) -> Unit,
    onSensitivity: (Float) -> Unit,
) {
    var look by remember(sensitivity) { mutableFloatStateOf(sensitivity) }
    EditorRow("Opacity") {
        MSlider(
            value = layout.shownOpacity,
            onValueChange = { onLayout(layout.withOpacity(it)) },
            valueRange = HudLayout.MIN_OPACITY..1f,
        )
    }
    MSwitchRow(
        "Tap to attack",
        "A short tap attacks instead of using an item",
        layout.tapHits,
    ) { hits -> onLayout(layout.withLookTap(if (hits) HudKeys.MOUSE_LEFT else HudKeys.MOUSE_RIGHT)) }
    EditorRow("Look ${String.format(Locale.ROOT, "%.1f×", look)}") {
        MSlider(
            value = look,
            onValueChange = { look = it },
            valueRange = 0.3f..3f,
            steps = 26,
            onValueChangeFinished = { onSensitivity(look) },
        )
    }
    EditorRow("Hold ${layout.shownHoldDelay} ms") {
        MSlider(
            value = layout.shownHoldDelay.toFloat(),
            onValueChange = { onLayout(layout.withHoldDelay(((it / HOLD_STEP_MS).roundToInt() * HOLD_STEP_MS))) },
            valueRange = HudLayout.MIN_HOLD_MS.toFloat()..HudLayout.MAX_HOLD_MS.toFloat(),
            steps = (HudLayout.MAX_HOLD_MS - HudLayout.MIN_HOLD_MS) / HOLD_STEP_MS - 1,
        )
    }
}

@Composable
private fun EditorRow(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.width(132.dp),
            style = MillidaType.small,
            color = millida.fgSubtle,
            maxLines = 1,
        )
        Box(Modifier.weight(1f)) { content() }
    }
}

private val addable: List<Pair<String, () -> HudControl>> = listOf(
    "Jump" to { key("jump", "Jump", HudKeys.SPACE, wide = true) },
    "Sneak" to { key("sneak", "Sneak", HudKeys.LEFT_SHIFT, wide = true, toggle = true) },
    "Sprint" to { key("sprint", "Sprint", HudKeys.LEFT_CONTROL, wide = true, toggle = true) },
    "W" to { key("forward", "W", HudKeys.W) },
    "A" to { key("left", "A", HudKeys.A) },
    "S" to { key("back", "S", HudKeys.S) },
    "D" to { key("right", "D", HudKeys.D) },
    "Inventory" to { key("inventory", "Inventory", HudKeys.E, wide = true) },
    "Chat" to { key("chat", "Chat", HudKeys.T, wide = true) },
    "Drop item" to { key("drop", "Drop item", HudKeys.Q, wide = true) },
    "Pick block" to { key("pick", "Pick block", HudKeys.F, wide = true) },
    "Game menu" to { key("escape", "Esc", HudKeys.ESCAPE, wide = true, visibility = HudVisibility.ALWAYS) },
    "Player list" to { key("tab", "Players", HudKeys.TAB, wide = true) },
    "Debug screen" to { key("debug", "F3", HudKeys.F3, wide = true) },
    "Hide HUD (F1)" to { key("hide", "F1", HudKeys.F1) },
    "Screenshot (F2)" to { key("screenshot", "F2", HudKeys.F2) },
    "Change view (F5)" to { key("camera", "F5", HudKeys.F5) },
    "Hotbar slot 1" to { key("slot1", "1", HudKeys.ONE) },
    "Hotbar slot 2" to { key("slot2", "2", HudKeys.TWO) },
    "Hotbar slot 3" to { key("slot3", "3", HudKeys.THREE) },
    "Hotbar slot 4" to { key("slot4", "4", HudKeys.FOUR) },
    "Hotbar slot 5" to { key("slot5", "5", HudKeys.FIVE) },
    "Hotbar slot 6" to { key("slot6", "6", HudKeys.SIX) },
    "Hotbar slot 7" to { key("slot7", "7", HudKeys.SEVEN) },
    "Hotbar slot 8" to { key("slot8", "8", HudKeys.EIGHT) },
    "Hotbar slot 9" to { key("slot9", "9", HudKeys.NINE) },
    "Previous hotbar slot" to { wheel("slot-prev", "Slot −", HudKeys.SCROLL_UP) },
    "Next hotbar slot" to { wheel("slot-next", "Slot +", HudKeys.SCROLL_DOWN) },
    "Attack" to { mouse("hit", "Attack", HudKeys.MOUSE_LEFT) },
    "Use item" to { mouse("use", "Use", HudKeys.MOUSE_RIGHT) },
    "Middle mouse" to { mouse("middle", "Copy block", HudKeys.MOUSE_MIDDLE) },
    "Keyboard" to { action("keyboard", "Keyboard", HudAction.KEYBOARD, 140f) },
    "Menu" to { action("menu", "Menu", HudAction.MENU, 96f) },
    "Pointer" to { action("mode", "Pointer", HudAction.POINTER_MODE, 104f) },
    "Joystick" to {
        HudControl("stick", "Movement", HudKind.STICK, x = 0.5f, y = 0.5f, width = 176f, height = 176f)
    },
    "Floating joystick" to { HudPresets.base(HudKind.FLOAT_STICK).copy(x = 0.15f, y = 0.72f) },
    "Jump and fly" to { HudPresets.base(HudKind.JUMP).copy(icon = HudIcons.JUMP) },
    "Inventory shortcut" to { HudPresets.base(HudKind.HOTBAR_MORE).copy(icon = HudIcons.MORE) },
)

private fun key(
    id: String,
    label: String,
    code: Int,
    wide: Boolean = false,
    toggle: Boolean = false,
    visibility: HudVisibility = HudVisibility.LOOK,
): HudControl = HudControl(
    id = id,
    label = label,
    kind = HudKind.KEY,
    code = code,
    x = 0.5f,
    y = 0.5f,
    width = if (wide) 104f else 64f,
    height = 46f,
    toggle = toggle,
    visibility = visibility,
)

private fun wheel(id: String, label: String, notches: Int): HudControl = HudControl(
    id = id,
    label = label,
    kind = HudKind.SCROLL,
    code = notches,
    x = 0.5f,
    y = 0.5f,
    width = 80f,
    height = 46f,
)

private fun mouse(id: String, label: String, code: Int): HudControl = HudControl(
    id = id,
    label = label,
    kind = HudKind.MOUSE,
    code = code,
    x = 0.5f,
    y = 0.5f,
    width = 104f,
    height = 46f,
)

private fun action(id: String, label: String, what: HudAction, width: Float): HudControl = HudControl(
    id = id,
    label = label,
    kind = HudKind.ACTION,
    action = what,
    x = 0.5f,
    y = 0.5f,
    width = width,
    height = 44f,
    visibility = HudVisibility.ALWAYS,
)
