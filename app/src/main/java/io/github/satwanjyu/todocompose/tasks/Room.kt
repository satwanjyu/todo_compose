package io.github.satwanjyu.todocompose.tasks

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Fts4(notIndexed = ["completed"])
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val id: Int,
    val title: String,
    val notes: String,
    val completed: Boolean,
) {
    internal fun toTask() = Task(id, title, notes, completed)
}

@Dao
interface TasksDao {
    @Query("SELECT *, rowid FROM tasks")
    fun getAll(): Flow<List<TaskEntity>>

    // FIXME search doesn't work with title
    @Query("SELECT *, rowid FROM tasks WHERE title LIKE :query OR notes LIKE :query")
    suspend fun search(query: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Delete
    suspend fun deleteAll(vararg tasks: TaskEntity)
}

