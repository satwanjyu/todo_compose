package io.github.satwanjyu.todocompose.tasks

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.satwanjyu.todocompose.db
import io.github.satwanjyu.todocompose.tasks.data.Task
import io.github.satwanjyu.todocompose.tasks.data.TaskEntity
import io.github.satwanjyu.todocompose.tasks.data.TasksDao
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed class UiState {
    data class Tick(
        val tasks: ImmutableList<Task>,
        val queriedTasks: ImmutableList<Task> = persistentListOf(),
    ) : UiState()

    data class Select(
        val tasks: ImmutableList<Task>,
        val selectedTasks: ImmutableSet<Task>,
    ) : UiState()

    data class Edit(val task: Task) : UiState()

    data object Create : UiState()
}

internal class TasksViewModel : ViewModel() {
    val uiState = MutableStateFlow<UiState>(UiState.Tick(tasks = persistentListOf()))

    private val dao: TasksDao = db!!.tasksDao()
    val tasks: StateFlow<ImmutableList<Task>> = dao.getAll().map { list ->
        list.map(TaskEntity::toTask).toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = persistentListOf()
    )

    var searchText by mutableStateOf("")

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
}
