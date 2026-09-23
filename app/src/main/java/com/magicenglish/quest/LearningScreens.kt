package com.magicenglish.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun WordLearningScreen(
    words: List<Word>,
    store: StudentStore,
    speak: (String) -> Unit,
    success: () -> Unit,
    error: () -> Unit,
    onComplete: () -> Unit
) {
    var wordIndex by remember { mutableIntStateOf(0) }
    var step by remember { mutableIntStateOf(1) }
    var feedback by remember { mutableStateOf("") }
    val word = words[wordIndex]
    val meaningChoices = remember(word.id) {
        randomizedChoices(
            correct = word.meaning,
            alternatives = words.map { it.meaning },
            seed = word.id.hashCode() xor 0x31A7
        )
    }
    val wordChoices = remember(word.id) {
        randomizedChoices(
            correct = word.word,
            alternatives = words.map { it.word },
            seed = word.id.hashCode() xor 0x6D2B
        )
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF101A2E)).padding(horizontal = 110.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth().gamePanel(AppColors.Panel, Color(0xFF40506A), 16)
                .padding(horizontal = 22.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📖 单词探索 ${wordIndex + 1} / ${words.size}",
                color = AppColors.Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("1.词形发音", "2.语境探秘", "3.听音辨义", "4.看义识词", "5.拼写闯关")
                    .forEachIndexed { index, label -> Pill(label, step == index + 1) }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (step) {
                1 -> LearningCard {
                    Text("Step 1 · 看词形 & 听纯正发音", color = AppColors.Gold, fontWeight = FontWeight.Black)
                    Text(word.word, color = AppColors.Gold, fontSize = 58.sp, fontWeight = FontWeight.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(word.phonetic, color = AppColors.Muted, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        GameButton("🔊", Modifier.size(48.dp), AppColors.Sky, Color(0xFF1272A0)) { speak(word.word) }
                    }
                    Text(
                        word.meaning,
                        Modifier.background(Color(0xFF102A38), RoundedCornerShape(14.dp))
                            .padding(horizontal = 30.dp, vertical = 10.dp),
                        color = Color(0xFF34D399),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                    GameButton("进入语境例句 ➜", Modifier.width(230.dp).height(52.dp)) { step = 2 }
                }

                2 -> LearningCard {
                    Text("Step 2 · 在真实语境中理解", color = AppColors.Gold, fontWeight = FontWeight.Black)
                    Text("“ ${word.example} ”", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("先听完整句子，再观察单词在句子中的位置。", color = AppColors.Muted, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        GameButton("🔊 听例句", Modifier.width(150.dp).height(52.dp), AppColors.Sky, Color(0xFF1272A0)) {
                            speak(word.example)
                        }
                        GameButton("我理解了 ➜", Modifier.width(180.dp).height(52.dp)) { step = 3 }
                    }
                }

                3 -> ChoiceCard(
                    title = "Step 3 · 听音辨义",
                    prompt = "点击喇叭听发音，选择正确的中文意思",
                    options = meaningChoices.first,
                    correct = meaningChoices.second,
                    speakAction = { speak(word.word) },
                    onCorrect = { success(); feedback = ""; step = 4 },
                    onWrong = { error(); feedback = "再认真听一次"; store.recordWordError(word) },
                    feedback = feedback
                )

                4 -> ChoiceCard(
                    title = "Step 4 · 看义识词",
                    prompt = "“${word.meaning}” 对应哪个英文单词？",
                    options = wordChoices.first,
                    correct = wordChoices.second,
                    onCorrect = { success(); feedback = ""; step = 5 },
                    onWrong = { error(); feedback = "再观察一下词形"; store.recordWordError(word) },
                    feedback = feedback
                )

                else -> SpellingPanel(
                    word = word,
                    speak = speak,
                    onError = { store.recordWordError(word); error() },
                    onFinished = { usedHint ->
                        if (!usedHint) store.recordWordMastered(word)
                        success()
                        if (wordIndex == words.lastIndex) onComplete()
                        else {
                            wordIndex += 1
                            step = 1
                            feedback = ""
                        }
                    }
                )
            }
        }
        Text("提示：支持触摸屏与外接键盘，答错不惩罚，随时点击喇叭听发音", color = Color(0xFF64748B), fontSize = 12.sp)
    }
}

@Composable
private fun LearningCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.width(770.dp).wrapContentHeight().gamePanel(AppColors.Panel, Color(0xFF8A712B), 24)
            .padding(horizontal = 44.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
        content = content
    )
}

@Composable
private fun ChoiceCard(
    title: String,
    prompt: String,
    options: List<String>,
    correct: Int,
    feedback: String,
    speakAction: (() -> Unit)? = null,
    onCorrect: () -> Unit,
    onWrong: () -> Unit
) {
    LearningCard {
        Text(title, color = AppColors.Gold, fontWeight = FontWeight.Black)
        if (speakAction != null) {
            GameButton("🔊 播放发音", Modifier.width(180.dp).height(50.dp), AppColors.Sky, Color(0xFF1272A0), onClick = speakAction)
        }
        Text(prompt, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            options.forEachIndexed { index, option ->
                GameButton(
                    option,
                    Modifier.width(195.dp).height(62.dp),
                    if (index == correct) Color(0xFF475569) else Color(0xFF475569),
                    Color(0xFF1E293B)
                ) {
                    if (index == correct) onCorrect() else onWrong()
                }
            }
        }
        Text(feedback, color = if (feedback.contains("再")) Color(0xFFFB7185) else Color(0xFF34D399), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SpellingScreen(
    words: List<Word>,
    store: StudentStore,
    speak: (String) -> Unit,
    success: () -> Unit,
    error: () -> Unit,
    onComplete: () -> Unit
) {
    var wordIndex by remember { mutableIntStateOf(0) }
    val word = words[wordIndex]
    Box(Modifier.fillMaxSize().background(Color(0xFF101A2E)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "⌨ 单元拼写 ${wordIndex + 1} / ${words.size}",
                color = AppColors.Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(12.dp))
            SpellingPanel(
                word = word,
                speak = speak,
                onError = { store.recordWordError(word); error() },
                onFinished = { usedHint ->
                    if (!usedHint) store.recordWordMastered(word)
                    success()
                    if (wordIndex == words.lastIndex) onComplete() else wordIndex += 1
                }
            )
        }
    }
}

@Composable
fun SpellingPanel(
    word: Word,
    speak: (String) -> Unit,
    onError: () -> Unit,
    onFinished: (usedHint: Boolean) -> Unit
) {
    var typed by remember(word.id) { mutableStateOf("") }
    var message by remember(word.id) { mutableStateOf("") }
    var usedHint by remember(word.id) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val target = remember(word.id) { word.word.filter { it.isLetter() }.lowercase() }
    val tileWidth = when {
        target.length > 18 -> 27.dp
        target.length > 13 -> 34.dp
        target.length > 9 -> 43.dp
        else -> 52.dp
    }

    fun submitLetter(letter: Char) {
        if (typed.length >= target.length) return
        val expected = target[typed.length]
        if (letter.lowercaseChar() == expected) {
            typed += expected
            message = if (typed.length == target.length) "🎉 拼写完全正确！Awesome!" else ""
        } else {
            message = "❌ 字母 ${letter.uppercaseChar()} 不对，再试一次"
            onError()
        }
    }

    fun revealNextLetter() {
        if (typed.length >= target.length) return
        typed += target[typed.length]
        usedHint = true
        message = "💡 已提示 1 个字母，并记录 1 次错误"
        onError()
    }

    fun revealAnswer() {
        if (typed.length >= target.length) return
        typed = target
        usedHint = true
        message = "💡 已显示完整拼写，并记录 1 次错误"
        onError()
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier.width(760.dp).gamePanel(AppColors.Panel, Color(0xFF38BDF8), 24)
            .padding(horizontal = 34.dp, vertical = 24.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.Backspace -> {
                        if (typed.isNotEmpty()) typed = typed.dropLast(1)
                        true
                    }
                    event.nativeKeyEvent.unicodeChar.toChar().isLetter() -> {
                        submitLetter(event.nativeKeyEvent.unicodeChar.toChar())
                        true
                    }
                    else -> false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⌨ 键盘拼写模式（外接键盘或屏幕轻触）", color = Color(0xFF7DD3FC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("🔊 播放发音", Modifier.clickable { speak(word.word) }, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        }
        Text(word.meaning, color = AppColors.Gold, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(word.phonetic, color = AppColors.Muted, fontSize = 15.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(if (target.length > 15) 3.dp else 8.dp)) {
            target.forEachIndexed { index, char ->
                Box(
                    Modifier.size(tileWidth, 60.dp)
                        .background(
                            when {
                                index < typed.length -> Color(0xFF22C55E)
                                index == typed.length -> Color(0xFF334155)
                                else -> Color(0xFF273449)
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            2.dp,
                            if (index == typed.length) Color(0xFF38BDF8) else Color(0xFF172033),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (index < typed.length) char.uppercase() else "_", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Text(message, color = if (message.startsWith("❌")) Color(0xFFFB7185) else Color(0xFF34D399), fontWeight = FontWeight.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameButton(
                "💡 提示下一个字母",
                Modifier.width(190.dp).height(42.dp),
                Color(0xFF7C3AED),
                Color(0xFF4C1D95),
                enabled = typed.length < target.length,
                onClick = ::revealNextLetter
            )
            GameButton(
                "👀 显示完整答案",
                Modifier.width(190.dp).height(42.dp),
                Color(0xFF475569),
                Color(0xFF1E293B),
                enabled = typed.length < target.length,
                onClick = ::revealAnswer
            )
        }
        TouchKeyboard(
            enabled = typed.length < target.length,
            onLetter = ::submitLetter,
            onBackspace = { if (typed.isNotEmpty()) typed = typed.dropLast(1) }
        )
        if (typed.length == target.length) {
            GameButton("完成本关 🏆", Modifier.width(190.dp).height(48.dp)) { onFinished(usedHint) }
        }
    }
}

@Composable
private fun TouchKeyboard(enabled: Boolean, onLetter: (Char) -> Unit, onBackspace: () -> Unit) {
    Column(
        Modifier.width(620.dp).background(Color(0xFF0F172A), RoundedCornerShape(14.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM").forEachIndexed { rowIndex, letters ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                letters.forEach { letter ->
                    Box(
                        Modifier.size(42.dp, 38.dp).background(Color(0xFF475569), RoundedCornerShape(7.dp))
                            .clickable(enabled) { onLetter(letter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(letter.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                if (rowIndex == 2) {
                    Box(
                        Modifier.size(74.dp, 38.dp).background(AppColors.Coral, RoundedCornerShape(7.dp))
                            .clickable(onClick = onBackspace),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("退格⌫", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun randomizedChoices(
    correct: String,
    alternatives: List<String>,
    seed: Int
): Pair<List<String>, Int> {
    val distractors = alternatives
        .filterNot { it.equals(correct, ignoreCase = true) }
        .distinct()
        .shuffled(Random(seed))
        .take(2)
    val options = (distractors + correct).shuffled(Random(seed xor 0x5F3759DF))
    return options to options.indexOf(correct)
}
