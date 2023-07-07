package io.github.satwanjyu.todocompose.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.satwanjyu.todocompose.db
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewModel : ViewModel() {
    private val dao: TasksDao = db!!.tasksDao()
    val tasks: StateFlow<ImmutableList<Task>>
        get() = dao.getAll().map { list ->
            list.map(TaskEntity::toTask).toImmutableList()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = persistentListOf()
        )

    val selectedTasks = MutableStateFlow(persistentListOf<Task>())

    suspend fun getTask(id: Int): Task {
        return dao.get(id).toTask()
    }

    suspend fun addTask(
        title: String,
        notes: String,
    ) {
        val taskEntity = TaskEntity(0, title, notes, false)
        dao.insert(taskEntity)
    }

    suspend fun editTask(task: Task) {
        val taskEntity = TaskEntity(task.id, task.title, task.notes, task.completed)
        dao.insert(taskEntity)
    }

    suspend fun removeTasks(tasks: List<Task>) {
        val entities = tasks.map { TaskEntity(it.id, it.title, it.notes, it.completed) }
        dao.deleteAll(*entities.toTypedArray())
    }
}
