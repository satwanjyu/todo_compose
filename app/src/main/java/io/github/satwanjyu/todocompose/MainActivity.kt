package io.github.satwanjyu.todocompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.satwanjyu.todocompose.tasks.TaskEntity
import io.github.satwanjyu.todocompose.tasks.TasksDao
import io.github.satwanjyu.todocompose.tasks.tasksScreen
import io.github.satwanjyu.todocompose.ui.theme.TodoComposeTheme

var db: AppDataBase? = null

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (db == null) {
            db =
                Room.databaseBuilder(
                    context = applicationContext,
                    klass = AppDataBase::class.java,
                    name = "todo-compose-db"
                )
                    .build()
        }
        setContent {
            val navController = rememberNavController()
            TodoComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController, startDestination = "task-list") {
                        tasksScreen(
                            onNavigateToEditTask = { taskId ->
                                navController.navigate("edit-task?taskId=$taskId")
                            },
                            onNavigateToNewTask = {
                                navController.navigate("new-task")
                            },
                            onPop = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AppDataBase : RoomDatabase() {
    abstract fun tasksDao(): TasksDao
}
