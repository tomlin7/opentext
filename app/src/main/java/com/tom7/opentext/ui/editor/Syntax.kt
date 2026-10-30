package com.tom7.opentext.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Syntax

enum class Language(
    val label: String,
    val keywords: Set<String> = emptySet(),
    val lineComment: String? = null,
    val blockComment: Pair<String, String>? = null,
    val tripleQuotes: Boolean = false,
) {
    Kotlin(
        "Kotlin",
        setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in",
            "interface", "is", "null", "object", "package", "return", "super", "this", "throw",
            "true", "try", "typealias", "typeof", "val", "var", "when", "while", "by", "catch",
            "constructor", "finally", "get", "import", "init", "set", "where", "actual", "abstract",
            "annotation", "companion", "const", "crossinline", "data", "enum", "expect", "external",
            "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open",
            "operator", "out", "override", "private", "protected", "public", "reified", "sealed",
            "suspend", "tailrec", "vararg", "it",
        ),
        "//", "/*" to "*/",
    ),
    Java(
        "Java",
        setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "try", "void", "volatile", "while", "var", "record", "sealed",
            "permits", "true", "false", "null",
        ),
        "//", "/*" to "*/",
    ),
    JavaScript(
        "JavaScript",
        setOf(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete",
            "do", "else", "export", "extends", "finally", "for", "function", "if", "import", "in",
            "instanceof", "new", "return", "super", "switch", "this", "throw", "try", "typeof",
            "var", "void", "while", "with", "yield", "let", "static", "async", "await", "of",
            "interface", "type", "enum", "implements", "declare", "readonly", "public", "private",
            "protected", "abstract", "as", "namespace", "module", "from", "get", "set",
            "true", "false", "null", "undefined",
        ),
        "//", "/*" to "*/",
    ),
    Python(
        "Python",
        setOf(
            "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del",
            "elif", "else", "except", "False", "finally", "for", "from", "global", "if", "import",
            "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return",
            "True", "try", "while", "with", "yield", "match", "case", "self",
        ),
        "#", null, tripleQuotes = true,
    ),
    Rust(
        "Rust",
        setOf(
            "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn",
            "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut", "pub", "ref",
            "return", "self", "Self", "static", "struct", "super", "trait", "true", "type",
            "unsafe", "use", "where", "while", "async", "await", "dyn",
        ),
        "//", "/*" to "*/",
    ),
    Go(
        "Go",
        setOf(
            "break", "case", "chan", "const", "continue", "default", "defer", "else", "fallthrough",
            "for", "func", "go", "goto", "if", "import", "interface", "map", "package", "range",
            "return", "select", "struct", "switch", "type", "var", "nil", "true", "false",
        ),
        "//", "/*" to "*/",
    ),
    C(
        "C",
        setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
            "register", "return", "short", "signed", "sizeof", "static", "struct", "switch",
            "typedef", "union", "unsigned", "void", "volatile", "while", "class", "namespace",
            "template", "public", "private", "protected", "virtual", "override", "new", "delete",
            "nullptr", "true", "false", "using", "auto",
        ),
        "//", "/*" to "*/",
    ),
    Json("JSON", setOf("true", "false", "null"), lineComment = null),
    Html("HTML", lineComment = null, blockComment = "<!--" to "-->"),
    Css("CSS", lineComment = null, blockComment = "/*" to "*/"),
    Markdown("Markdown", lineComment = null),
    Shell(
        "Shell",
        setOf(
            "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case", "esac",
            "function", "in", "return", "export", "local", "readonly", "true", "false",
        ),
        "#",
    ),
    Yaml("YAML", setOf("true", "false", "null"), "#"),
    Toml("TOML", setOf("true", "false"), "#"),
    Sql(
        "SQL",
        setOf(
            "select", "from", "where", "insert", "into", "values", "update", "set", "delete",
            "create", "table", "alter", "drop", "join", "inner", "left", "right", "outer", "on",
            "group", "by", "order", "having", "limit", "offset", "as", "and", "or", "not", "null",
            "true", "false",
        ),
        "--",
    ),
    Plain("Plain text"),
}

fun detectLanguage(path: String): Language {
    val extension = path.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "kt", "kts" -> Language.Kotlin
        "java" -> Language.Java
        "js", "jsx", "mjs", "cjs", "ts", "tsx" -> Language.JavaScript
        "py", "pyw" -> Language.Python
        "rs" -> Language.Rust
        "go" -> Language.Go
        "c", "h", "cc", "cpp", "cxx", "hpp", "hxx", "cs" -> Language.C
        "json", "jsonc" -> Language.Json
        "html", "htm", "xml", "svg", "vue", "svelte" -> Language.Html
        "css", "scss", "sass", "less" -> Language.Css
        "md", "mdx", "markdown" -> Language.Markdown
        "sh", "bash", "zsh", "fish" -> Language.Shell
        "yml", "yaml" -> Language.Yaml
        "toml" -> Language.Toml
        "sql" -> Language.Sql
        else -> Language.Plain
    }
}

private const val MAX_HIGHLIGHT = 120_000

/**
 * A quiet, regex-driven highlighter. It protects comment and string ranges first, then paints
 * keywords, numbers and call sites only outside them. Good enough for reading on a phone, and
 * bounded so a giant file never stalls a keystroke.
 */
fun highlight(text: String, language: Language): AnnotatedString {
    if (language == Language.Plain || text.length > MAX_HIGHLIGHT) return AnnotatedString(text)
    if (language == Language.Markdown) return highlightMarkdown(text)

    val protected = mutableListOf<IntRange>()

    language.blockComment?.let { (open, close) ->
        val pattern = Regex(Regex.escape(open) + "[\\s\\S]*?" + Regex.escape(close))
        pattern.findAll(text).forEach { protected += it.range }
    }

    language.lineComment?.let { token ->
        val pattern = Regex(Regex.escape(token) + "[^\n]*")
        pattern.findAll(text).forEach { protected += it.range }
    }

    // Strings. Triple-quoted blocks first when the language allows them.
    if (language.tripleQuotes) {
        Regex("\"\"\"[\\s\\S]*?\"\"\"").findAll(text).forEach { protected += it.range }
        Regex("'''[\\s\\S]*?'''").findAll(text).forEach { protected += it.range }
    }
    Regex("\"(?:\\\\.|[^\"\\\\\n])*\"").findAll(text).forEach { protected += it.range }
    Regex("'(?:\\\\.|[^'\\\\\n])*'").findAll(text).forEach { protected += it.range }
    Regex("`(?:\\\\.|[^`\\\\])*`").findAll(text).forEach { protected += it.range }

    val ordered = protected.sortedBy { it.first }

    fun isProtected(start: Int, end: Int): Boolean {
        var low = 0
        var high = ordered.size - 1
        while (low <= high) {
            val mid = (low + high) / 2
            val range = ordered[mid]
            when {
                range.last < start -> low = mid + 1
                range.first > end -> high = mid - 1
                else -> return true
            }
        }
        return false
    }

    return buildAnnotatedString {
        append(text)

        for (range in ordered) {
            addStyle(
                SpanStyle(color = if (language.blockComment?.let { text.startsWith(it.first, range.first) } == true) Syntax.comment else Syntax.string),
                range.first,
                range.last + 1,
            )
        }

        if (language.keywords.isNotEmpty()) {
            val keywordPattern = Regex("\\b(" + language.keywords.joinToString("|") { Regex.escape(it) } + ")\\b")
            keywordPattern.findAll(text).forEach { match ->
                if (!isProtected(match.range.first, match.range.last)) {
                    addStyle(SpanStyle(color = Syntax.keyword), match.range.first, match.range.last + 1)
                }
            }
        }

        Regex("\\b\\d+(?:\\.\\d+)?\\b").findAll(text).forEach { match ->
            if (!isProtected(match.range.first, match.range.last)) {
                addStyle(SpanStyle(color = Syntax.number), match.range.first, match.range.last + 1)
            }
        }

        Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?=\\()").findAll(text).forEach { match ->
            val word = match.groupValues[1]
            if (word !in language.keywords && !isProtected(match.range.first, match.range.last)) {
                addStyle(SpanStyle(color = Syntax.function), match.range.first, match.range.last + 1)
            }
        }

        Regex("\\b([A-Z][A-Za-z0-9_]{2,})\\b").findAll(text).forEach { match ->
            val word = match.groupValues[1]
            if (word !in language.keywords && !isProtected(match.range.first, match.range.last)) {
                addStyle(SpanStyle(color = Syntax.type), match.range.first, match.range.last + 1)
            }
        }
    }
}

/**
 * Markdown is prose, so the code tokenizer's "capitalised word is a type" rule reads as noise.
 * This keeps to the structure a reader scans: headings, emphasis, code spans and links.
 */
private fun highlightMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    append(text)

    fun paint(pattern: Regex, style: SpanStyle) {
        pattern.findAll(text).forEach { match ->
            addStyle(style, match.range.first, match.range.last + 1)
        }
    }

    paint(
        Regex("^#{1,6} [^\n]*", RegexOption.MULTILINE),
        SpanStyle(color = Grey100, fontWeight = FontWeight.SemiBold),
    )
    paint(Regex("\\*\\*[^*\n]+\\*\\*"), SpanStyle(color = Syntax.type, fontWeight = FontWeight.Bold))
    paint(Regex("__[^_\n]+__"), SpanStyle(color = Syntax.type, fontWeight = FontWeight.Bold))
    paint(Regex("`[^`\n]+`"), SpanStyle(color = Syntax.string))
    paint(Regex("\\[[^\\]\\n]*\\]\\([^)\\n]*\\)"), SpanStyle(color = Syntax.function))
    paint(Regex("^> [^\n]*", RegexOption.MULTILINE), SpanStyle(color = Syntax.comment, fontStyle = FontStyle.Italic))
    paint(Regex("^\\s*(?:[-*+]|\\d+\\.) ", RegexOption.MULTILINE), SpanStyle(color = Syntax.operator))
}
