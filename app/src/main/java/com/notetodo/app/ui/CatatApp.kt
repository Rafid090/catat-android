package com.notetodo.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notetodo.app.R
import com.notetodo.app.ui.notes.NoteEditScreen
import com.notetodo.app.ui.notes.NoteEditViewModel
import com.notetodo.app.ui.notes.NotesScreen
import com.notetodo.app.ui.tasks.TaskEditScreen
import com.notetodo.app.ui.tasks.TaskEditViewModel
import com.notetodo.app.ui.tasks.TasksScreen

object Route {
    const val NOTES = "notes"
    const val TASKS = "tasks"
    const val NOTE_EDIT = "note_edit"
    const val TASK_EDIT = "task_edit"

    fun noteEdit(id: Long) = "$NOTE_EDIT/$id"
    fun taskEdit(id: Long) = "$TASK_EDIT/$id"
}

@Composable
fun CatatApp(navController: NavHostController = rememberNavController()) {

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Passed into each list screen so its Scaffold lays out the FAB above the bar.
    val bottomBar: @Composable () -> Unit = {
        CatatBottomBar(
            currentRoute = currentRoute,
            onSelect = { route ->
                if (route != currentRoute) {
                    navController.navigate(route) {
                        popUpTo(Route.NOTES) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            },
        )
    }

    NavHost(navController = navController, startDestination = Route.NOTES) {

        composable(Route.NOTES) {
            NotesScreen(
                onOpenNote = { id -> navController.navigate(Route.noteEdit(id)) },
                bottomBar = bottomBar,
            )
        }

        composable(Route.TASKS) {
            TasksScreen(
                onOpenTask = { id -> navController.navigate(Route.taskEdit(id)) },
                bottomBar = bottomBar,
            )
        }

        composable(
            route = "${Route.NOTE_EDIT}/{${NoteEditViewModel.NOTE_ID_ARG}}",
            arguments = listOf(
                navArgument(NoteEditViewModel.NOTE_ID_ARG) { type = NavType.LongType },
            ),
        ) {
            NoteEditScreen(onDone = { navController.popBackStack() })
        }

        composable(
            route = "${Route.TASK_EDIT}/{${TaskEditViewModel.TASK_ID_ARG}}",
            arguments = listOf(
                navArgument(TaskEditViewModel.TASK_ID_ARG) { type = NavType.LongType },
            ),
        ) {
            TaskEditScreen(onDone = { navController.popBackStack() })
        }
    }
}

@Composable
private fun CatatBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Route.NOTES,
            onClick = { onSelect(Route.NOTES) },
            icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_notes)) },
        )
        NavigationBarItem(
            selected = currentRoute == Route.TASKS,
            onClick = { onSelect(Route.TASKS) },
            icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_tasks)) },
        )
    }
}
