package io.github.satwanjyu.todocompose.tasks

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.satwanjyu.todocompose.R
import io.github.satwanjyu.todocompose.ui.theme.TodoComposeTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay


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

private fun uiStateType(uiState: UiState) = when (uiState) {
    is UiState.Tick, is UiState.Select -> 0
    is UiState.Create -> 1
    is UiState.Edit -> 2
}

@Composable
private fun TasksCompact(
    viewModel: TasksViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            if (uiStateType(initialState) == 0 && uiStateType(targetState) == 0) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            }
        },
        contentKey = { uiStateType(it) },
        label = "Cross-fade between task list modes"
    ) { state ->
        when (state) {
            is UiState.Tick, is UiState.Select -> TaskListScaffold(
                uiState = state,
                onTick = viewModel::insertTask,
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
                onQueryChange = { query ->
                    val tick = state as UiState.Tick
                    viewModel.uiState.value = tick.copy(query = query)
                    if (query != null) {
                        viewModel.search(query)
                    }
                }
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
private fun TasksMedium(viewModel: TasksViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NavRailScaffold(
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
        },
        onQueryChange = { query ->
            val tick = uiState as UiState.Tick
            viewModel.uiState.value = tick.copy(query = query)
            if (query != null) {
                viewModel.search(query)
            }
        }
    )
}

@Composable
private fun TasksExpanded(viewModel: TasksViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NavRailTwoPaneScaffold(
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
        },
        onQueryChange = { query ->
            val tick = uiState as UiState.Tick
            viewModel.uiState.value = tick.copy(query = query)
            if (query != null) {
                viewModel.search(query)
            }
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
    uiState: UiState,
    onTick: (Task) -> Unit,
    onSelectedTasksChange: (Set<Task>) -> Unit,
    onRemoveTasks: (Set<Task>) -> Unit,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksSearchBar(
    modifier: Modifier = Modifier,
    docked: Boolean,
    query: String?,
    onQueryChange: (String?) -> Unit,
    enabled: Boolean,
    queriedTasks: ImmutableList<Task>,
    onNavigateToEdit: (Task) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val onSearch = { keyboardController?.hide() }
    val active = query != null
    val onActiveChange = { newActive: Boolean ->
        if (newActive) {
            onQueryChange("")
        } else {
            onQueryChange(null)
        }
    }
    val leadingIcon = @Composable {
        if (query != null) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(stringResource(R.string.go_back))
                    }
                },
                state = rememberTooltipState()
            ) {
                IconButton(
                    onClick = {
                        onQueryChange(null)
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.go_back)
                    )
                }
            }
        } else {
            Icon(Icons.Default.Search, stringResource(R.string.search))
        }
    }
    val trailingIcon = @Composable {
        AnimatedVisibility(
            visible = !query.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(stringResource(R.string.clear))
                    }
                },
                state = rememberTooltipState()
            ) {
                IconButton(
                    onClick = { onQueryChange("") }
                ) {
                    Icon(
                        Icons.Default.Close,
                        stringResource(R.string.clear)
                    )
                }
            }
        }
    }
    val placeholder = @Composable { Text(stringResource(R.string.search_tasks)) }
    val content: @Composable ColumnScope.() -> Unit = {
        LaunchedEffect(query) {
            delay(500)
            onQueryChange(query)
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(queriedTasks) { task ->
                ListItem(
                    headlineContent = { Text(task.title) },
                    modifier = Modifier.clickable {
                        onNavigateToEdit(task)
                    }
                )
            }
        }
    }

    if (!docked) {
        SearchBar(
            modifier = modifier,
            query = query ?: "",
            onQueryChange = onQueryChange,
            onSearch = { onSearch() },
            active = active,
            onActiveChange = onActiveChange,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            placeholder = placeholder,
            enabled = enabled,
            content = content
        )
    } else {
        DockedSearchBar(
            modifier = modifier,
            query = query ?: "",
            onQueryChange = onQueryChange,
            onSearch = { onSearch() },
            active = active,
            onActiveChange = onActiveChange,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            placeholder = placeholder,
            enabled = enabled,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksSelectAppBar(
    selectedTasks: ImmutableSet<Task>,
    onSelectedTasksChange: (Set<Task>) -> Unit,
    onRemoveTasks: (Set<Task>) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(
                    R.string.task_selected,
                    selectedTasks.size
                )
            )
        },
        navigationIcon = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(stringResource(R.string.dismiss))
                    }
                },
                state = rememberTooltipState()
            ) {
                IconButton(
                    onClick = {
                        onSelectedTasksChange(emptySet())
                    }) {
                    Icon(Icons.Default.Close, stringResource(R.string.dismiss))
                }
            }
        },
        actions = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text(stringResource(R.string.remove_tasks)) },
                state = rememberTooltipState()
            ) {
                IconButton(
                    onClick = {
                        onRemoveTasks(selectedTasks)
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
}

private sealed interface TaskListMode {
    data object Tick : TaskListMode
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
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
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
                            val currentlySelected = mode.selectedTasks.contains(task)
                            val selectedTasksMut = mode.selectedTasks.toMutableSet()
                            when {
                                currentlySelected -> selectedTasksMut.remove(task)
                                !currentlySelected -> selectedTasksMut.add(task)
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
            onTick = {},
            onSelectedTasksChange = {},
            onRemoveTasks = {},
            onNavigateToCreate = {},
            onNavigateToEdit = {},
            onQueryChange = {},
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { Text(stringResource(R.string.dismiss)) },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = onDismiss
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.dismiss)
                            )
                        }
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
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { Text(stringResource(R.string.confirm)) },
                        state = rememberTooltipState()
                    ) {
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
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
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
    contentPadding: PaddingValues = PaddingValues(),
) {
    var titleValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(title))
    }
    var notesValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(notes))
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxHeight(),
        contentPadding = contentPadding,
    ) {
        val textFieldModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)

        item {
            TextField(
                value = titleValue,
                onValueChange = {
                    titleValue = it
                    onTitleChange(it.text)
                },
                modifier = textFieldModifier,
                label = { Text(stringResource(R.string.title)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        defaultKeyboardAction(ImeAction.Next)
                        notesValue = notesValue.copy(
                            selection = TextRange(notesValue.text.length)
                        )
                    }
                )
            )
        }
        item {
            TextField(
                value = notesValue,
                onValueChange = {
                    notesValue = it
                    onNotesChange(it.text)
                },
                modifier = textFieldModifier,
                label = { Text(stringResource(R.string.notes)) },
                minLines = 6,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        defaultKeyboardAction(ImeAction.Done)
                    }
                ),
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

// TODO Remodel UiState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavRailTwoPaneScaffold(
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
    onQueryChange: (String?) -> Unit,
) {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
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

        Row(modifier = Modifier.padding(paddingValues)) {
            NavigationRail(header = {
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
            }) {
                // NavRail items
            }
            // TODO Highlight editing task
            Box(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(nestedScrollConnection)
            ) {
                TaskList(
                    tasks = uiState.tasks,
                    mode = when (uiState) {
                        is UiState.Select -> TaskListMode.Select(uiState.selectedTasks)
                        else -> TaskListMode.Tick
                    },
                    onTaskChange = onEditTask,
                    onSelectedTasksChange = onSelectedTasksChange,
                    onNavigateToEdit = onNavigateToEdit,
                    contentPadding = PaddingValues(
                        top = when (uiState) {
                            is UiState.Tick -> 68.dp
                            else -> 0.dp
                        }
                    ),
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState is UiState.Tick && searchBarVisible,
                    enter = slideInVertically { -it },
                    exit = slideOutVertically { -it },
                ) {
                    if (uiState is UiState.Tick) {
                        TasksSearchBar(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp),
                            docked = true,
                            query = uiState.query,
                            onQueryChange = onQueryChange,
                            enabled = uiState.tasks.isNotEmpty(),
                            queriedTasks = uiState.queriedTasks,
                            onNavigateToEdit = onNavigateToEdit,
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TasksSearchBar(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(vertical = 8.dp),
                                docked = true,
                                query = null,
                                onQueryChange = {},
                                enabled = false,
                                queriedTasks = persistentListOf(),
                                onNavigateToEdit = {},
                            )
                        }
                    }
                }
            }
            AnimatedContent(
                uiState,
                modifier = Modifier.weight(1f),
                contentKey = { it::class },
                label = "second pane animated content"
            ) { uiState ->
                Column {
                    when (uiState) {
                        is UiState.Tick -> {}
                        is UiState.Select -> {
                            TasksSelectAppBar(
                                selectedTasks = uiState.selectedTasks,
                                onSelectedTasksChange = onSelectedTasksChange,
                                onRemoveTasks = onRemoveTasks,
                            )
                            Box(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    stringResource(
                                        R.string.task_selected,
                                        uiState.selectedTasks.size
                                    ),
                                    modifier = Modifier.align(Alignment.Center),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }

                        is UiState.Create -> {
                            EditTaskAppBar(
                                onDismiss = onDismiss,
                                onTaskCreate = { onTaskCreate(uiState.title, uiState.notes) }
                            )
                            EditTaskForm(
                                title = uiState.title,
                                onTitleChange = { onCreateBufferChange(it, uiState.notes) },
                                notes = uiState.notes,
                                onNotesChange = { onCreateBufferChange(uiState.title, it) }
                            )
                        }

                        is UiState.Edit -> {
                            CreateTaskAppBar(
                                onDismiss = onDismiss,
                                onEditTask = { onEditTask(uiState.task) }
                            )
                            EditTaskForm(
                                title = uiState.task.title,
                                onTitleChange = { onEditBufferChange(uiState.task.copy(title = it)) },
                                notes = uiState.task.notes,
                                onNotesChange = { onEditBufferChange(uiState.task.copy(notes = it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.TABLET)
@Composable
private fun NavRailTwoPaneScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
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
        NavRailTwoPaneScaffold(
            uiState = UiState.Edit(tasks, tasks.first()),
            onEditTask = {},
            onSelectedTasksChange = {},
            onNavigateToEdit = {},
            onCreateBufferChange = { _, _ -> },
            onEditBufferChange = {},
            onDismiss = {},
            onRemoveTasks = {},
            onTaskCreate = { _, _ -> },
            onNavigateToCreate = {},
            onQueryChange = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskAppBar(
    onDismiss: () -> Unit,
    onTaskCreate: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.new_task)) },
        navigationIcon = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text(stringResource(R.string.dismiss)) },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.dismiss)
                    )
                }
            }
        },
        actions = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text(stringResource(R.string.confirm)) },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = {
                    onTaskCreate()
                }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.confirm)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskAppBar(
    onDismiss: () -> Unit,
    onEditTask: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.edit_task)) },
        navigationIcon = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text(stringResource(R.string.dismiss)) },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.dismiss)
                    )
                }
            }
        },
        actions = {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text(stringResource(R.string.confirm)) },
                state = rememberTooltipState()
            ) {
                IconButton(onClick = {
                    onEditTask()
                }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.confirm)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavRailScaffold(
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
    onQueryChange: (String?) -> Unit,
) {
    Scaffold(
        modifier = modifier,
    ) { paddingValues ->
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
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { Text(stringResource(R.string.new_task)) },
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
            }) {
                // NavRail items
            }
            Box(
                modifier = Modifier.nestedScroll(nestedScrollConnection)
            ) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        if (uiStateType(uiState) == 0) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                    scaleIn(
                                        initialScale = 0.92f,
                                        animationSpec = tween(220, delayMillis = 90)
                                    ))
                                .togetherWith(fadeOut(animationSpec = tween(90)))
                        }
                    },
                    contentKey = { uiStateType(it) },
                    label = "animated content"
                ) { uiState ->
                    val contentPaddingTop = (64 + 8).dp

                    when (uiState) {
                        is UiState.Tick, is UiState.Select -> {
                            TaskList(
                                tasks = uiState.tasks,
                                mode = when (uiState) {
                                    is UiState.Select -> TaskListMode.Select(uiState.selectedTasks)
                                    else -> TaskListMode.Tick
                                },
                                onTaskChange = onEditTask,
                                onSelectedTasksChange = onSelectedTasksChange,
                                onNavigateToEdit = onNavigateToEdit,
                                contentPadding = PaddingValues(top = contentPaddingTop),
                            )
                        }

                        is UiState.Create -> {
                            EditTaskForm(
                                title = uiState.title,
                                onTitleChange = { onCreateBufferChange(it, uiState.notes) },
                                notes = uiState.notes,
                                onNotesChange = { onCreateBufferChange(uiState.title, it) },
                                contentPadding = PaddingValues(top = contentPaddingTop),
                            )
                        }

                        is UiState.Edit -> {
                            EditTaskForm(
                                title = uiState.task.title,
                                onTitleChange = { onEditBufferChange(uiState.task.copy(title = it)) },
                                notes = uiState.task.notes,
                                onNotesChange = { onEditBufferChange(uiState.task.copy(notes = it)) },
                                contentPadding = PaddingValues(top = contentPaddingTop),
                            )
                        }
                    }
                }
                AnimatedContent(
                    targetState = uiState,
                    modifier = Modifier.fillMaxWidth(),
                    contentKey = { it::class },
                    label = "top bar animated state"
                ) { uiState ->
                    when (uiState) {
                        is UiState.Tick -> {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = searchBarVisible,
                                enter = slideInVertically { -it },
                                exit = slideOutVertically { -it },
                            ) {
                                TasksSearchBar(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(8.dp),
                                    docked = true,
                                    query = uiState.query,
                                    onQueryChange = onQueryChange,
                                    enabled = uiState.tasks.isNotEmpty(),
                                    queriedTasks = uiState.queriedTasks,
                                    onNavigateToEdit = {},
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

                        is UiState.Create -> {
                            EditTaskAppBar(
                                onDismiss = onDismiss,
                                onTaskCreate = { onTaskCreate(uiState.title, uiState.notes) }
                            )
                        }

                        is UiState.Edit -> {
                            CreateTaskAppBar(
                                onDismiss = onDismiss,
                                onEditTask = { onEditTask(uiState.task) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = Devices.FOLDABLE)
@Composable
private fun NavRailScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
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
        NavRailScaffold(
            uiState = UiState.Tick(tasks),
            onEditTask = {},
            onSelectedTasksChange = {},
            onNavigateToEdit = {},
            onCreateBufferChange = { _, _ -> },
            onEditBufferChange = {},
            onDismiss = {},
            onRemoveTasks = {},
            onTaskCreate = { _, _ -> },
            onNavigateToCreate = {},
            onQueryChange = {},
        )
    }
}
