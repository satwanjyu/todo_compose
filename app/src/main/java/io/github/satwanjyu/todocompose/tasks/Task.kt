package io.github.satwanjyu.todocompose.tasks

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class Task(
    val id: Int,
    val title: String,
    val notes: String,
    val completed: Boolean,
) : Parcelable

