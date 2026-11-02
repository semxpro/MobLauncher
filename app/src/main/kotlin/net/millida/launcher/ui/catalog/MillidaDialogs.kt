package net.millida.launcher.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.millida.launcher.LauncherViewModel

/**
 * Phone refusal and install-confirmation dialogs are shared across screens.
 */
@Composable
fun MillidaDialogsHost(model: LauncherViewModel) {
    val shelf = model.millidaShelf
    val content = model.millidaContent
    val ms by shelf.state.collectAsStateWithLifecycle()
    val cs by content.state.collectAsStateWithLifecycle()

    ms.refusal?.let { refusal -> PhoneRefusalDialog(refusal = refusal, onDismiss = shelf::dismissRefusal) }

    cs.ask?.let { ask ->
        ContentAskDialog(
            ask = ask,
            owned = ms.access[ask.slug] == true,
            onConfirm = content::confirmAsk,
            onDismiss = content::dismissAsk,
        )
    }
}
