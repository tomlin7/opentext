package com.tom7.opentext

import com.tom7.opentext.ui.editor.Language
import com.tom7.opentext.ui.editor.detectLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageTest {
    @Test
    fun mapsCommonExtensions() {
        assertEquals(Language.Kotlin, detectLanguage("src/main/Main.kt"))
        assertEquals(Language.Kotlin, detectLanguage("build.gradle.kts"))
        assertEquals(Language.JavaScript, detectLanguage("app/index.tsx"))
        assertEquals(Language.Python, detectLanguage("tools/run.py"))
        assertEquals(Language.Markdown, detectLanguage("README.md"))
        assertEquals(Language.Yaml, detectLanguage(".github/workflows/ci.yml"))
    }

    @Test
    fun isCaseInsensitive() {
        assertEquals(Language.Rust, detectLanguage("SRC/MAIN.RS"))
    }

    @Test
    fun fallsBackToPlainText() {
        assertEquals(Language.Plain, detectLanguage("LICENSE"))
        assertEquals(Language.Plain, detectLanguage("data.bin"))
    }

    @Test
    fun dotfilesHaveNoExtension() {
        assertEquals(Language.Plain, detectLanguage(".gitignore"))
    }
}
