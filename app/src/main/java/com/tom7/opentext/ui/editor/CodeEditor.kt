package com.tom7.opentext.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tom7.opentext.ui.theme.Accent
import com.tom7.opentext.ui.theme.CodeTextStyle
import com.tom7.opentext.ui.theme.Grey600
import com.tom7.opentext.ui.theme.GutterTextStyle
import com.tom7.opentext.ui.theme.Syntax

/**
 * A Zed-ish editor surface: a clipped gutter that mirrors the editor's vertical scroll, and a
 * plain [BasicTextField] whose glyphs are recoloured by a [VisualTransformation]. The field sits
 * inside scroll containers with unbounded constraints, so a logical line always stays one visual
 * line and the gutter numbers keep their alignment.
 */
@Composable
fun CodeEditor(
    file: OpenFile,
    onTextChange: (String) -> Unit,
    onCaret: (line: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember(file.path) { mutableStateOf(TextFieldValue(file.text, TextRange(0))) }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val transformation = remember(value.text, file.language) {
        VisualTransformation { TransformedText(highlight(value.text, file.language), OffsetMapping.Identity) }
    }
    val lineCount = remember(value.text) { value.text.count { it == '\n' } + 1 }

    Row(modifier.fillMaxSize()) {
        Box(Modifier.width(46.dp).fillMaxHeight().clipToBounds()) {
            Column(
                Modifier
                    .graphicsLayer { translationY = -vertical.value.toFloat() }
                    .padding(top = 8.dp),
            ) {
                for (line in 1..lineCount) {
                    Text(
                        text = line.toString(),
                        style = GutterTextStyle,
                        color = Grey600,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().padding(end = 10.dp),
                    )
                }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(vertical)
                .horizontalScroll(horizontal),
        ) {
            BasicTextField(
                value = value,
                onValueChange = { next ->
                    value = next
                    onTextChange(next.text)
                    val caret = next.selection.start.coerceIn(0, next.text.length)
                    val before = next.text.take(caret)
                    val line = before.count { it == '\n' } + 1
                    val column = caret - before.lastIndexOf('\n')
                    onCaret(line, column)
                },
                textStyle = CodeTextStyle.copy(color = Syntax.text),
                cursorBrush = SolidColor(Accent),
                visualTransformation = transformation,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 24.dp, bottom = 16.dp),
            )
        }
    }
}
