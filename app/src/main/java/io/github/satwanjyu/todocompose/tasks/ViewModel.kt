package io.github.satwanjyu.todocompose.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.satwanjyu.todocompose.db
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class UiState(
    val tasks: ImmutableList<Task>,
    val queriedTasks: ImmutableList<Task>,
    val selectedTasks: ImmutableSet<Task>,
    val creatingTask: Boolean,
    val editingTask: Task?,
)

// TODO inject dao
internal class TasksViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(
        UiState(
            tasks = persistentListOf(),
            queriedTasks = persistentListOf(),
            selectedTasks = persistentSetOf(),
            creatingTask = false,
            editingTask = null,
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    private val dao: TasksDao = db!!.tasksDao()

    init {
        viewModelScope.launch {
            dao.getAll().map { taskEntities ->
                taskEntities.map { taskEntity ->
                    taskEntity.toTask()
                }
            }.collect { tasks ->
                _uiState.update { previous ->
                    previous.copy(tasks = tasks.toImmutableList())
                }
            }
        }
    }

    var queryText by mutableStateOf<String?>(null)

    fun insertTask(title: String, notes: String) {
        val taskEntity = TaskEntity(0, title, notes, false)
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(taskEntity)
        }
    }

    fun insertTask(task: Task) {
        val taskEntity = TaskEntity(task.id, task.title, task.notes, task.completed)
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(taskEntity)
        }
    }

    fun removeTasks(tasks: Set<Task>) {
        val entities = tasks.map { TaskEntity(it.id, it.title, it.notes, it.completed) }
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAll(*entities.toTypedArray())
        }
    }

    fun selectTasks(tasks: ImmutableSet<Task>) {
        _uiState.update { prev ->
            prev.copy(selectedTasks = tasks)
        }
    }

    fun createTask() {
        _uiState.update { prev ->
            prev.copy(creatingTask = true)
        }
    }

    fun dismissCreateTask() {
        _uiState.update { prev ->
            prev.copy(creatingTask = false)
        }
    }

    var taskTitleText by mutableStateOf("")
    var taskNotesText by mutableStateOf("")

    fun editTask(task: Task) {
        _uiState.update { prev ->
            prev.copy(editingTask = task)
        }
    }

    fun dismissEditTask() {
        _uiState.update { prev ->
            prev.copy(editingTask = null)
        }
    }

    fun searchTask(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val matchingTasks = dao.search(query)
                .map { it.toTask() }
                .toPersistentList()
            withContext(Dispatchers.Main) {
                _uiState.update { prev ->
                    prev.copy(queriedTasks = matchingTasks)
                }
            }
        }
    }
}
