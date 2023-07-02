package io.github.satwanjyu.todocompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import io.github.satwanjyu.todocompose.ui.theme.TodoComposeTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    title: String,
    notes: String,
    completed: Boolean,
    onCompletedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(title, maxLines = 2)
        },
        supportingContent = {
            Text(notes, maxLines = 3)
        },
        leadingContent = {
            Checkbox(
                checked = completed,
                onCheckedChange = onCompletedChange
            )
        },
        modifier = modifier,
    )
}

@Preview
@Composable
fun TaskItemPreview(
    @PreviewParameter(provider = LoremIpsum::class, limit = 10) lorem: String,
) {
    TodoComposeTheme {
        val words = lorem
            .replace(Regex("[^A-Za-z ]"), "")
            .split(" ")
        val title = words.subList(0, 5).joinToString(" ")
        val notes = words.subList(5, 10).joinToString(" ")
        TaskItem(
            title = title,
            notes = notes,
            completed = false,
            onCompletedChange = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    tasks: ImmutableList<Task>,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.tasks),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            // TODO Supply key
            items(tasks) { task ->
                TaskItem(
                    title = task.title,
                    notes = task.notes,
                    completed = task.completed,
                    onCompletedChange = {}
                )
            }
        }
    }
}

data class Task(
    val title: String,
    val notes: String,
    val completed: Boolean,
)

@Preview
@Composable
fun TaskListScreenPreview() {
    TodoComposeTheme {
        val tasks by remember {
            mutableStateOf(List(20) {
                Task(
                    title = "Lorem",
                    notes = "Ipsum",
                    completed = false,
                )
            }.toPersistentList())
        }

        TaskListScreen(tasks = tasks)
    }
}

// TODO Expand transition
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    task: Task,
    onConfirm: (task: Task) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes) }

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
                        stringResource(R.string.edit_task),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            onConfirm(task.copy(title = title, notes = notes))
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
fun EditTaskScreenPreview(
    @PreviewParameter(
        provider = LoremIpsum::class,
        limit = 10
    ) lorem: String
) {
    TodoComposeTheme {
        val words = lorem
            .replace(Regex("[^A-Za-z ]"), "")
            .split(" ")
        val title = words.subList(0, 5).joinToString(" ")
        val notes = words.subList(5, 10).joinToString(" ")
        EditTaskScreen(task = Task(title, notes, false), onConfirm = { }, onDismiss = { })
    }
}