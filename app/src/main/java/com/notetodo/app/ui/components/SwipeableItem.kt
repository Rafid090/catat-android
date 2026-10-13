package com.notetodo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Swipe left to delete, swipe right to toggle done.
 *
 * Deleting dismisses the row; toggling snaps back, because the row stays in the
 * list with a checkbox that has simply flipped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItem(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleDone: (() -> Unit)? = null,
    isDone: Boolean = false,
    content: @Composable () -> Unit,
) {
    // rememberSwipeToDismissBoxState keeps the lambda it was first given, so the
    // callbacks have to be read through rememberUpdatedState. Capturing them
    // directly freezes the row's data as of first composition, and an undo then
    // restores a stale copy of the item.
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnToggleDone by rememberUpdatedState(onToggleDone)

    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    currentOnDelete()
                    true
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    currentOnToggleDone?.invoke()
                    false
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = onToggleDone != null,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = state.dismissDirection
            val isDelete = direction == SwipeToDismissBoxValue.EndToStart
            val background = if (isDelete) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
            val alignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(background)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment,
            ) {
                // Settled has no direction; drawing nothing avoids a flash of colour.
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        imageVector = when {
                            isDelete -> Icons.Default.Delete
                            isDone -> Icons.Default.Undo
                            else -> Icons.Default.Check
                        },
                        contentDescription = null,
                        tint = if (isDelete) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    )
                }
            }
        },
        content = { content() },
    )
}
