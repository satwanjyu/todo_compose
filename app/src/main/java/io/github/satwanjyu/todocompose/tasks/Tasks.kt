package io.github.satwanjyu.todocompose.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.satwanjyu.todocompose.R
import io.github.satwanjyu.todocompose.tasks.data.Task
import io.github.satwanjyu.todocompose.tasks.edit.EditTaskAction
import io.github.satwanjyu.todocompose.tasks.edit.EditTaskScaffold
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.serialization.Serializable

// TODO replace route strings with serializable objects
@Serializable
object Tasks

// Use
fun NavGraphBuilder.tasks(
    windowWidthSizeClass: WindowWidthSizeClass,
) {
    composable("tasks") {
        when (windowWidthSizeClass) {
            WindowWidthSizeClass.Compact -> TasksCompact()
            WindowWidthSizeClass.Medium -> TasksMedium()
            WindowWidthSizeClass.Expanded -> TasksExpanded()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksCompact(
    viewModel: TasksViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var searchBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
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

    Scaffold(
        floatingActionButton = {
            val searching = uiState is UiState.Tick
            AnimatedVisibility(
                visible = !searching,
                enter = slideIn { IntOffset(it.width / 2, it.height / 2) } + fadeIn(),
                exit = slideOut { IntOffset(it.width / 2, it.height / 2) } + fadeOut(),
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.new_task))
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    FloatingActionButton(
                        onClick = {
                            // TODO create task full-screen dialog
                        },
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.new_task)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection),
        ) {
            TaskList(
                tasks = uiState.tasks,
                mode = when (uiState) {
                    is UiState.Select -> TaskListMode.Select(uiState.selectedTasks)
                    else -> TaskListMode.Tick
                },
                onTaskChange = onTick,
                onSelectedTasksChange = onSelectedTasksChange,
                onNavigateToEdit = onNavigateToEdit,
                contentPadding = PaddingValues(top = 64.dp)
            )
            AnimatedContent(
                modifier = Modifier.align(Alignment.TopCenter),
                targetState = uiState,
                contentKey = { it::class },
                label = "top bar animated state"
            ) { uiState ->
                when (uiState) {
                    is UiState.Tick -> {
                        AnimatedVisibility(
                            visible = searchBarVisible,
                            enter = slideInVertically { -it },
                            exit = slideOutVertically { -it },
                        ) {
                            TasksSearchBar(
                                docked = false,
                                query = uiState.query,
                                onQueryChange = onQueryChange,
                                enabled = uiState.tasks.isNotEmpty(),
                                queriedTasks = uiState.queriedTasks,
                                onNavigateToEdit = onNavigateToEdit,
                            )
                        }
                    }

                    is UiState.Select -> {
                        TasksSelectAppBar(
                            selectedTasks = uiState.selectedTasks,
                            onSelectedTasksChange = onSelectedTasksChange,
                            onRemoveTasks = onRemoveTasks,
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun TasksMedium(viewModel: TasksViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}

@Composable
private fun TasksExpanded(viewModel: TasksViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListScaffold(
    uiState: UiState,
    onTick: (Task) -> Unit,
    onSelectedTasksChange: (ImmutableSet<Task>) -> Unit,
    onRemoveTasks: (ImmutableSet<Task>) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Task) -> Unit,
    onQueryChange: (String?) -> Unit,
) {
    var searchBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
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

    Scaffold(
        floatingActionButton = {
            val searching = uiState is UiState.Tick && uiState.query != null
            AnimatedVisibility(
                visible = !searching,
                enter = slideIn { IntOffset(it.width / 2, it.height / 2) } + fadeIn(),
                exit = slideOut { IntOffset(it.width / 2, it.height / 2) } + fadeOut(),
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.new_task))
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    FloatingActionButton(
                        onClick = onNavigateToCreate,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.new_task)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection),
        ) {
            TaskList(
                tasks = uiState.tasks,
                mode = when (uiState) {
                    is UiState.Select -> TaskListMode.Select(uiState.selectedTasks)
                    else -> TaskListMode.Tick
                },
                onTaskChange = onTick,
                onSelectedTasksChange = onSelectedTasksChange,
                onNavigateToEdit = onNavigateToEdit,
                contentPadding = PaddingValues(top = 64.dp)
            )
            AnimatedContent(
                modifier = Modifier.align(Alignment.TopCenter),
                targetState = uiState,
                contentKey = { it::class },
                label = "top bar animated state"
            ) { uiState ->
                when (uiState) {
                    is UiState.Tick -> {
                        AnimatedVisibility(
                            visible = searchBarVisible,
                            enter = slideInVertically { -it },
                            exit = slideOutVertically { -it },
                        ) {
                            TasksSearchBar(
                                docked = false,
                                query = uiState.query,
                                onQueryChange = onQueryChange,
                                enabled = uiState.tasks.isNotEmpty(),
                                queriedTasks = uiState.queriedTasks,
                                onNavigateToEdit = onNavigateToEdit,
                            )
                        }
                    }

                    is UiState.Select -> {
                        TasksSelectAppBar(
                            selectedTasks = uiState.selectedTasks,
                            onSelectedTasksChange = onSelectedTasksChange,
                            onRemoveTasks = onRemoveTasks,
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

