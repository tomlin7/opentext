package com.tom7.opentext.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom7.opentext.data.Entry
import com.tom7.opentext.data.Workspace
import kotlinx.coroutines.launch

data class OpenFile(
    val path: String,
    val name: String,
    val language: Language,
    val text: String,
    val saved: String,
) {
    val modified: Boolean get() = text != saved
}

data class TreeRow(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val depth: Int,
    val expanded: Boolean,
)

private val SKIPPED = setOf("node_modules", ".git", "build", "dist", ".gradle", ".idea", ".next", "target", "vendor")

class EditorViewModel : ViewModel() {
    var workspace by mutableStateOf<Workspace?>(null)
        private set
    var rows by mutableStateOf<List<TreeRow>>(emptyList())
        private set
    var files by mutableStateOf<List<OpenFile>>(emptyList())
        private set
    var activeIndex by mutableIntStateOf(-1)
        private set
    var loading by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var toast by mutableStateOf<String?>(null)
        private set
    var pendingClose by mutableStateOf<Int?>(null)
        private set

    private val expanded = linkedSetOf<String>()
    private val cache = mutableMapOf<String, List<Entry>>()

    val active: OpenFile? get() = files.getOrNull(activeIndex)

    fun open(workspace: Workspace) {
        if (this.workspace?.id == workspace.id && cache.isNotEmpty()) return
        this.workspace = workspace
        expanded.clear()
        cache.clear()
        files = emptyList()
        activeIndex = -1
        error = null
        refresh()
    }

    fun refresh() {
        val workspace = workspace ?: return
        viewModelScope.launch {
            loading = true
            error = null
            runCatching { workspace.list("") }
                .onSuccess {
                    cache.clear()
                    cache[""] = it
                    rebuild()
                }
                .onFailure { error = it.message ?: "Could not open this folder." }
            loading = false
        }
    }

    fun toggle(entry: Entry) {
        if (!entry.isDirectory) return
        val workspace = workspace ?: return
        if (expanded.remove(entry.path)) {
            rebuild()
            return
        }
        expanded.add(entry.path)
        if (cache.containsKey(entry.path)) {
            rebuild()
            return
        }
        viewModelScope.launch {
            runCatching { workspace.list(entry.path) }
                .onSuccess {
                    cache[entry.path] = it
                    rebuild()
                }
                .onFailure { toast = it.message ?: "Could not open ${entry.name}." }
        }
    }

    fun openFile(entry: Entry) {
        if (entry.isDirectory) {
            toggle(entry)
            return
        }
        val existing = files.indexOfFirst { it.path == entry.path }
        if (existing >= 0) {
            activeIndex = existing
            return
        }
        val workspace = workspace ?: return
        viewModelScope.launch {
            loading = true
            runCatching { workspace.readText(entry.path) }
                .onSuccess { text ->
                    files = files + OpenFile(
                        path = entry.path,
                        name = entry.name,
                        language = detectLanguage(entry.path),
                        text = text,
                        saved = text,
                    )
                    activeIndex = files.lastIndex
                }
                .onFailure { toast = it.message ?: "Could not open ${entry.name}." }
            loading = false
        }
    }

    fun updateActive(text: String) {
        val index = activeIndex
        if (index !in files.indices) return
        if (files[index].text == text) return
        files = files.toMutableList().also { it[index] = it[index].copy(text = text) }
    }

    fun save() {
        val workspace = workspace ?: return
        val index = activeIndex
        if (index !in files.indices) return
        viewModelScope.launch { saveAt(workspace, index) }
    }

    /** Writes every modified tab, used when the whole workspace is closing. */
    fun saveAll() {
        val workspace = workspace ?: return
        val dirty = files.indices.filter { files[it].modified }
        if (dirty.isEmpty()) return
        viewModelScope.launch {
            saving = true
            var failed = 0
            for (index in dirty) {
                runCatching { workspace.writeText(files[index].path, files[index].text) }
                    .onSuccess { files = files.toMutableList().also { list -> list[index] = list[index].copy(saved = list[index].text) } }
                    .onFailure { failed++ }
            }
            toast = if (failed == 0) "Saved ${dirty.size} file${if (dirty.size == 1) "" else "s"}" else "Could not save $failed file(s)."
            saving = false
        }
    }

    private suspend fun saveAt(workspace: Workspace, index: Int) {
        val file = files[index]
        saving = true
        runCatching { workspace.writeText(file.path, file.text) }
            .onSuccess {
                files = files.toMutableList().also { list -> list[index] = list[index].copy(saved = list[index].text) }
                toast = "Saved ${file.name}"
            }
            .onFailure { toast = it.message ?: "Could not save ${file.name}." }
        saving = false
    }

    fun select(index: Int) {
        if (index in files.indices) activeIndex = index
    }

    fun requestClose(index: Int) {
        if (index !in files.indices) return
        if (files[index].modified) {
            pendingClose = index
            return
        }
        closeNow(index)
    }

    fun confirmClose() {
        pendingClose?.let { closeNow(it) }
        pendingClose = null
    }

    fun dismissClose() {
        pendingClose = null
    }

    fun consumeToast() {
        toast = null
    }

    fun clearError() {
        error = null
    }

    /** Recursively index text files for quick open. Bounded so a huge tree stays responsive. */
    suspend fun buildIndex(): List<Entry> {
        val workspace = workspace ?: return emptyList()
        val result = mutableListOf<Entry>()
        val queue = ArrayDeque<Pair<String, Int>>()
        queue.add("" to 0)
        while (queue.isNotEmpty() && result.size < 4000) {
            val (directory, depth) = queue.removeFirst()
            if (depth > 8) continue
            val entries = runCatching { workspace.list(directory) }.getOrDefault(emptyList())
            for (entry in entries) {
                if (entry.name in SKIPPED) continue
                if (entry.isDirectory) queue.add(entry.path to depth + 1) else result.add(entry)
            }
        }
        return result.sortedBy { it.path }
    }

    private fun closeNow(index: Int) {
        if (index !in files.indices) return
        files = files.filterIndexed { i, _ -> i != index }
        activeIndex = when {
            files.isEmpty() -> -1
            activeIndex > index -> activeIndex - 1
            activeIndex == index -> index.coerceAtMost(files.lastIndex)
            else -> activeIndex
        }
    }

    private fun rebuild() {
        val out = mutableListOf<TreeRow>()
        fun walk(directory: String, depth: Int) {
            val entries = cache[directory] ?: return
            for (entry in entries) {
                val isExpanded = entry.isDirectory && expanded.contains(entry.path)
                out += TreeRow(entry.path, entry.name, entry.isDirectory, depth, isExpanded)
                if (isExpanded) walk(entry.path, depth + 1)
            }
        }
        walk("", 0)
        rows = out
    }
}
