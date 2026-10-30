package com.tom7.opentext.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tom7.opentext.ui.theme.Accent
import com.tom7.opentext.ui.theme.CodeTextStyle
import com.tom7.opentext.ui.theme.Grey600
import com.tom7.opentext.ui.theme.GutterTextStyle
import com.tom7.opentext.ui.theme.Syntax
import kotlin.math.roundToInt

/**
 * A Zed-ish editor surface: a clipped gutter that mirrors the editor's vertical scroll, and a
 * plain [BasicTextField] whose glyphs are recoloured by a [VisualTransformation]. The field sits
 * inside scroll containers with unbounded constraints, so a logical line always stays one visual
 * line and the gutter numbers keep their alignment.
 *
 * Line numbers are positioned using the text layout's actual line top positions so that the
 * gutter baseline always matches the code baseline regardless of font size differences.
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

    // Capture the text layout so gutter numbers can be positioned at exact line positions.
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textHeightPx by remember { mutableStateOf(0) }

    // Gutter padding must match the BasicTextField padding so both start at the same y-offset.
    val topPadding = 8.dp
    val bottomPadding = 16.dp
    val gutterStartPadding = 4.dp
    val gutterEndPadding = 10.dp
    val codeStartPadding = 4.dp
    val codeEndPadding = 24.dp
    val gutterWidth = 46.dp

    Row(modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(vertical)
                .horizontalScroll(horizontal),
        ) {
            Row {
                // Gutter: renders each line number at the exact y-position of the
                // corresponding code line, guaranteeing baseline alignment.
                Box(
                    Modifier
                        .width(gutterWidth)
                        .padding(
                            top = topPadding,
                            end = gutterEndPadding,
                            start = gutterStartPadding,
                            bottom = bottomPadding,
                        ),
                ) {
                    val layout = textLayout
                    if (layout != null) {
                        val lineCount = layout.lineCount
                        for (i in 0 until lineCount) {
                            val lineTopPx = layout.getLineTop(i)
                            Text(
                                text = "${i + 1}",
                                style = GutterTextStyle,
                                color = Grey600,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(x = 0, y = lineTopPx.roundToInt()) },
                            )
                        }
                    }
                    // Reserve height equal to the editor's text area so the scroll
                    // container scrolls far enough even with few lines.
                    if (textHeightPx > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(0, textHeightPx) },
                        ) {}
                    }
                }

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
                    onTextLayout = { result ->
                        textLayout = result
                        textHeightPx = result.size.height
                    },
                    modifier = Modifier
                        .padding(
                            top = topPadding,
                            start = codeStartPadding,
                            end = codeEndPadding,
                            bottom = bottomPadding,
                        ),
                )
            }
        }
    }
}
