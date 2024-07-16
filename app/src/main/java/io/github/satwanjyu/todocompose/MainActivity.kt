package io.github.satwanjyu.todocompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.satwanjyu.todocompose.tasks.TaskEntity
import io.github.satwanjyu.todocompose.tasks.TasksDao
import io.github.satwanjyu.todocompose.tasks.tasks
import io.github.satwanjyu.todocompose.ui.theme.TodoComposeTheme

var db: AppDataBase? = null

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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
            TodoComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val windowSizeClass = calculateWindowSizeClass(this)

                    NavHost(navController, startDestination = "tasks") {
                        tasks(windowSizeClass.widthSizeClass)
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
