package io.github.satwanjyu.todocompose.tasks.data

internal data class Task(
    val id: Int,
    val title: String,
    val notes: String,
    val completed: Boolean,
)
