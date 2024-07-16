package io.github.satwanjyu.todocompose.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import io.github.satwanjyu.todocompose.R
import io.github.satwanjyu.todocompose.ui.theme.TodoComposeTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.delay

enum class FabStyle { Standard, Extended }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskFab(
    modifier: Modifier = Modifier,
    style: FabStyle,
    onClick: () -> Unit,
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
        when (style) {
            FabStyle.Standard -> {
                FloatingActionButton(onClick = onClick, modifier = modifier) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.new_task)
                    )
                }
            }

            FabStyle.Extended -> {
                ExtendedFloatingActionButton(onClick = onClick, modifier = modifier) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.new_task)
                    )
                    Text(
                        stringResource(R.string.new_task),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun NewTaskFabStandardPreview() {
    NewTaskFab(style = FabStyle.Standard, onClick = {})
}

@Preview
@Composable
fun NewTaskFabExtendedPreview() {
    NewTaskFab(style = FabStyle.Extended, onClick = {})
}

enum class TaskItemMode { Tick, Select }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TaskItem(
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
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
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
        supportingContent = {
            Text(
                notes,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = LocalTextStyle.current.let { style ->
                    if (mode == TaskItemMode.Tick && checked) {
                        style.copy(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    } else {
                        style
                    }
                }
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
private fun TaskItemTickedPreview(
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
internal fun TasksSearchBar(
    modifier: Modifier = Modifier,
    docked: Boolean,
    query: String?,
    onQueryChange: (String?) -> Unit,
    queriedTasks: ImmutableList<Task>,
    onTaskClick: (Task) -> Unit,
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
                        onTaskClick(task)
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
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TasksSelectAppBar(
    selectedTasks: ImmutableSet<Task>,
    onSelectedTasksChange: (ImmutableSet<Task>) -> Unit,
    onRemoveTasks: (ImmutableSet<Task>) -> Unit,
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
                        Text(stringResource(R.string.clear_selection))
                    }
                },
                state = rememberTooltipState()
            ) {
                IconButton(
                    onClick = {
                        onSelectedTasksChange(persistentSetOf())
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

/**
 * List of tasks that support multi-selection when a task item is held.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskList(
    modifier: Modifier = Modifier,
    tasks: ImmutableList<Task>,
    onTaskChange: (Task) -> Unit,
    selectedTasks: ImmutableSet<Task>,
    onSelectedTasksChange: (ImmutableSet<Task>) -> Unit,
    onRemoveTasks: (ImmutableSet<Task>) -> Unit,
    onEditTask: (Task) -> Unit,
    query: String?,
    onQueryChange: (String?) -> Unit,
    queriedTasks: ImmutableList<Task>,
    onQueriedTaskClick: (Task) -> Unit,
) {
    Box {
        val selecting = selectedTasks.isNotEmpty()
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 64.dp),
        ) {
            items(tasks, key = { it.id }) { task ->
                fun flipSelection(task: Task, selectedTasks: ImmutableSet<Task>) {
                    val newSelection = selectedTasks
                        .toPersistentSet()
                        .mutate { selection ->
                            if (selection.contains(task)) {
                                selection.remove(task)
                            } else {
                                selection.add(task)
                            }
                        }
                    onSelectedTasksChange(newSelection)
                }
                TaskItem(
                    title = task.title,
                    notes = task.notes,
                    checked = if (!selecting) {
                        task.completed
                    } else {
                        selectedTasks.contains(task)
                    },
                    onCheckedChange = { checked ->
                        onTaskChange(task.copy(completed = checked))
                    },
                    onClick = {
                        if (!selecting) {
                            onEditTask(task)
                        } else {
                            flipSelection(task, selectedTasks)
                        }
                    },
                    onLongClick = {
                        if (!selecting) {
                            onSelectedTasksChange(persistentSetOf(task))
                        } else {
                            flipSelection(task, selectedTasks)
                        }
                    },
                    mode = if (!selecting) {
                        TaskItemMode.Tick
                    } else {
                        TaskItemMode.Select
                    }
                )
            }
        }
        AnimatedContent(
            modifier = Modifier.align(Alignment.TopCenter),
            targetState = selecting,
            label = "top bar cross-fade between searchbar and select status bar"
        ) { selectingInner ->
            if (!selectingInner) {
                TasksSearchBar(
                    docked = false,
                    query = query,
                    onQueryChange = onQueryChange,
                    queriedTasks = queriedTasks,
                    onTaskClick = onQueriedTaskClick,
                )
            } else {
                TasksSelectAppBar(
                    selectedTasks = selectedTasks,
                    onSelectedTasksChange = onSelectedTasksChange,
                    onRemoveTasks = onRemoveTasks,
                )
            }
        }
    }
}

//@Preview(device = Devices.TABLET)
//@Composable
//private fun NavRailTwoPaneScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
//    val words = lorem
//        .replace(Regex("[^A-Za-z ]"), "")
//        .split(" ")
//
//    val tasks = List(20) { index ->
//        val titleOffset = (0..490).random()
//        val titleLimit = (2..10).random()
//        val title = words
//            .subList(titleOffset, titleOffset + titleLimit)
//            .joinToString(" ")
//        val notesOffset = (0..50).random()
//        val notesLimit = (0..450).random()
//        val notes = words
//            .subList(notesOffset, notesOffset + notesLimit)
//            .joinToString(" ")
//
//        Task(
//            id = index,
//            title = title,
//            notes = notes,
//            completed = false,
//        )
//    }.toPersistentList()
//
//    TodoComposeTheme {
//        NavRailTwoPaneScaffold(
//            uiState = UiState.Edit,
//            onEditTask = {},
//            onSelectedTasksChange = {},
//            onNavigateToEdit = {},
//            onCreateBufferChange = { _, _ -> },
//            onEditBufferChange = {},
//            onDismiss = {},
//            onRemoveTasks = {},
//            onTaskCreate = { _, _ -> },
//            onNavigateToCreate = {},
//            onQueryChange = {},
//        )
//    }
//}

//@Preview(device = Devices.FOLDABLE)
//@Composable
//private fun NavRailScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
//    val words = lorem
//        .replace(Regex("[^A-Za-z ]"), "")
//        .split(" ")
//
//    val tasks = List(20) { index ->
//        val titleOffset = (0..490).random()
//        val titleLimit = (2..10).random()
//        val title = words
//            .subList(titleOffset, titleOffset + titleLimit)
//            .joinToString(" ")
//        val notesOffset = (0..50).random()
//        val notesLimit = (0..450).random()
//        val notes = words
//            .subList(notesOffset, notesOffset + notesLimit)
//            .joinToString(" ")
//
//        Task(
//            id = index,
//            title = title,
//            notes = notes,
//            completed = false,
//        )
//    }.toPersistentList()
//
//    TodoComposeTheme {
//        NavRailScaffold(
//            uiState = UiState.Tick(tasks),
//            onEditTask = {},
//            onSelectedTasksChange = {},
//            onNavigateToEdit = {},
//            onCreateBufferChange = { _, _ -> },
//            onEditBufferChange = {},
//            onDismiss = {},
//            onRemoveTasks = {},
//            onTaskCreate = { _, _ -> },
//            onNavigateToCreate = {},
//            onQueryChange = {},
//        )
//    }
//}
internal sealed interface EditTaskAction {
    data object Create : EditTaskAction
    data class Edit(val task: Task) : EditTaskAction
}

// TODO Expand transition
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTaskScaffold(
    modifier: Modifier = Modifier,
    action: EditTaskAction,
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onTaskSave: (String, String) -> Unit,
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
                            when (action) {
                                is EditTaskAction.Create -> R.string.new_task
                                is EditTaskAction.Edit -> R.string.edit_task
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
                                onTaskSave(title, notes)
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
            title = title,
            onTitleChange = onTitleChange,
            notes = notes,
            onNotesChange = onNotesChange,
        )
    }
    // FIXME hack to intercept back gesture as dialog dismiss
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
                value = title,
                onValueChange = onTitleChange,
                modifier = textFieldModifier,
                label = { Text(stringResource(R.string.title)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        // TODO focus on notes
                    }
                )
            )
        }
        item {
            TextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = textFieldModifier,
                label = { Text(stringResource(R.string.notes)) },
                minLines = 6,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // TODO submit
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
    TodoComposeTheme {
        EditTaskScaffold(
            action = EditTaskAction.Create,
            title = "",
            onTitleChange = {},
            notes = "",
            onNotesChange = {},
            onTaskSave = { _, _ -> },
            onDismiss = {}
        )
    }
}

