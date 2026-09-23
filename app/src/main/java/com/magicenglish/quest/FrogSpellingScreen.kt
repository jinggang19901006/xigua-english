package com.magicenglish.quest

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FrogSpellingScreen(
    unit: CourseUnit,
    practiceWords: List<Word>,
    store: StudentStore,
    speak: (String) -> Unit,
    success: () -> Unit,
    error: () -> Unit
) {
    val words = remember(unit.id, practiceWords) {
        practiceWords.shuffled(Random(System.nanoTime()))
    }
    var wordIndex by remember(unit.id) { mutableIntStateOf(0) }
    var typed by remember(unit.id) { mutableStateOf("") }
    var hearts by remember(unit.id) { mutableIntStateOf(3) }
    var score by remember(unit.id) { mutableIntStateOf(0) }
    var combo by remember(unit.id) { mutableIntStateOf(0) }
    var deliveredFrogs by remember(unit.id) { mutableIntStateOf(0) }
    var mistakesOnWord by remember(unit.id) { mutableIntStateOf(0) }
    var attemptRevision by remember(unit.id) { mutableIntStateOf(0) }
    var activeY by remember(unit.id) { mutableFloatStateOf(230f) }
    var feedback by remember(unit.id) { mutableStateOf("照着荷叶上的单词完整拼写") }
    var transitioning by remember(unit.id) { mutableStateOf(false) }
    var gameOver by remember(unit.id) { mutableStateOf(false) }
    var roundComplete by remember(unit.id) { mutableStateOf(false) }
    val word = words[wordIndex]
    val target = remember(word.id) { typingTarget(word.word) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val jumpProgress = remember(unit.id, practiceWords) { Animatable(0f) }

    fun finishRound(finalScore: Int) {
        store.completeFrogRound(finalScore)
        roundComplete = true
        transitioning = false
    }

    fun advanceAfterSuccess() {
        transitioning = true
        scope.launch {
            jumpProgress.snapTo(0f)
            jumpProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing)
            )
            val frogReachedBank = wordIndex == words.lastIndex
            if (frogReachedBank) {
                val nextDelivered = deliveredFrogs + 1
                deliveredFrogs = nextDelivered
                if (nextDelivered == 3) {
                    feedback = "3 只青蛙全部安全过河！"
                    finishRound(score)
                    jumpProgress.snapTo(0f)
                    return@launch
                }
                wordIndex = 0
                activeY = 230f
                feedback = "第 $nextDelivered 只青蛙已过河，第 ${nextDelivered + 1} 只出发"
            } else {
                activeY = (activeY - 150f).coerceAtLeast(92f)
                wordIndex += 1
                feedback = "落到新荷叶，继续拼写"
            }
            typed = ""
            mistakesOnWord = 0
            jumpProgress.snapTo(0f)
            transitioning = false
        }
    }

    fun submitLetter(letter: Char) {
        if (transitioning || gameOver || roundComplete || typed.length >= target.length) return
        val expected = target[typed.length]
        if (letter.lowercaseChar() == expected) {
            val nextTyped = typed + expected
            typed = nextTyped
            if (nextTyped.length == target.length) {
                success()
                store.recordFrogWordSolved(word)
                val earned = 100 + combo * 15 + ((340f - activeY).coerceAtLeast(0f) / 4).toInt()
                score += earned
                combo += 1
                mistakesOnWord = 0
                feedback = "完整拼对！青蛙向上跳"
                advanceAfterSuccess()
            }
        } else {
            error()
            combo = 0
            typed = ""
            score = (score - 10).coerceAtLeast(0)
            val nextMistakes = mistakesOnWord + 1
            mistakesOnWord = nextMistakes
            val keyName = if (letter == ' ') "空格" else letter.uppercaseChar().toString()
            store.recordFrogMistake(word)
            if (nextMistakes >= 3) {
                val nextHearts = (hearts - 1).coerceAtLeast(0)
                hearts = nextHearts
                mistakesOnWord = 0
                activeY = 230f
                attemptRevision += 1
                store.recordFrogLifeLost()
                feedback = "连续错键 3 次，生命 -1、得分 -10"
                if (nextHearts == 0) {
                    gameOver = true
                    store.recordFrogFailure()
                }
            } else {
                feedback = "$keyName 输入错误（$nextMistakes / 3），得分 -10，本词重拼"
            }
        }
    }

    fun restart() {
        wordIndex = 0
        typed = ""
        hearts = 3
        score = 0
        combo = 0
        deliveredFrogs = 0
        mistakesOnWord = 0
        attemptRevision += 1
        activeY = 230f
        feedback = "照着荷叶上的单词完整拼写"
        transitioning = false
        gameOver = false
        roundComplete = false
        scope.launch { jumpProgress.snapTo(0f) }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(wordIndex, gameOver, roundComplete, transitioning, attemptRevision) {
        if (gameOver || roundComplete || transitioning) return@LaunchedEffect
        while (activeY < 340f) {
            delay(16)
            activeY += 0.1024f
        }
        error()
        store.recordFrogMistake(word)
        store.recordFrogLifeLost()
        combo = 0
        score = (score - 50).coerceAtLeast(0)
        mistakesOnWord = 0
        val nextHearts = (hearts - 1).coerceAtLeast(0)
        hearts = nextHearts
        feedback = "荷叶沉底，生命 -1、得分 -50；当前词重新挑战"
        if (nextHearts == 0) {
            gameOver = true
            store.recordFrogFailure()
        } else {
            typed = ""
            activeY = 230f
            attemptRevision += 1
        }
    }

    Row(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF083B52), Color(0xFF087F8C), Color(0xFF16A6A1))
                )
            )
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.nativeKeyEvent.repeatCount > 0) {
                    return@onPreviewKeyEvent false
                }
                val char = event.nativeKeyEvent.unicodeChar.toChar()
                if (char.isLetter() || char == ' ') {
                    submitLetter(char)
                    true
                } else {
                    false
                }
            },
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FallingStatsPanel(
            unit, wordIndex, words.size, hearts, deliveredFrogs,
            mistakesOnWord, score, combo, store.frogBestScore
        )

        Column(
            Modifier.weight(1f).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FrogSprite(Modifier.size(46.dp), jumpAmount = 0f)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("激流勇进", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text("一次无错拼完整词，帮助青蛙逆流过河", color = Color(0xFFCCFBF1), fontSize = 12.sp)
                    }
                }
                GameButton("🔊 当前单词", Modifier.width(145.dp).height(44.dp), AppColors.Sky, Color(0xFF0369A1)) {
                    speak(word.word)
                }
            }

            Box(
                Modifier.fillMaxWidth().height(390.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, Color(0x665EEAD4), RoundedCornerShape(20.dp))
            ) {
                RiverCurrentLayer()
                Column(
                    Modifier.align(Alignment.TopEnd).padding(12.dp)
                        .background(Color(0xB30B3B45), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("已过河", color = Color(0xFF99F6E4), fontSize = 10.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) { index ->
                            FrogSprite(
                                Modifier.size(27.dp).graphicsLayer {
                                    alpha = if (index < deliveredFrogs) 1f else .2f
                                },
                                jumpAmount = 0f
                            )
                        }
                    }
                }

                for (offset in 2 downTo 0) {
                    val index = wordIndex + offset
                    if (index < words.size) {
                        val y = activeY - offset * 150f
                        if (y > -85f && y < 410f) {
                            FallingWordPad(
                                word = words[index],
                                active = offset == 0,
                                typedCount = if (offset == 0) typed.length else 0,
                                x = padX(index),
                                y = y
                            )
                        }
                    }
                }

                val progress = jumpProgress.value
                val currentX = padX(wordIndex) + 88f
                val nextX = padX((wordIndex + 1).coerceAtMost(words.lastIndex)) + 88f
                val frogX = currentX + (nextX - currentX) * progress
                val frogY = activeY - 28f - 150f * progress -
                    sin(progress * PI).toFloat() * 58f
                FrogSprite(
                    Modifier.offset(frogX.dp, frogY.dp).size(68.dp),
                    jumpAmount = sin(progress * PI).toFloat()
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 9.dp)
                    .background(Color(0x66134E5E), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    feedback,
                    Modifier.weight(1f),
                    color = if (feedback.contains("错误") || feedback.contains("-1")) Color(0xFFFFB4B4) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "荷叶填充 ${typed.length} / ${target.length}",
                    color = if (typed.isNotEmpty()) AppColors.Gold else Color(0xFFCCFBF1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            when {
                roundComplete -> FallingResultPanel(
                    "3 只青蛙全部过河",
                    "每只 ${words.size} 词 · 共完成 ${words.size * 3} 次拼写 · $score 分 · 生命 $hearts",
                    "再玩一轮",
                    AppColors.Gold,
                    ::restart
                )
                gameOver -> FallingResultPanel(
                    "生命耗尽，闯关失败",
                    "已送达 $deliveredFrogs / 3 只 · 第 ${wordIndex + 1} / ${words.size} 词 · $score 分",
                    "重新挑战",
                    AppColors.Coral,
                    ::restart
                )
                else -> FallingKeyboard(enabled = !transitioning, onLetter = ::submitLetter)
            }
        }
    }
}

@Composable
private fun FallingStatsPanel(
    unit: CourseUnit,
    index: Int,
    total: Int,
    hearts: Int,
    deliveredFrogs: Int,
    mistakesOnWord: Int,
    score: Int,
    combo: Int,
    best: Int
) {
    Column(
        Modifier.width(205.dp).fillMaxHeight()
            .gamePanel(Color(0xD90B3B45), Color(0xFF5EEAD4), 18)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("当前单元", color = Color(0xFF99F6E4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Unit ${unit.number}\n${unit.title}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        LinearProgressIndicator(
            progress = { (index + 1) / total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = AppColors.Gold,
            trackColor = Color(0xFF155E75)
        )
        Text("${index + 1} / $total 个随机练习词", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            "第 ${(deliveredFrogs + 1).coerceAtMost(3)} 只青蛙",
            color = AppColors.Gold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
        Text("完整词库 ${unit.words.size} 词", color = Color(0xFF99F6E4), fontSize = 11.sp)
        FrogLives(hearts)
        Text("已送过河 $deliveredFrogs / 3 只", color = Color(0xFF86EFAC), fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text("当前词错键 $mistakesOnWord / 3", color = if (mistakesOnWord > 0) Color(0xFFFDA4AF) else Color(0xFF99AEB7), fontSize = 11.sp)
        FallingStat("本轮得分", "$score", AppColors.Gold)
        FallingStat("连续拼对", "×$combo", Color(0xFF86EFAC))
        Spacer(Modifier.weight(1f))
        Text("最高分 $best", color = Color(0xFFCCFBF1), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FrogLives(hearts: Int) {
    Column {
        Text("生命值", color = Color(0xFF99AEB7), fontSize = 11.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(3) { index ->
                FrogSprite(
                    Modifier.size(34.dp).graphicsLayer {
                        alpha = if (index < hearts) 1f else 0.22f
                        scaleX = if (index < hearts) 1f else 0.84f
                        scaleY = scaleX
                    },
                    jumpAmount = 0f
                )
            }
        }
    }
}

@Composable
private fun FallingStat(label: String, value: String, color: Color) {
    Column {
        Text(label, color = Color(0xFF99AEB7), fontSize = 11.sp)
        Text(value, color = color, fontSize = 21.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun FallingWordPad(
    word: Word,
    active: Boolean,
    typedCount: Int,
    x: Int,
    y: Float
) {
    val drift = rememberInfiniteTransition(label = "pad_drift_$x")
    val sway by drift.animateFloat(
        initialValue = -1.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100 + x, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pad_sway"
    )
    val pulse by drift.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pad_pulse"
    )

    Box(
        Modifier.offset(x.dp, y.dp).width(255.dp).height(104.dp)
            .graphicsLayer { rotationZ = sway }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            fun padPath(verticalOffset: Float): Path {
                val centerX = size.width * 0.5f
                val centerY = size.height * 0.55f + verticalOffset
                val radiusX = size.width * 0.48f
                val radiusY = size.height * 0.38f
                return Path().apply {
                    moveTo(centerX, centerY)
                    for (step in 0..56) {
                        val angle = Math.toRadians(18.0 + step * (324.0 / 56.0))
                        val px = centerX + cos(angle).toFloat() * radiusX
                        val py = centerY + sin(angle).toFloat() * radiusY
                        if (step == 0) lineTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
            }

            val shadow = padPath(7.dp.toPx())
            val leaf = padPath(0f)
            drawPath(shadow, Color(0x5500222A))
            if (active) {
                drawPath(
                    leaf,
                    Color(0xFF5EEAD4).copy(alpha = 0.16f + pulse * 0.2f),
                    style = Stroke(width = 11.dp.toPx())
                )
            }
            drawPath(
                leaf,
                Brush.linearGradient(
                    colors = if (active) {
                        listOf(Color(0xFF78E36D), Color(0xFF1FAD62), Color(0xFF087F5B))
                    } else {
                        listOf(Color(0xFF58CFA0), Color(0xFF159477), Color(0xFF096B68))
                    }
                )
            )
            drawPath(
                leaf,
                if (active) Color(0xFF064E3B) else Color(0xFF075985),
                style = Stroke(width = if (active) 5.dp.toPx() else 3.5.dp.toPx())
            )

            val veinStartX = size.width * 0.5f
            val veinStartY = size.height * 0.55f
            listOf(75.0, 125.0, 180.0, 235.0, 285.0).forEach { degrees ->
                val angle = Math.toRadians(degrees)
                drawLine(
                    color = Color.White.copy(alpha = if (active) 0.2f else 0.13f),
                    start = androidx.compose.ui.geometry.Offset(veinStartX, veinStartY),
                    end = androidx.compose.ui.geometry.Offset(
                        veinStartX + cos(angle).toFloat() * size.width * 0.35f,
                        veinStartY + sin(angle).toFloat() * size.height * 0.27f
                    ),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            val multiline = word.word.length > 14 && word.word.contains(' ')
            val labelWidth = (0.36f + word.word.length * .032f).coerceIn(.48f, .84f)
            val labelTop = if (multiline) size.height * .19f else size.height * .3f
            val labelHeight = if (multiline) size.height * .65f else size.height * .46f
            drawRoundRect(
                color = Color(0xFF082F36).copy(alpha = if (active) .86f else .72f),
                topLeft = androidx.compose.ui.geometry.Offset(size.width * (1f - labelWidth) / 2f, labelTop),
                size = androidx.compose.ui.geometry.Size(size.width * labelWidth, labelHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
            )
        }

        Box(
            Modifier.align(Alignment.Center).width(238.dp).height(96.dp),
            contentAlignment = Alignment.Center
        ) {
            val (displayWord, prefixEnd) = progressDisplay(wrapSourceForLeaf(word.word), typedCount)
            val longestLine = displayWord.lineSequence().maxOf { it.length }
            val textSize = when {
                longestLine <= 8 -> 28.sp
                longestLine <= 12 -> 25.sp
                longestLine <= 18 -> 21.sp
                longestLine <= 24 -> 18.sp
                else -> 16.sp
            }
            Text(
                displayWord,
                color = Color(0xFF102A2D),
                style = TextStyle(
                    fontSize = textSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                    drawStyle = Stroke(width = if (active) 4.3f else 3.8f)
                ),
                maxLines = 2
            )
            Text(
                displayWord,
                color = if (active) Color(0xFFFFFFE8) else Color(0xFFFFF0B0),
                style = TextStyle(
                    fontSize = textSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                    drawStyle = Stroke(width = if (active) 1.8f else 1.5f)
                ),
                maxLines = 2
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFFFA600))) {
                        append(displayWord.take(prefixEnd))
                    }
                    withStyle(SpanStyle(color = Color.Transparent)) {
                        append(displayWord.drop(prefixEnd))
                    }
                },
                fontSize = textSize,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun RiverCurrentLayer() {
    val flow = rememberInfiniteTransition(label = "river_flow")
    val phase by flow.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "current_phase"
    )
    val shimmer by flow.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "water_shimmer"
    )

    Canvas(
        Modifier.fillMaxSize()
            .background(Color(0xFF087B88), RoundedCornerShape(20.dp))
    ) {
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF0B6073), Color(0xFF0D8C98), Color(0xFF0EA5A2))
            )
        )
        repeat(9) { index ->
            val x = size.width * (0.08f + (index % 5) * 0.21f)
            val startY = ((index * 83f + phase * (size.height + 150f)) % (size.height + 150f)) - 120f
            val current = Path().apply {
                moveTo(x, startY)
                cubicTo(
                    x - 28f, startY + 45f,
                    x + 34f, startY + 82f,
                    x + 4f, startY + 132f
                )
            }
            drawPath(
                current,
                color = Color.White.copy(alpha = 0.08f + shimmer * 0.08f),
                style = Stroke(width = (2f + index % 3).dp.toPx())
            )
        }
        repeat(4) { index ->
            val y = ((phase * size.height * 1.35f + index * size.height * 0.31f) % size.height)
            drawArc(
                color = Color(0xFFB8FFF3).copy(alpha = 0.12f + shimmer * 0.1f),
                startAngle = 8f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(size.width * (0.12f + index * 0.21f), y),
                size = androidx.compose.ui.geometry.Size(76.dp.toPx(), 22.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun FrogSprite(modifier: Modifier, jumpAmount: Float) {
    Canvas(
        modifier.graphicsLayer {
            val stretch = 1f + jumpAmount * 0.13f
            scaleX = 1f - jumpAmount * 0.07f
            scaleY = stretch
            rotationZ = sin(jumpAmount * PI).toFloat() * 4f
        }
    ) {
        val w = size.width
        val h = size.height
        drawOval(
            Color(0x33002028),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.82f),
            size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.12f)
        )
        drawOval(
            Color(0xFF177A45),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.02f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.29f)
        )
        drawOval(
            Color(0xFF177A45),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.29f)
        )
        drawOval(
            Brush.verticalGradient(listOf(Color(0xFF72E56B), Color(0xFF20B85D))),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.19f, h * 0.24f),
            size = androidx.compose.ui.geometry.Size(w * 0.62f, h * 0.62f)
        )
        drawCircle(Color(0xFF54D765), radius = w * 0.19f, center = androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.24f))
        drawCircle(Color(0xFF54D765), radius = w * 0.19f, center = androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.24f))
        drawCircle(Color.White, radius = w * 0.115f, center = androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.22f))
        drawCircle(Color.White, radius = w * 0.115f, center = androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.22f))
        drawCircle(Color(0xFF12313A), radius = w * 0.055f, center = androidx.compose.ui.geometry.Offset(w * 0.34f, h * 0.23f))
        drawCircle(Color(0xFF12313A), radius = w * 0.055f, center = androidx.compose.ui.geometry.Offset(w * 0.66f, h * 0.23f))
        drawCircle(Color.White, radius = w * 0.018f, center = androidx.compose.ui.geometry.Offset(w * 0.355f, h * 0.205f))
        drawCircle(Color.White, radius = w * 0.018f, center = androidx.compose.ui.geometry.Offset(w * 0.675f, h * 0.205f))
        drawCircle(Color(0xFFFFA6A6), radius = w * 0.045f, center = androidx.compose.ui.geometry.Offset(w * 0.27f, h * 0.53f))
        drawCircle(Color(0xFFFFA6A6), radius = w * 0.045f, center = androidx.compose.ui.geometry.Offset(w * 0.73f, h * 0.53f))
        val smile = Path().apply {
            moveTo(w * 0.36f, h * 0.52f)
            quadraticBezierTo(w * 0.5f, h * 0.67f, w * 0.64f, h * 0.52f)
        }
        drawPath(smile, Color(0xFF14532D), style = Stroke(width = (w * 0.035f)))
    }
}

@Composable
private fun FallingKeyboard(
    enabled: Boolean,
    onLetter: (Char) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(top = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM").forEachIndexed { rowIndex, letters ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                letters.forEach { letter ->
                    Box(
                        Modifier.size(43.dp, 34.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(7.dp))
                            .border(1.dp, Color(0xFF64748B), RoundedCornerShape(7.dp))
                            .clickable(enabled = enabled) { onLetter(letter) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(letter.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (rowIndex == 2) {
                    Box(
                        Modifier.size(128.dp, 34.dp)
                            .background(Color(0xFF0E7490), RoundedCornerShape(7.dp))
                            .border(1.5.dp, Color(0xFF67E8F9), RoundedCornerShape(7.dp))
                            .clickable(enabled = enabled) { onLetter(' ') },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("空格  ␣", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun FallingResultPanel(
    title: String,
    detail: String,
    button: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        Modifier.padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Text(detail, color = Color(0xFFCCFBF1), fontSize = 12.sp)
        Spacer(Modifier.height(7.dp))
        GameButton(button, Modifier.width(175.dp).height(43.dp), color, Color(0xFF0F766E), onClick = onClick)
    }
}

private fun padX(index: Int): Int = when (index % 3) {
    0 -> 65
    1 -> 360
    else -> 650
}

private fun typingTarget(source: String): String = buildString {
    var previousWasSpace = false
    source.trim().forEach { char ->
        when {
            char.isLetter() -> {
                append(char.lowercaseChar())
                previousWasSpace = false
            }
            char.isWhitespace() && isNotEmpty() && !previousWasSpace -> {
                append(' ')
                previousWasSpace = true
            }
        }
    }
}.trim()

private fun wrapSourceForLeaf(source: String): String {
    val trimmed = source.trim()
    val spaces = trimmed.indices.filter { trimmed[it].isWhitespace() }
    val expandedLength = trimmed.length + spaces.size * 2
    if (expandedLength <= 18 || spaces.isEmpty()) return trimmed

    fun visualLength(value: String): Int =
        value.length + value.count { it.isWhitespace() } * 2

    val splitIndex = spaces.minBy { index ->
        maxOf(
            visualLength(trimmed.substring(0, index)) + 3,
            visualLength(trimmed.substring(index + 1))
        )
    }
    return trimmed.substring(0, splitIndex + 1) + "\n" + trimmed.substring(splitIndex + 1)
}

private fun progressDisplay(source: String, typedCharacters: Int): Pair<String, Int> {
    val display = StringBuilder()
    var targetIndex = 0
    var prefixEnd = 0
    var previousWasSpace = false
    source.trim().forEach { char ->
        when {
            char == '\n' -> {
                display.append('\n')
                previousWasSpace = false
            }
            char.isLetter() -> {
                display.append(char)
                targetIndex += 1
                previousWasSpace = false
            }
            char.isWhitespace() && !previousWasSpace -> {
                display.append(" ␣ ")
                targetIndex += 1
                previousWasSpace = true
            }
            !char.isWhitespace() -> display.append(char)
        }
        if (targetIndex <= typedCharacters) prefixEnd = display.length
    }
    return display.toString() to prefixEnd
}
