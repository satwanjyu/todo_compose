package io.github.satwanjyu.todocompose.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.satwanjyu.todocompose.R
import io.github.satwanjyu.todocompose.ui.theme.TodoComposeTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun NavGraphBuilder.tasksScreen(
    onNavigateToNewTask: () -> Unit,
    onNavigateToEditTask: (taskId: Int) -> Unit,
    onPop: () -> Unit,
) {
    composable("task-list") {
        TaskListScreen(
            onNavigateToEditTask = { task ->
                onNavigateToEditTask(task.id)
            },
            onNavigateToNewTask = onNavigateToNewTask,
        )
    }
    composable("new-task") {
        EditTaskScreen(
            mode = EditTaskScreenMode.NewTask,
            onPop = onPop,
        )
    }
    composable(
        "edit-task?taskId={id}",
        arguments = listOf(
            navArgument("id") { type = NavType.IntType },
        )
    ) { entry ->
        val id = entry.arguments!!.getInt("id")

        EditTaskScreen(
            mode = EditTaskScreenMode.EditTask(id),
            onPop = onPop,
        )
    }
}

// TODO Encapsulate TaskItem UI behaviour
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    modifier: Modifier = Modifier,
    title: String,
    notes: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    containerColor: Color,
    textStyle: TextStyle,
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = textStyle,
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
        colors = ListItemDefaults.colors(
            containerColor = containerColor
        )
    )
}

@Preview
@Composable
fun TaskItemPreview(
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
            checked = false,
            onCheckedChange = {},
            onClick = {},
            onLongClick = {},
            containerColor = Color.Transparent,
            textStyle = LocalTextStyle.current
        )
    }
}

@Preview
@Composable
fun TaskItemPreviewTicked(
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
            onCheckedChange = {},
            onClick = {},
            onLongClick = {},
            containerColor = Color.Transparent,
            textStyle = LocalTextStyle.current.copy(
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

sealed class TaskListMode {
    data class Tick(
        val onTaskChange: suspend (Task) -> Unit,
        val onNavigateToEditTask: (Task) -> Unit,
        val onSelectTask: suspend (Task) -> Unit,
    ) : TaskListMode()

    data class Select(
        val selectedTasks: ImmutableList<Task>,
        val onSelectedTasksChange: (List<Task>) -> Unit,
        val onRemoveTasks: suspend (List<Task>) -> Unit,
    ) : TaskListMode()
}

@Composable
fun TaskListScreen(
    viewModel: TasksViewModel = viewModel(),
    onNavigateToNewTask: () -> Unit,
    onNavigateToEditTask: (task: Task) -> Unit,
) {
    val tasks by viewModel.tasks.collectAsState()
    val selectedTasks by viewModel.selectedTasks.collectAsState()

    TaskListScaffold(
        mode = if (selectedTasks.isEmpty()) {
            TaskListMode.Tick(
                onTaskChange = { viewModel.editTask(it) },
                onNavigateToEditTask = { onNavigateToEditTask(it) },
                onSelectTask = { task -> viewModel.selectedTasks.update { persistentListOf(task) } }
            )
        } else {
            TaskListMode.Select(
                selectedTasks = selectedTasks,
                onSelectedTasksChange = { list -> viewModel.selectedTasks.update { list.toPersistentList() } },
                onRemoveTasks = { viewModel.removeTasks(it) }
            )
        },
        tasks = tasks,
        onNavigateToNewTask = onNavigateToNewTask,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScaffold(
    modifier: Modifier = Modifier,
    mode: TaskListMode,
    tasks: ImmutableList<Task>,
    onNavigateToNewTask: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

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
                        mode,
                        animationSpec = tweenFloat,
                        label = "title crossfade"
                    ) { mode ->
                        Text(
                            when (mode) {
                                is TaskListMode.Tick -> stringResource(R.string.tasks)
                                is TaskListMode.Select -> stringResource(
                                    R.string.task_selected,
                                    mode.selectedTasks.size
                                )
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    val visible = when (mode) {
                        is TaskListMode.Tick -> false
                        is TaskListMode.Select -> true
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
                            when (mode) {
                                is TaskListMode.Tick -> {}
                                is TaskListMode.Select -> mode.onSelectedTasksChange(emptyList())
                            }
                        }) {
                            Icon(Icons.Default.Close, stringResource(R.string.dismiss))
                        }
                    }
                },
                actions = {
                    val visible = when (mode) {
                        is TaskListMode.Tick -> false
                        is TaskListMode.Select -> true
                    }
                    AnimatedVisibility(
                        visible,
                        enter = slideInHorizontally(tweenIntOffset) { it / 2 } +
                                expandHorizontally(tweenIntSize, Alignment.End) +
                                fadeIn(tweenFloat),
                        exit = slideOutHorizontally(tweenIntOffset) { it / 2 } +
                                shrinkHorizontally(tweenIntSize, Alignment.End) +
                                fadeOut(tweenFloat),
                    ) {
                        IconButton(onClick = {
                            when (mode) {
                                is TaskListMode.Tick -> {}
                                is TaskListMode.Select -> {
                                    scope.launch(Dispatchers.IO) {
                                        mode.onRemoveTasks(mode.selectedTasks)
                                    }
                                }
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
                onClick = onNavigateToNewTask,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_task)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            items(tasks, key = { it.id }) { task ->
                // TODO Ugly as fuck closures
                val selected = { mode: TaskListMode.Select ->
                    mode.selectedTasks.contains(task)
                }
                val setSelected = { mode: TaskListMode.Select, newSelected: Boolean ->
                    with(mode) {
                        val result = if (newSelected) {
                            selectedTasks.toPersistentList().add(task)
                        } else {
                            selectedTasks.toPersistentList().remove(task)
                        }
                        onSelectedTasksChange(result)
                    }
                }

                TaskItem(
                    title = task.title,
                    notes = task.notes,
                    checked = when (mode) {
                        is TaskListMode.Tick -> task.completed
                        is TaskListMode.Select -> selected(mode)
                    },
                    onCheckedChange = { checked ->
                        when (mode) {
                            is TaskListMode.Tick -> scope.launch(Dispatchers.IO) {
                                mode.onTaskChange(task.copy(completed = checked))
                            }

                            is TaskListMode.Select -> setSelected(mode, checked)
                        }
                    },
                    onClick = {
                        when (mode) {
                            is TaskListMode.Tick -> mode.onNavigateToEditTask(task)
                            is TaskListMode.Select -> setSelected(mode, !selected(mode))
                        }
                    },
                    onLongClick = {
                        when (mode) {
                            is TaskListMode.Tick -> scope.launch(Dispatchers.IO) {
                                mode.onSelectTask(task)
                            }

                            is TaskListMode.Select -> setSelected(mode, !selected(mode))
                        }
                    },
                    containerColor = when (mode) {
                        is TaskListMode.Tick -> Color.Transparent
                        is TaskListMode.Select -> if (selected(mode)) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    },
                    textStyle = when (mode) {
                        is TaskListMode.Tick -> if (task.completed) {
                            LocalTextStyle.current.copy(
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        } else {
                            LocalTextStyle.current
                        }

                        is TaskListMode.Select -> if (selected(mode)) {
                            LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            LocalTextStyle.current
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
fun TaskListScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
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
            mode = TaskListMode.Tick(
                onTaskChange = {},
                onNavigateToEditTask = {},
                onSelectTask = {},
            ),
            tasks = tasks,
            onNavigateToNewTask = {},
        )
    }
}

sealed class EditTaskScreenMode {
    object NewTask : EditTaskScreenMode()
    data class EditTask(val taskId: Int) : EditTaskScreenMode()
}

// TODO Expand transition
@Composable
fun EditTaskScreen(
    viewModel: TasksViewModel = viewModel(),
    mode: EditTaskScreenMode,
    onPop: () -> Unit,
) {
    EditTaskScaffold(
        mode = when (mode) {
            EditTaskScreenMode.NewTask -> EditTaskMode.NewTask(viewModel::addTask)
            is EditTaskScreenMode.EditTask -> EditTaskMode.EditTask(
                getTask = { viewModel.getTask(mode.taskId) },
                editTask = viewModel::editTask,
            )
        },
        onPop = onPop,
    )
}

sealed class EditTaskMode {
    data class NewTask(
        val addTask: suspend (title: String, notes: String) -> Unit,
    ) : EditTaskMode()

    data class EditTask(
        val getTask: suspend () -> Task,
        val editTask: suspend (Task) -> Unit,
    ) : EditTaskMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScaffold(
    modifier: Modifier = Modifier,
    mode: EditTaskMode,
    onPop: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var originalTask by rememberSaveable { mutableStateOf<Task?>(null) }

    val scope = rememberCoroutineScope()

    when (mode) {
        is EditTaskMode.NewTask -> {}
        is EditTaskMode.EditTask -> {
            LaunchedEffect(mode) {
                val task = mode.getTask()
                title = task.title
                notes = task.notes
                originalTask = task
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onPop
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
                                is EditTaskMode.NewTask -> R.string.new_task
                                is EditTaskMode.EditTask -> R.string.edit_task
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                when (mode) {
                                    is EditTaskMode.NewTask -> {
                                        mode.addTask(title, notes)
                                    }

                                    is EditTaskMode.EditTask -> {
                                        mode.editTask(
                                            originalTask!!.copy(
                                                title = title,
                                                notes = notes,
                                            )
                                        )
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    onPop()
                                }
                            }
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
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues)
        ) {
            val textFieldModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
            item {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = textFieldModifier,
                    label = { Text(stringResource(R.string.title)) },
                )
            }
            item {
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = textFieldModifier,
                    label = { Text(stringResource(R.string.notes)) },
                    minLines = 6,
                )
            }
        }
    }
}

@Preview
@Composable
fun EditTaskScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
    val words = lorem
        .replace(Regex("[^A-Za-z ]"), "")
        .split(" ")
    val title = words.subList(0, 5).joinToString(" ")
    val notes = words.subList(5, 10).joinToString(" ")

    TodoComposeTheme {
        EditTaskScaffold(
            mode = EditTaskMode.EditTask(
                getTask = { Task(0, title, notes, false) },
                editTask = {},
            ),
            onPop = {}
        )
    }
}
