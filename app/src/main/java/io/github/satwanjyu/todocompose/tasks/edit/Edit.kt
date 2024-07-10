package io.github.satwanjyu.todocompose.tasks.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import io.github.satwanjyu.todocompose.R
import io.github.satwanjyu.todocompose.tasks.data.Task
import io.github.satwanjyu.todocompose.ui.theme.TodoComposeTheme

internal sealed interface EditTaskAction {
    data object Create : EditTaskAction

    data class Edit(val task: Task) : EditTaskAction
}

// TODO Expand transition
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTaskScaffold(
    modifier: Modifier = Modifier,
    action: EditTaskAction,
    onTaskSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val (title, notes) = when (action) {
        is EditTaskAction.Create -> {
            Pair("", "")
        }

        is EditTaskAction.Edit -> {
            action.task.let { task ->
                Pair(task.title, task.notes)
            }
        }
    }
    var titleBuffer by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(title))
    }
    var notesBuffer by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(notes))
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { Text(stringResource(R.string.dismiss)) },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = onDismiss
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.dismiss)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        stringResource(
                            when (action) {
                                is EditTaskAction.Create -> R.string.new_task
                                is EditTaskAction.Edit -> R.string.edit_task
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { Text(stringResource(R.string.confirm)) },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = {
                                onTaskSave(titleBuffer.text, notesBuffer.text)
                            }
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.confirm)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        EditTaskForm(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues),
            title = titleBuffer.text,
            onTitleChange = { titleBuffer = titleBuffer.copy(text = it) },
            notes = notesBuffer.text,
            onNotesChange = { notesBuffer = notesBuffer.copy(text = it) }
        )
    }
    // TODO Hacky
    BackHandler(onBack = onDismiss)
}

@Composable
private fun EditTaskForm(
    modifier: Modifier = Modifier,
    title: String,
    onTitleChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxHeight(),
        contentPadding = contentPadding,
    ) {
        val textFieldModifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)

        item {
            TextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = textFieldModifier,
                label = { Text(stringResource(R.string.title)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        // TODO focus on notes
                    }
                )
            )
        }
        item {
            TextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = textFieldModifier,
                label = { Text(stringResource(R.string.notes)) },
                minLines = 6,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // TODO submit
                        defaultKeyboardAction(ImeAction.Done)
                    }
                ),
            )
        }
    }
}

@Preview
@Composable
private fun EditTaskScaffoldPreview(@PreviewParameter(LoremIpsum::class) lorem: String) {
    val words = lorem
        .replace(Regex("[^A-Za-z ]"), "")
        .split(" ")
    val title = words.subList(0, 5).joinToString(" ")
    val notes = words.subList(5, 10).joinToString(" ")

    TodoComposeTheme {
        EditTaskScaffold(action = EditTaskAction.Create, onTaskSave = { _, _ -> }, onDismiss = {})
    }
}

