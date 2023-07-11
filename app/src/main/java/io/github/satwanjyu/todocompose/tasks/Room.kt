package io.github.satwanjyu.todocompose.tasks

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "completed") val completed: Boolean,
) {
    internal fun toTask() = Task(uid, title, notes, completed)
}

@Dao
interface TasksDao {
    @Query("SELECT * FROM taskentity WHERE uid = :id")
    suspend fun get(id: Int): TaskEntity

    @Query("SELECT * FROM taskentity")
    fun getAll(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Delete
    suspend fun deleteAll(vararg tasks: TaskEntity)
}

