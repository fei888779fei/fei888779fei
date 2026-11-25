package com.novelreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NovelReaderApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { NovelRepository(context) }
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Book>>(emptyList()) }
    var library by remember { mutableStateOf(repository.library()) }
    var selectedText by remember { mutableStateOf<String?>(null) }
    var selectedTitle by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    MaterialTheme {
        selectedText?.let { text ->
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text(selectedTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                SelectionContainer(Modifier.weight(1f).verticalScroll(rememberScrollState())) { Text(text) }
                TextButton(onClick = { selectedText = null }) { Text("关闭") }
            }
            return
        }

        Scaffold(bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, label = { Text("发现") }, icon = {})
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1; library = repository.library() }, label = { Text("书架") }, icon = {})
            }
        }) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                if (tab == 0) {
                    OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("搜索中文公版小说") })
                    Spacer(Modifier.height(8.dp))
                    Button(enabled = !loading, onClick = {
                        loading = true; message = ""
                        scope.launch {
                            try { results = withContext(Dispatchers.IO) { repository.search(query) }; if (results.isEmpty()) message = "没有找到结果" }
                            catch (e: Exception) { message = "搜索失败：${e.message}" }
                            finally { loading = false }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "搜索中…" else "搜索") }
                    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
                    LazyColumn(Modifier.fillWeight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results) { book ->
                            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                                Text(book.title, style = MaterialTheme.typography.titleMedium)
                                Text(book.authors.joinToString { it.name }, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { scope.launch {
                                    message = "正在下载…"
                                    val saved = withContext(Dispatchers.IO) { repository.download(book) }
                                    message = if (saved) "已加入书架" else "没有可下载的纯文本版本"
                                    library = repository.library()
                                } }) { Text("下载离线") }
                            } }
                        }
                    }
                } else {
                    Text("我的书架", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(library) { file -> ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                            Text(file.nameWithoutExtension)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { scope.launch(Dispatchers.IO) {
                                val text = file.readText()
                                withContext(Dispatchers.Main) { selectedTitle = file.nameWithoutExtension; selectedText = text }
                            } }) { Text("阅读") }
                        } } }
                    }
                }
            }
        }
    }
}
