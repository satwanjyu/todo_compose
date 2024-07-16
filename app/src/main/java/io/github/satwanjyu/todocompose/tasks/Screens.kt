package io.github.satwanjyu.todocompose.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

// TODO replace route strings with serializable objects
fun NavGraphBuilder.tasks(
    windowWidthSizeClass: WindowWidthSizeClass,
) {
    composable("tasks") {
        when (windowWidthSizeClass) {
            else -> TasksCompact()
        }
    }
}

@Composable
private fun TasksCompact(
    viewModel: TasksViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = uiState,
        contentKey = { state ->
            listOf(state.creatingTask, state.editingTask)
        },
        label = "animate between task list and edit task fullscreen dialog",
    ) { state ->
        when {
            state.creatingTask -> {
                EditTaskScaffold(
                    action = EditTaskAction.Create,
                    title = viewModel.taskTitleText,
                    onTitleChange = { viewModel.taskTitleText = it },
                    notes = viewModel.taskNotesText,
                    onNotesChange = { viewModel.taskNotesText = it },
                    onTaskSave = { title, notes ->
                        viewModel.run {
                            insertTask(title, notes)
                            dismissCreateTask()
                            taskTitleText = ""
                            taskNotesText = ""
                        }
                    },
                    onDismiss = viewModel::dismissCreateTask,
                )
            }

            state.editingTask != null -> {
                val prev = state.editingTask
                EditTaskScaffold(
                    action = EditTaskAction.Edit(task = prev),
                    title = viewModel.taskTitleText,
                    onTitleChange = { viewModel.taskTitleText = it },
                    notes = viewModel.taskNotesText,
                    onNotesChange = { viewModel.taskNotesText = it },
                    onTaskSave = { title, notes ->
                        viewModel.run {
                            insertTask(prev.copy(title = title, notes = notes))
                            dismissEditTask()
                            taskTitleText = ""
                            taskNotesText = ""
                        }
                    },
                    onDismiss = viewModel::dismissEditTask
                )
            }

            else -> {
                Scaffold(
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = viewModel.queryText == null,
                            enter = slideIn { IntOffset(it.width / 2, it.height / 2) } + fadeIn(),
                            exit = slideOut { IntOffset(it.width / 2, it.height / 2) } + fadeOut(),
                        ) {
                            NewTaskFab(
                                style = FabStyle.Standard,
                                onClick = viewModel::createTask
                            )
                        }
                    }
                ) { paddingValues ->
                    TaskList(
                        modifier = Modifier.padding(paddingValues),
                        tasks = uiState.tasks,
                        onTaskChange = viewModel::insertTask,
                        selectedTasks = uiState.selectedTasks,
                        onSelectedTasksChange = viewModel::selectTasks,
                        onRemoveTasks = viewModel::removeTasks,
                        onEditTask = { task ->
                            viewModel.run {
                                taskTitleText = task.title
                                taskNotesText = task.notes
                                editTask(task)
                            }
                        },
                        query = viewModel.queryText,
                        onQueryChange = { query ->
                            viewModel.run {
                                queryText = query
                                if (query != null) {
                                    searchTask(query)
                                }
                            }
                        },
                        queriedTasks = uiState.queriedTasks,
                        onQueriedTaskClick = { task ->
                            viewModel.run {
                                taskTitleText = task.title
                                taskNotesText = task.notes
                                editTask(task)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TasksMedium(viewModel: TasksViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { paddingValues ->
        Row(
            modifier = Modifier.padding(paddingValues),
        ) {
            var searchBarVisible by remember { mutableStateOf(true) }
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        // Scrolling down
                        if (available.y < -10) {
                            searchBarVisible = false
                            // Scrolling up
                        } else if (available.y > 10) {
                            searchBarVisible = true
                        }

                        return Offset.Zero
                    }
                }
            }

            NavigationRail(header = {
                NewTaskFab(
                    style = FabStyle.Standard
                ) {
                    // TODO
                }
            }) {
                // TODO NavRail items
            }
            Box(
                modifier = Modifier.nestedScroll(nestedScrollConnection)
            ) {

            }
        }
    }
}

@Composable
private fun TasksExpanded(viewModel: TasksViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}
