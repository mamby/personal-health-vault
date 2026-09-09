package net.mamby.health.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import net.mamby.androidkit.compose.layout.AndroidKitPageAction
import net.mamby.androidkit.compose.layout.AndroidKitPageActionItem
import net.mamby.androidkit.navigation3.listDetailBackAction
import net.mamby.health.R
import net.mamby.androidkit.compose.action.AndroidKitFloatingAction

@Composable
fun floatingAddAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
): AndroidKitFloatingAction.Button = AndroidKitFloatingAction.Button(
    icon = painterResource(R.drawable.ic_lucide_plus), label = label,
    onClick = onClick, modifier = modifier, tooltip = label,
)

@Composable
fun titleBarAction(
    label: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
): AndroidKitPageAction = AndroidKitPageAction(
    icon = ImageVector.vectorResource(icon),
    label = label,
    onClick = onClick,
    enabled = enabled,
)

@Composable
fun detailTitleBarActions(
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
): List<AndroidKitPageActionItem> = buildList {
    onEdit?.let { edit ->
        add(
            titleBarAction(
                label = stringResource(R.string.common_edit),
                icon = R.drawable.ic_lucide_pencil,
                onClick = edit,
            ),
        )
    }
    onDelete?.let { delete ->
        add(
            titleBarAction(
                label = stringResource(R.string.common_delete),
                icon = R.drawable.ic_lucide_trash_2,
                onClick = delete,
            ),
        )
    }
}

@Composable
fun listDetailAwareBack(onBack: () -> Unit): (() -> Unit)? = listDetailBackAction(onBack)
