package io.github.satwanjyu.todocompose.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.satwanjyu.todocompose.R
import io.github.satwanjyu.todocompose.ui.theme.TodoComposeTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentList


fun NavGraphBuilder.tasks(
    windowWidthSizeClass: WindowWidthSizeClass,
) {
    composable("tasks") {
        when (windowWidthSizeClass) {
            WindowWidthSizeClass.Compact -> TasksCompact()
            WindowWidthSizeClass.Medium -> TasksCompact()
            WindowWidthSizeClass.Expanded -> TasksExpanded()
        }
    }
}

private fun uiStateType(uiState: UiState) = when (uiState) {
    is UiState.Tick, is UiState.Select -> 0
    is UiState.Create -> 1
    is UiState.Edit -> 2
}

@Composable
private fun TasksCompact(viewModel: TasksViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // TODO Should be implemented as self-contained navigation
    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            if (uiStateType(uiState) == 0) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            }
        },
        contentKey = { uiStateType(it) },
        label = "crossfade"
    ) { state ->
        when (state) {
            is UiState.Tick, is UiState.Select -> TaskListScaffold(
                uiState = state,
                onEditTask = viewModel::insertTask,
                onSelectedTasksChange = { selectedTasks ->
                    when {
                        selectedTasks.isEmpty() -> viewModel.uiState.value =
                            UiState.Tick(uiState.tasks)

                        else -> viewModel.uiState.value =
                            UiState.Select(uiState.tasks, selectedTasks.toImmutableSet())
                    }
                },
                onRemoveTasks = {
                    viewModel.removeTasks(it)
                    viewModel.uiState.value = UiState.Tick(uiState.tasks)
                },
                onNavigateToCreate = {
                    viewModel.uiState.value = UiState.Create(uiState.tasks, "", "")
                },
                onNavigateToEdit = {
                    viewModel.uiState.value = UiState.Edit(uiState.tasks, it)
                },
            )

            is UiState.Create -> {
                EditTaskScaffold(
                    mode = EditTaskMode.Create(state.title, state.notes),
                    onCreateBufferChange = { title, notes ->
                        viewModel.uiState.value =
                            UiState.Create(tasks = state.tasks, title = title, notes = notes)
                    },
                    onCreateTask = { title, notes -> viewModel.insertTask(title, notes) },
                    onEditTask = {},
                    onEditBufferChange = {},
                    onDismiss = { viewModel.uiState.value = UiState.Tick(state.tasks) }
                )
            }

            is UiState.Edit -> {
                EditTaskScaffold(
                    mode = EditTaskMode.Edit(state.task),
                    onCreateBufferChange = { _, _ -> },
                    onCreateTask = { _, _ -> },
                    onEditTask = { viewModel.insertTask(it) },
                    onEditBufferChange = { viewModel.uiState.value = state.copy(task = it) },
                    onDismiss = { viewModel.uiState.value = UiState.Tick(state.tasks) }
                )
            }
        }
    }
}

@Composable
private fun TasksExpanded(viewModel: TasksViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // TODO Basically identical to TasksCompact, consider merging the two.
    TwoPaneTaskListScaffold(
        uiState = uiState,
        onEditBufferChange = { viewModel.uiState.value = UiState.Edit(uiState.tasks, it) },
        onSelectedTasksChange = { selectedTasks ->
            when {
                selectedTasks.isEmpty() -> viewModel.uiState.value =
                    UiState.Tick(uiState.tasks)

                else -> viewModel.uiState.value =
                    UiState.Select(uiState.tasks, selectedTasks.toImmutableSet())
            }
        },
        onNavigateToEdit = {
            viewModel.uiState.value = UiState.Edit(uiState.tasks, it)
        },
        onCreateBufferChange = { title, notes ->
            viewModel.uiState.value =
                UiState.Create(tasks = uiState.tasks, title = title, notes = notes)
        },
        onEditTask = {
            viewModel.insertTask(it)
            viewModel.uiState.value = UiState.Tick(uiState.tasks)
        },
        onDismiss = { viewModel.uiState.value = UiState.Tick(uiState.tasks) },
        onRemoveTasks = {
            viewModel.removeTasks(it)
            viewModel.uiState.value = UiState.Tick(uiState.tasks)
        },
        onTaskCreate = { title, notes ->
            viewModel.insertTask(title, notes)
            viewModel.uiState.value = UiState.Tick(uiState.tasks)
        },
        onNavigateToCreate = {
            viewModel.uiState.value = UiState.Create(uiState.tasks, "", "")
        }
    )
}

enum class TaskItemMode { Tick, Select }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskItem(
    modifier: Modifier = Modifier,
    title: String,
    notes: String,
    mode: TaskItemMode,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = when {
                    mode == TaskItemMode.Tick && checked -> LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                    )

                    else -> LocalTextStyle.current
                },
            )
        },
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        supportingContent = {
            Text(
                notes,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Checkbox(checked, onCheckedChange)
        },
        colors = when {
            mode == TaskItemMode.Select && checked -> ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )

            else -> ListItemDefaults.colors()
        }
    )
}

@Preview
@Composable
private fun TaskItemPreview(
    @PreviewParameter(provider = LoremIpsum::class) lorem: String,
) {
    val words = lorem
        .replace(Regex("[^A-Za-z ]"), "")
        .split(" ")
    val title = words.subList(0, 5).joinToString(" ")
    val notes = words.subList(5, 10).joinToString(" ")

    TodoComposeTheme {
        TaskItem(
            title = title,
            notes = notes,
            mode = TaskItemMode.Tick,
            checked = false,
            onCheckedChange = {},
            onClick = {},
            onLongClick = {},
        )
    }
}

@Preview
@Composable
private fun TaskItemPreviewTicked(
    @PreviewParameter(provider = LoremIpsum::class) lorem: String,
) {
    val words = lorem
        .replace(Regex("[^A-Za-z ]"), "")
        .split(" ")
    val title = words.subList(0, 5).joinToString(" ")
    val notes = words.subList(5, 10).joinToString(" ")

    TodoComposeTheme {
        TaskItem(
            title = title,
            notes = notes,
            checked = true,
            mode = TaskItemMode.Tick,
            onCheckedChange = {},
            onClick = {},
            onLongClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskListScaffold(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onEditTask: (Task) -> Unit,
    onSelectedTasksChange: (Set<Task>) -> Unit,
    onRemoveTasks: (Set<Task>) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Task) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // TODO Refer to Material Motion
            val tweenFloat = tween<Float>(100)
            val tweenIntSize = tween<IntSize>(100)
            val tweenIntOffset = tween<IntOffset>(100)
            LargeTopAppBar(
                title = {
                    Crossfade(
                        targetState = uiState,
                        animationSpec = tweenFloat,
                        label = "title crossfade"
                    ) { state ->
                        Text(
                            when (state) {
                                is UiState.Select -> stringResource(
                                    R.string.task_selected,
                                    state.selectedTasks.size
                                )

                                else -> stringResource(R.string.tasks)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    val visible = when (uiState) {
                        is UiState.Select -> true
                        else -> false
                    }
                    AnimatedVisibility(
                        visible,
                        enter = slideInHorizontally(tweenIntOffset) { -it / 2 } +
                                expandHorizontally(tweenIntSize, Alignment.Start) +
                                fadeIn(tweenFloat),
                        exit = slideOutHorizontally(tweenIntOffset) { -it / 2 } +
                                shrinkHorizontally(tweenIntSize, Alignment.Start) +
                                fadeOut(tweenFloat),
                    ) {
                        IconButton(onClick = {
                            when (uiState) {
                                is UiState.Select -> onSelectedTasksChange(emptySet())
                                else -> {}
                            }
                        }) {
                            Icon(Icons.Default.Close, stringResource(R.string.dismiss))
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = when (uiState) {
                            is UiState.Select -> true
                            else -> false
                        },
                        enter = slideInHorizontally(tweenIntOffset) { it / 2 } +
                                expandHorizontally(tweenIntSize, Alignment.End) +
                                fadeIn(tweenFloat),
                        exit = slideOutHorizontally(tweenIntOffset) { it / 2 } +
                                shrinkHorizontally(tweenIntSize, Alignment.End) +
                                fadeOut(tweenFloat),
                    ) {
                        IconButton(onClick = {
                            when (uiState) {
                                is UiState.Select -> {
                                    onRemoveTasks(uiState.selectedTasks)
                                }

                                else -> {}
                            }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                stringResource(R.string.remove_tasks)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_task)
                )
            }
        }
    ) { paddingValues ->
        TaskList(
            modifier = Modifier.padding(paddingValues),
            tasks = uiState.tasks,
            mode = when (uiState) {
                is UiState.Select -> TaskListMode.Select(uiState.selectedTasks)
                else -> TaskListMode.Tick
            },
            onTaskChange = onEditTask,
            onSelectedTasksChange = onSelectedTasksChange,
            onNavigateToEdit = onNavigateToEdit
        )
    }
}

private sealed interface TaskListMode {
    object Tick : TaskListMode
    data class Select(val selectedTasks: ImmutableSet<Task>) : TaskListMode
}

@Composable
private fun TaskList(
    modifier: Modifier = Modifier,
    tasks: ImmutableList<Task>,
    mode: TaskListMode,
    onTaskChange: (Task) -> Unit,
    onSelectedTasksChange: (Set<Task>) -> Unit,
    onNavigateToEdit: (Task) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskItem(
                title = task.title,
                notes = task.notes,
                checked = when (mode) {
                    is TaskListMode.Select -> mode.selectedTasks.contains(task)
                    else -> task.completed
                },
                onCheckedChange = { checked ->
                    when (mode) {
                        is TaskListMode.Select -> {
                            val selectedTasksMut = mode.selectedTasks.toMutableSet()
                            when {
                                checked -> selectedTasksMut.add(task)
                                !checked -> selectedTasksMut.remove(task)
                            }
                            onSelectedTasksChange(selectedTasksMut)
                        }

                        else -> onTaskChange(task.copy(completed = checked))
                    }
                },
                onClick = {
                    when (mode) {
                        is TaskListMode.Tick -> onNavigateToEdit(task)
                        is TaskListMode.Select -> {
                            val selected = mode.selectedTasks.contains(task)
                            val selectedTasksMut = mode.selectedTasks.toMutableSet()
                            // Flip selected
                            when {
                                selected -> selectedTasksMut.remove(task)
                                !selected -> selectedTasksMut.add(task)
                            }
                            onSelectedTasksChange(selectedTasksMut)
                        }
                    }
                },
                onLongClick = {
                    when (mode) {
                        is TaskListMode.Tick -> onSelectedTasksChange(setOf(task))
                        is TaskListMode.Select -> {
                            val selected = mode.selectedTasks.contains(task)
                            val selectedTasksMut = mode.selectedTasks.toMutableSet()
                            when {
                                selected -> selectedTasksMut.remove(task)
                                !selected -> selectedTasksMut.add(task)
                            }
                            onSelectedTasksChange(selectedTasksMut)
                        }
                    }
                },
                mode = when (mode) {
                    is TaskListMode.Tick -> TaskItemMode.Tick
                    is TaskListMode.Select -> TaskItemMode.Select
                }
            )
        }
    }
}

@Preview
@Composable
private fun TaskListScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
    val words = lorem
        .replace(Regex("[^A-Za-z ]"), "")
        .split(" ")

    TodoComposeTheme {
        val tasks by remember {
            mutableStateOf(List(20) { index ->
                val titleOffset = (0..490).random()
                val titleLimit = (2..10).random()
                val title = words
                    .subList(titleOffset, titleOffset + titleLimit)
                    .joinToString(" ")
                val notesOffset = (0..50).random()
                val notesLimit = (0..450).random()
                val notes = words
                    .subList(notesOffset, notesOffset + notesLimit)
                    .joinToString(" ")

                Task(
                    id = index,
                    title = title,
                    notes = notes,
                    completed = false,
                )
            }.toPersistentList())
        }

        TaskListScaffold(
            uiState = UiState.Tick(tasks),
            onEditTask = {},
            onSelectedTasksChange = {},
            onRemoveTasks = {},
            onNavigateToCreate = {},
            onNavigateToEdit = {},
        )
    }
}

private sealed interface EditTaskMode {
    data class Create(
        val title: String,
        val notes: String,
    ) : EditTaskMode

    data class Edit(val task: Task) : EditTaskMode
}

// TODO Expand transition
// TODO Intercept system back
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskScaffold(
    modifier: Modifier = Modifier,
    mode: EditTaskMode,
    onCreateBufferChange: (title: String, notes: String) -> Unit,
    onCreateTask: (title: String, notes: String) -> Unit,
    onEditTask: (Task) -> Unit,
    onEditBufferChange: (Task) -> Unit,
    onDismiss: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.dismiss)
                        )
                    }
                },
                title = {
                    Text(
                        stringResource(
                            when (mode) {
                                is EditTaskMode.Create -> R.string.new_task
                                is EditTaskMode.Edit -> R.string.edit_task
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            when (mode) {
                                is EditTaskMode.Create -> {
                                    onCreateTask(mode.title, mode.notes)
                                }

                                is EditTaskMode.Edit -> {
                                    onEditTask(mode.task)
                                }
                            }
                            onDismiss()
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.confirm)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        EditTaskForm(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues),
            title = when (mode) {
                is EditTaskMode.Create -> mode.title
                is EditTaskMode.Edit -> mode.task.title
            },
            onTitleChange = {
                when (mode) {
                    is EditTaskMode.Create -> onCreateBufferChange(it, mode.notes)
                    is EditTaskMode.Edit -> onEditBufferChange(mode.task.copy(title = it))
                }
            },
            notes = when (mode) {
                is EditTaskMode.Create -> mode.notes
                is EditTaskMode.Edit -> mode.task.notes
            },
            onNotesChange = {
                when (mode) {
                    is EditTaskMode.Create -> onCreateBufferChange(mode.title, it)
                    is EditTaskMode.Edit -> onEditBufferChange(mode.task.copy(notes = it))
                }
            }
        )
    }
    // TODO Hacky
    BackHandler(onBack = onDismiss)
}

@Composable
private fun EditTaskForm(
    modifier: Modifier = Modifier,
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxHeight(),
    ) {
        val textFieldModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        item {
            TextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = textFieldModifier,
                label = { Text(stringResource(R.string.title)) },
            )
        }
        item {
            TextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = textFieldModifier,
                label = { Text(stringResource(R.string.notes)) },
                minLines = 6,
            )
        }
    }
}

@Preview
@Composable
private fun EditTaskScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
    val words = lorem
        .replace(Regex("[^A-Za-z ]"), "")
        .split(" ")
    val title = words.subList(0, 5).joinToString(" ")
    val notes = words.subList(5, 10).joinToString(" ")

    TodoComposeTheme {
        EditTaskScaffold(
            mode = EditTaskMode.Edit(Task(0, title, notes, false)),
            onCreateBufferChange = { _, _ -> },
            onCreateTask = { _, _ -> },
            onEditTask = {},
            onEditBufferChange = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwoPaneTaskListScaffold(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onEditTask: (Task) -> Unit,
    onSelectedTasksChange: (Set<Task>) -> Unit,
    onNavigateToEdit: (Task) -> Unit,
    onCreateBufferChange: (title: String, notes: String) -> Unit,
    onEditBufferChange: (Task) -> Unit,
    onDismiss: () -> Unit,
    onRemoveTasks: (Set<Task>) -> Unit,
    onTaskCreate: (title: String, notes: String) -> Unit,
    onNavigateToCreate: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            val tweenFloat = tween<Float>(100)
            val tweenIntSize = tween<IntSize>(100)
            val tweenIntOffset = tween<IntOffset>(100)

            TopAppBar(
                title = { Text(stringResource(R.string.tasks)) },
                navigationIcon = {
                    AnimatedVisibility(
                        when (uiState) {
                            is UiState.Tick -> false
                            else -> true
                        },
                        enter = slideInHorizontally(tweenIntOffset) { -it / 2 } +
                                expandHorizontally(tweenIntSize, Alignment.Start) +
                                fadeIn(tweenFloat),
                        exit = slideOutHorizontally(tweenIntOffset) { -it / 2 } +
                                shrinkHorizontally(tweenIntSize, Alignment.Start) +
                                fadeOut(tweenFloat),
                    ) {
                        IconButton(onClick = {
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, stringResource(R.string.dismiss))
                        }
                    }
                },
                actions = {
                    // Delete tasks
                    AnimatedVisibility(
                        when (uiState) {
                            is UiState.Select -> true
                            else -> false
                        },
                        enter = slideInHorizontally(tweenIntOffset) { it / 2 } +
                                expandHorizontally(tweenIntSize, Alignment.End) +
                                fadeIn(tweenFloat),
                        exit = slideOutHorizontally(tweenIntOffset) { it / 2 } +
                                shrinkHorizontally(tweenIntSize, Alignment.End) +
                                fadeOut(tweenFloat),
                    ) {
                        IconButton(onClick = {
                            when (uiState) {
                                is UiState.Select -> {
                                    onRemoveTasks(uiState.selectedTasks)
                                }

                                else -> {}
                            }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                stringResource(R.string.remove_tasks)
                            )
                        }
                    }
                    // Confirm
                    AnimatedVisibility(
                        when (uiState) {
                            is UiState.Edit -> true
                            is UiState.Create -> true
                            else -> false
                        }
                    ) {
                        IconButton(
                            onClick = {
                                when (uiState) {
                                    is UiState.Create -> {
                                        onTaskCreate(uiState.title, uiState.notes)
                                    }

                                    is UiState.Edit -> {
                                        onEditTask(uiState.task)
                                    }

                                    else -> {}
                                }
                            }
                        ) {
                            Icon(Icons.Default.Check, stringResource(R.string.new_task))
                        }
                    }
                },
            )
        }
    ) { paddingValues ->
        Row(modifier = Modifier.padding(paddingValues)) {
            NavigationRail(header = {
                FloatingActionButton(
                    onClick = {
                        onNavigateToCreate()
                    }
                ) {
                    Icon(Icons.Default.Add, stringResource(R.string.new_task))
                }
            }) {
                // NavRail items
            }
            // TODO Highlight editing task
            TaskList(
                modifier = Modifier.weight(1f),
                tasks = uiState.tasks,
                mode = when (uiState) {
                    is UiState.Select -> TaskListMode.Select(uiState.selectedTasks)
                    else -> TaskListMode.Tick
                },
                onTaskChange = onEditTask,
                onSelectedTasksChange = onSelectedTasksChange,
                onNavigateToEdit = onNavigateToEdit,
            )
            AnimatedContent(
                uiState,
                modifier = Modifier.weight(1f),
                contentKey = { it::class },
                label = "second pane crossfade"
            ) { state ->
                when (state) {
                    is UiState.Tick -> {}

                    is UiState.Select -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                stringResource(R.string.task_selected, state.selectedTasks.size),
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }

                    is UiState.Edit -> {
                        EditTaskForm(
                            title = state.task.title,
                            onTitleChange = { onEditBufferChange(state.task.copy(title = it)) },
                            notes = state.task.notes,
                            onNotesChange = { onEditBufferChange(state.task.copy(notes = it)) }
                        )
                    }

                    is UiState.Create -> {
                        EditTaskForm(
                            title = state.title,
                            onTitleChange = { onCreateBufferChange(it, state.notes) },
                            notes = state.notes,
                            onNotesChange = { onCreateBufferChange(state.title, it) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.TABLET)
@Composable
private fun TwoPaneTaskListScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
    val words = lorem
        .replace(Regex("[^A-Za-z ]"), "")
        .split(" ")

    val tasks = List(20) { index ->
        val titleOffset = (0..490).random()
        val titleLimit = (2..10).random()
        val title = words
            .subList(titleOffset, titleOffset + titleLimit)
            .joinToString(" ")
        val notesOffset = (0..50).random()
        val notesLimit = (0..450).random()
        val notes = words
            .subList(notesOffset, notesOffset + notesLimit)
            .joinToString(" ")

        Task(
            id = index,
            title = title,
            notes = notes,
            completed = false,
        )
    }.toPersistentList()

    TodoComposeTheme {
        TwoPaneTaskListScaffold(
            uiState = UiState.Edit(tasks, tasks.first()),
            onEditTask = {},
            onSelectedTasksChange = {},
            onNavigateToEdit = {},
            onCreateBufferChange = { _, _ -> },
            onEditBufferChange = {},
            onDismiss = {},
            onRemoveTasks = {},
            onTaskCreate = { _, _ -> },
            onNavigateToCreate = {}
        )
    }
}
