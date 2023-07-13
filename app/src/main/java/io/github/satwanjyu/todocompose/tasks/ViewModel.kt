package io.github.satwanjyu.todocompose.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.satwanjyu.todocompose.db
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal sealed class UiState {
    abstract val tasks: ImmutableList<Task>

    data class Tick(
        override val tasks: ImmutableList<Task>,
        val query: String? = null,
        val queriedTasks: ImmutableList<Task> = persistentListOf(),
    ) : UiState()

    data class Select(
        override val tasks: ImmutableList<Task>,
        val selectedTasks: ImmutableSet<Task>,
    ) : UiState()

    data class Edit(
        override val tasks: ImmutableList<Task>,
        val task: Task,
    ) : UiState()

    data class Create(
        override val tasks: ImmutableList<Task>,
        val title: String,
        val notes: String,
    ) : UiState()
}

internal class TasksViewModel : ViewModel() {
    private val dao: TasksDao = db!!.tasksDao()
    private val tasks: StateFlow<ImmutableList<Task>> = dao.getAll().map { list ->
        list.map(TaskEntity::toTask).toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = persistentListOf()
    )

    private val queriedTasks = MutableStateFlow<ImmutableList<Task>>(persistentListOf())

    val uiState = MutableStateFlow<UiState>(UiState.Tick(tasks = persistentListOf()))

    init {
        viewModelScope.launch {
            combine(tasks, queriedTasks) { tasks, queriedTasks ->
                tasks to queriedTasks
            }.collect { pair ->
                val tasks = pair.first
                val queriedTasks = pair.second
                uiState.update { uiState ->
                    when (uiState) {
                        is UiState.Tick -> uiState.copy(tasks = tasks, queriedTasks = queriedTasks)
                        is UiState.Select -> uiState.copy(tasks = tasks)
                        is UiState.Create -> uiState.copy(tasks = tasks)
                        is UiState.Edit -> uiState.copy(tasks = tasks)
                    }
                }
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val tasks = dao.search(query)
            queriedTasks.value = tasks.map(TaskEntity::toTask).toImmutableList()
        }
    }

    fun insertTask(
        title: String,
        notes: String,
    ) {
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
