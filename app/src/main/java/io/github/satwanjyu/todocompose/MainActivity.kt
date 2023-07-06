package io.github.satwanjyu.todocompose

import android.content.Context
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.satwanjyu.todocompose.ui.theme.TodoComposeTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

var db: AppDataBase? = null

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (db == null) {
            db = Room.databaseBuilder(applicationContext, AppDataBase::class.java, "tasks").build()
        }
        setContent {
            val tasksViewModel: TasksViewModel = viewModel()
            val tasks = tasksViewModel.tasks.collectAsState()

            TodoComposeTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController, startDestination = "task-list") {
                        composable("task-list") {
                            TaskListScreen(
                                tasks = tasks.value,
                                onTasksChange = {},
                                onNavigateToEditTask = { task ->
                                    navController.navigate("edit-task?taskId=${task.id}")
                                },
                                onNewTaskClick = {
                                    navController.navigate("new-task")
                                }
                            )
                        }
                        composable(
                            "new-task",
                        ) {
                            EditTaskScreen(
                                onConfirm = { newTask ->
                                    tasksViewModel.addTask(newTask)
                                },
                                onPop = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        // TODO Avoid passing complex data via backstack arguments
                        composable(
                            "edit-task?taskId={id}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.IntType },
                            )
                        ) { entry ->
                            val id = entry.arguments!!.getInt("id")
                            val task = tasks.value.first { it.id == id }

                            EditTaskScreen(
                                task = task,
                                onConfirm = { modifiedTask ->
                                    tasksViewModel.editTask(modifiedTask)
                                },
                                onPop = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

class TasksViewModel : ViewModel() {
    private val dao: TasksDao = db!!.tasksDao()
    val tasks: StateFlow<ImmutableList<Task>>
        get() = dao.getAll().map { list ->
            list.map { entity ->
                Task(
                    id = entity.uid,
                    title = entity.title,
                    notes = entity.notes,
                    completed = entity.completed
                )
            }.toImmutableList()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = persistentListOf()
        )

    suspend fun addTask(task: Task) {
        val taskEntity = TaskEntity(task.id ?: 0, task.title, task.notes, task.completed)
        dao.insert(taskEntity)
    }

    suspend fun editTask(task: Task) {
        val taskEntity = TaskEntity(task.id!!, task.title, task.notes, task.completed)
        dao.insert(taskEntity)
    }

    suspend fun removeTask(task: Task) {
        val taskEntity = TaskEntity(task.id!!, task.title, task.notes, task.completed)
        dao.delete(taskEntity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    title: String,
    notes: String,
    completed: Boolean,
    onCompletedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        content = {
            ListItem(
                headlineContent = {
                    Text(
                        title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = modifier,
                supportingContent = {
                    Text(
                        notes,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingContent = {
                    Checkbox(
                        checked = completed,
                        onCheckedChange = onCompletedChange
                    )
                },
            )
        },
        onClick = onClick,
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
            completed = false,
            onCompletedChange = {},
            onClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    tasks: ImmutableList<Task>,
    onTasksChange: (tasks: ImmutableList<Task>) -> Unit,
    onNavigateToEditTask: (task: Task) -> Unit,
    onNewTaskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewTaskClick,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            // TODO Supply key
            items(tasks, key = { it.id!! }) { task ->
                TaskItem(
                    title = task.title,
                    notes = task.notes,
                    completed = task.completed,
                    onCompletedChange = {},
                    onClick = {
                        onNavigateToEditTask(task)
                    }
                )
            }
        }
    }
}

data class Task(
    val id: Int? = null,
    val title: String,
    val notes: String,
    val completed: Boolean,
)

@Entity
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "completed") val completed: Boolean,
)

@Dao
interface TasksDao {
    @Query("SELECT * FROM taskentity")
    fun getAll(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AppDataBase : RoomDatabase() {
    abstract fun tasksDao(): TasksDao
}

@Preview
@Composable
fun TaskListScreenPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
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

        TaskListScreen(
            tasks = tasks,
            onTasksChange = {},
            onNavigateToEditTask = {},
            onNewTaskClick = {})
    }
}

// TODO Expand transition
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    modifier: Modifier = Modifier,
    task: Task? = null,
    onConfirm: suspend (task: Task) -> Unit,
    onPop: () -> Unit,
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var notes by remember { mutableStateOf(task?.notes ?: "") }

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
                            if (task == null) {
                                R.string.new_task
                            } else {
                                R.string.edit_task
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    val scope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            val newTask = when {
                                task != null -> task.copy(title = title, notes = notes)
                                else -> Task(title = title, notes = notes, completed = false)
                            }
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    onConfirm(newTask)
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
fun EditTaskScreenPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
    val words = lorem
        .replace(Regex("[^A-Za-z ]"), "")
        .split(" ")
    val title = words.subList(0, 5).joinToString(" ")
    val notes = words.subList(5, 10).joinToString(" ")

    TodoComposeTheme {
        EditTaskScreen(task = Task(0, title, notes, false), onConfirm = { }, onPop = { })
    }
}