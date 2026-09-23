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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val KEYBOARD_ROUND_SIZE = 18
private const val SPACE_KEY_COUNT = 3

private enum class KeyboardGameType(val title: String, val instruction: String) {
    MOLE("荷叶打地鼠", "字母冒出时，按下对应键"),
    SHOOT("水滴击落", "字母下落时，按键将它击落"),
    FOLLOW("指法跟打", "跟随高亮提示逐个按键")
}

private data class KeyboardLevel(
    val title: String,
    val subtitle: String,
    val keys: String
)

private data class FingerGuide(
    val hand: String,
    val finger: String,
    val color: Color
)

private val keyboardLevels = listOf(
    KeyboardLevel("基准键位", "ASDF · JKL; · 空格", "ASDFJKL; "),
    KeyboardLevel("食指扩展", "加入 G / H 与空格", "ASDFGHJKL; "),
    KeyboardLevel("上排探索", "QWERTYUIOP · 空格", "QWERTYUIOP "),
    KeyboardLevel("下排探索", "ZXCVBNM · 空格", "ZXCVBNM "),
    KeyboardLevel("全键混合", "26 字母与空格综合", "ABCDEFGHIJKLMNOPQRSTUVWXYZ ")
)

private fun keyboardSequence(level: KeyboardLevel): List<Char> {
    val letters = level.keys.filterNot(Char::isWhitespace)
    return (
        List(KEYBOARD_ROUND_SIZE - SPACE_KEY_COUNT) {
            letters[Random.nextInt(letters.length)]
        } + List(SPACE_KEY_COUNT) { ' ' }
    ).shuffled()
}

private fun keyLabel(key: Char): String = if (key == ' ') "空格" else key.toString()

@Composable
fun FrogTrainingHome(
    store: StudentStore,
    onKeyboardLevel: (Int) -> Unit,
    onFlume: () -> Unit
) {
    Column(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF083B52), Color(0xFF087F8C), Color(0xFF16A6A1))
                )
            )
            .padding(24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(150.dp)
                .gamePanel(Color(0xD90B3B45), Color(0xFF5EEAD4), 20)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FrogSprite(Modifier.size(105.dp), jumpAmount = .15f)
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text("青蛙打字训练中心", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(
                    "先在键盘启蒙岛掌握指法，再去《激流勇进》挑战完整单词",
                    color = Color(0xFFCCFBF1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("启蒙岛最高正确率", color = Color(0xFF99F6E4), fontSize = 12.sp)
                Text("${store.keyboardBestAccuracy}%", color = AppColors.Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("累计 ${store.keyboardKeystrokes} 次击键", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("键盘启蒙岛 · 五级指法航线", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(270.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            keyboardLevels.forEachIndexed { index, level ->
                val unlocked = store.isKeyboardLevelUnlocked(index)
                val stars = store.keyboardLevelStars(index)
                KeyboardLevelCard(
                    index = index,
                    level = level,
                    unlocked = unlocked,
                    stars = stars,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    if (unlocked) onKeyboardLevel(index)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        val flumeUnlocked = store.keyboardLevelStars(0) > 0
        Row(
            Modifier.fillMaxWidth().weight(1f)
                .gamePanel(Color(0xD90B3B45), if (flumeUnlocked) AppColors.Gold else Color(0xFF475569), 18)
                .padding(horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌊", fontSize = 54.sp)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text("激流勇进", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text(
                    if (flumeUnlocked) "基础定位已掌握，可以挑战完整单词"
                    else "完成“基准键位”并获得 1 星后解锁",
                    color = if (flumeUnlocked) Color(0xFFCCFBF1) else Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
            GameButton(
                if (flumeUnlocked) "开始渡河" else "🔒 尚未解锁",
                Modifier.width(180.dp).height(52.dp),
                if (flumeUnlocked) Color(0xFF16A34A) else Color(0xFF475569),
                if (flumeUnlocked) Color(0xFF166534) else Color(0xFF334155),
                enabled = flumeUnlocked,
                onClick = onFlume
            )
        }
    }
}

@Composable
private fun KeyboardLevelCard(
    index: Int,
    level: KeyboardLevel,
    unlocked: Boolean,
    stars: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .gamePanel(
                if (unlocked) Color(0xE6113340) else Color(0xD91E293B),
                if (unlocked) Color(0xFF5EEAD4) else Color(0xFF475569),
                16
            )
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(15.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(38.dp).background(
                    if (unlocked) AppColors.Gold else Color(0xFF475569),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", color = AppColors.Navy, fontWeight = FontWeight.Black)
            }
            Text(if (unlocked) "可挑战" else "🔒", color = Color(0xFF99F6E4), fontSize = 11.sp)
        }
        Column {
            Text(level.title, color = if (unlocked) Color.White else Color(0xFF64748B), fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(level.subtitle, color = if (unlocked) Color(0xFF99F6E4) else Color(0xFF64748B), fontSize = 12.sp)
        }
        Text(
            buildString {
                repeat(3) { append(if (it < stars) "★" else "☆") }
            },
            color = if (stars > 0) AppColors.Gold else Color(0xFF64748B),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            if (stars > 0) "点击复习" else if (unlocked) "点击开始" else "完成前一关",
            color = if (unlocked) Color.White else Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun KeyboardIslandScreen(
    levelIndex: Int,
    store: StudentStore,
    success: () -> Unit,
    error: () -> Unit,
    onBack: () -> Unit
) {
    val level = keyboardLevels[levelIndex.coerceIn(keyboardLevels.indices)]
    val sequence = remember(levelIndex) {
        keyboardSequence(level)
    }
    var questionIndex by remember(levelIndex) { mutableIntStateOf(0) }
    var correctKeys by remember(levelIndex) { mutableIntStateOf(0) }
    var wrongKeys by remember(levelIndex) { mutableIntStateOf(0) }
    var timeouts by remember(levelIndex) { mutableIntStateOf(0) }
    var score by remember(levelIndex) { mutableIntStateOf(0) }
    var combo by remember(levelIndex) { mutableIntStateOf(0) }
    var lives by remember(levelIndex) { mutableIntStateOf(3) }
    var timeLeft by remember(levelIndex) { mutableIntStateOf(6) }
    var attemptRevision by remember(levelIndex) { mutableIntStateOf(0) }
    var failed by remember(levelIndex) { mutableStateOf(false) }
    var feedback by remember(levelIndex) {
        mutableStateOf("错键不扣生命；倒计时结束生命 -1、得分 -20")
    }
    var lastPressed by remember(levelIndex) { mutableStateOf<Char?>(null) }
    var lastWasWrong by remember(levelIndex) { mutableStateOf(false) }
    var completedStars by remember(levelIndex) { mutableStateOf<Int?>(null) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val target = sequence[questionIndex.coerceAtMost(sequence.lastIndex)]
    val mode = when (questionIndex / 6) {
        0 -> KeyboardGameType.MOLE
        1 -> KeyboardGameType.SHOOT
        else -> KeyboardGameType.FOLLOW
    }
    val guide = fingerGuide(target)
    val timeLimit = when (mode) {
        KeyboardGameType.MOLE -> 6
        KeyboardGameType.SHOOT -> 5
        KeyboardGameType.FOLLOW -> 7
    }

    fun handleKey(key: Char) {
        if (completedStars != null || failed) return
        val normalized = if (key.isLetter()) key.uppercaseChar() else key
        if (normalized !in "QWERTYUIOPASDFGHJKL;ZXCVBNM ") return
        lastPressed = normalized
        val isCorrect = normalized == target
        store.recordKeyboardKey(target, isCorrect)
        if (isCorrect) {
            success()
            correctKeys += 1
            val nextCombo = combo + 1
            combo = nextCombo
            score += 10 + (nextCombo.coerceAtMost(10) * 2)
            lastWasWrong = false
            feedback = "正确！${guide.hand}${guide.finger}按下${keyLabel(target)}"
            if (questionIndex == sequence.lastIndex) {
                val attempts = correctKeys + wrongKeys + timeouts
                val accuracy = (correctKeys * 100 / attempts.coerceAtLeast(1))
                completedStars = store.completeKeyboardLevel(levelIndex, accuracy, passed = true)
            } else {
                questionIndex += 1
            }
        } else {
            error()
            wrongKeys += 1
            combo = 0
            lastWasWrong = true
            attemptRevision += 1
            feedback = "正确指法：${guide.hand}${guide.finger}按${keyLabel(target)}；不扣生命，计时重新开始"
        }
        scope.launch {
            delay(260)
            lastPressed = null
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(questionIndex, attemptRevision, completedStars, failed) {
        if (completedStars != null || failed) return@LaunchedEffect
        timeLeft = timeLimit
        repeat(timeLimit) {
            delay(1000)
            if (completedStars != null || failed) return@LaunchedEffect
            timeLeft -= 1
        }
        error()
        store.recordKeyboardTimeout(target)
        timeouts += 1
        combo = 0
        score = (score - 20).coerceAtLeast(0)
        lastWasWrong = true
        val nextLives = (lives - 1).coerceAtLeast(0)
        lives = nextLives
        if (nextLives == 0) {
            failed = true
            val attempts = correctKeys + wrongKeys + timeouts
            val accuracy = correctKeys * 100 / attempts.coerceAtLeast(1)
            completedStars = store.completeKeyboardLevel(levelIndex, accuracy, passed = false)
            feedback = "连续超时导致生命耗尽，本关失败"
        } else {
            feedback = "${keyLabel(target)}超时，生命 -1、得分 -20；当前题重新开始"
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.nativeKeyEvent.repeatCount > 0) {
                    return@onPreviewKeyEvent false
                }
                val raw = event.nativeKeyEvent.unicodeChar.toChar()
                if (raw.isLetter() || raw == ';' || raw == ' ') {
                    handleKey(raw)
                    true
                } else {
                    false
                }
            },
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        KeyboardStatsPanel(
            levelIndex = levelIndex,
            level = level,
            questionIndex = questionIndex,
            correct = correctKeys,
            wrong = wrongKeys,
            timeouts = timeouts,
            lives = lives,
            timeLeft = timeLeft,
            score = score,
            combo = combo,
            modifier = Modifier.width(220.dp).fillMaxHeight()
        )

        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(
                Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("键盘启蒙岛 · ${level.title}", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(mode.title, color = AppColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                GameButton("返回训练中心", Modifier.width(145.dp).height(42.dp), Color(0xFF475569), Color(0xFF1E293B), onClick = onBack)
            }

            Box(
                Modifier.fillMaxWidth().height(296.dp)
                    .gamePanel(Color(0xB30B6073), Color(0xFF5EEAD4), 20)
            ) {
                KeyboardPondBackground()
                when {
                    completedStars != null -> KeyboardResultPanel(
                        stars = completedStars ?: 0,
                        failed = failed,
                        correct = correctKeys,
                        wrong = wrongKeys,
                        timeouts = timeouts,
                        score = score,
                        onRetry = {
                            questionIndex = 0
                            correctKeys = 0
                            wrongKeys = 0
                            timeouts = 0
                            score = 0
                            combo = 0
                            lives = 3
                            timeLeft = timeLimit
                            failed = false
                            attemptRevision += 1
                            completedStars = null
                            feedback = "错键不扣生命；倒计时结束生命 -1、得分 -20"
                        },
                        onBack = onBack
                    )
                    mode == KeyboardGameType.MOLE -> MoleChallenge(target)
                    mode == KeyboardGameType.SHOOT -> ShootChallenge(target, questionIndex, attemptRevision, timeLimit)
                    else -> FollowChallenge(sequence, questionIndex)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp)
                    .background(
                        if (lastWasWrong) Color(0xCC7F1D1D) else Color(0xCC0B3B45),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(feedback, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${guide.hand} · ${guide.finger} · ${keyLabel(target)}",
                    color = if (lastWasWrong) Color(0xFFFFE4A3) else guide.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(7.dp))
            FingerKeyboard(
                availableKeys = level.keys,
                target = target,
                pressed = lastPressed,
                wrongPressed = lastWasWrong,
                enabled = completedStars == null && !failed,
                onKey = ::handleKey
            )
        }
    }
}

@Composable
private fun KeyboardStatsPanel(
    levelIndex: Int,
    level: KeyboardLevel,
    questionIndex: Int,
    correct: Int,
    wrong: Int,
    timeouts: Int,
    lives: Int,
    timeLeft: Int,
    score: Int,
    combo: Int,
    modifier: Modifier
) {
    val accuracy = correct * 100 / (correct + wrong + timeouts).coerceAtLeast(1)
    Column(
        modifier.gamePanel(Color(0xD90B3B45), Color(0xFF5EEAD4), 18).padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text("键盘启蒙岛", color = Color(0xFF99F6E4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("第 ${levelIndex + 1} 关\n${level.title}", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(level.subtitle, color = AppColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(
            progress = { (questionIndex + 1) / KEYBOARD_ROUND_SIZE.toFloat() },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = AppColors.Gold,
            trackColor = Color(0xFF155E75)
        )
        Text("${questionIndex + 1} / $KEYBOARD_ROUND_SIZE", color = Color.White, fontWeight = FontWeight.Black)
        Column {
            Text("生命值", color = Color(0xFF99AEB7), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    Text(
                        "♥",
                        color = if (index < lives) Color(0xFFFB7185) else Color(0xFF475569),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        KeyboardStat("本题倒计时", "${timeLeft}s", if (timeLeft <= 2) Color(0xFFFDA4AF) else AppColors.Gold)
        KeyboardStat("本轮正确率", "$accuracy%", Color(0xFF86EFAC))
        KeyboardStat("本轮击键", "${correct + wrong}", Color(0xFF67E8F9))
        KeyboardStat("本轮超时", "$timeouts", if (timeouts > 0) Color(0xFFFDA4AF) else Color(0xFF99AEB7))
        KeyboardStat("连续正确", "×$combo", AppColors.Gold)
        Spacer(Modifier.weight(1f))
        Text("得分 $score", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun KeyboardStat(label: String, value: String, color: Color) {
    Column {
        Text(label, color = Color(0xFF99AEB7), fontSize = 11.sp)
        Text(value, color = color, fontSize = 21.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MoleChallenge(target: Char) {
    val pulse = rememberInfiniteTransition(label = "mole_pulse")
    val scale by pulse.animateFloat(
        initialValue = .9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "mole_scale"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        listOf(-255, 0, 255).forEachIndexed { index, x ->
            Box(
                Modifier.offset(x.dp, (if (index == 1) 35 else 65).dp)
                    .size(150.dp, 62.dp)
                    .background(Color(0xFF1FAD62), CircleShape)
                    .border(4.dp, Color(0xFF064E3B), CircleShape)
            )
        }
        FrogSprite(Modifier.align(Alignment.BottomStart).offset(70.dp, (-24).dp).size(78.dp), .05f)
        Box(
            Modifier.offset(y = (-34).dp).size((96 * scale).dp)
                .background(Color(0xFFFFF4CF), CircleShape)
                .border(5.dp, Color(0xFFF59E0B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(keyLabel(target), color = Color(0xFF082F36), fontSize = if (target == ' ') 26.sp else 48.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ShootChallenge(target: Char, questionIndex: Int, attemptRevision: Int, timeLimit: Int) {
    val fall = remember(questionIndex, attemptRevision) { Animatable(0f) }
    LaunchedEffect(questionIndex, attemptRevision) {
        while (true) {
            fall.snapTo(0f)
            fall.animateTo(1f, tween(timeLimit * 1000, easing = LinearEasing))
        }
    }
    val y = (-100f + fall.value * 330f).dp
    Box(Modifier.fillMaxSize()) {
        FrogSprite(Modifier.align(Alignment.BottomCenter).offset(y = (-18).dp).size(82.dp), .08f)
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = y)
                .size(88.dp)
                .background(Color(0xFFBAE6FD), CircleShape)
                .border(5.dp, Color(0xFF0284C7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(keyLabel(target), color = Color(0xFF0C4A6E), fontSize = if (target == ' ') 24.sp else 45.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun FollowChallenge(sequence: List<Char>, questionIndex: Int) {
    val start = 12
    val follow = sequence.drop(start).take(6)
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        follow.forEachIndexed { index, key ->
            val absoluteIndex = start + index
            val active = absoluteIndex == questionIndex
            val completed = absoluteIndex < questionIndex
            Box(
                Modifier.padding(horizontal = 7.dp)
                    .size(if (key == ' ') 116.dp else 82.dp, 82.dp)
                    .background(
                        when {
                            completed -> Color(0xFF16A34A)
                            active -> AppColors.Gold
                            else -> Color(0xFF334155)
                        },
                        RoundedCornerShape(15.dp)
                    )
                    .border(
                        if (active) 4.dp else 2.dp,
                        if (active) Color.White else Color(0xFF64748B),
                        RoundedCornerShape(15.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    keyLabel(key),
                    color = if (active) AppColors.Navy else Color.White,
                    fontSize = if (key == ' ') 21.sp else 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun KeyboardResultPanel(
    stars: Int,
    failed: Boolean,
    correct: Int,
    wrong: Int,
    timeouts: Int,
    score: Int,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val accuracy = correct * 100 / (correct + wrong + timeouts).coerceAtLeast(1)
    Column(
        Modifier.fillMaxSize().background(Color(0xE60B3B45), RoundedCornerShape(18.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            when {
                failed -> "生命耗尽，闯关失败"
                stars > 0 -> "指法训练完成"
                else -> "未达到 1 星，再练一次"
            },
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            buildString { repeat(3) { append(if (it < stars) "★" else "☆") } },
            color = if (stars > 0) AppColors.Gold else Color(0xFF64748B),
            fontSize = 38.sp
        )
        Text(
            "正确率 $accuracy% · 击键 ${correct + wrong} 次 · 超时 $timeouts 次 · 得分 $score",
            color = Color(0xFFCCFBF1),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameButton("再练一次", Modifier.width(145.dp).height(44.dp), Color(0xFF2563EB), Color(0xFF1E40AF), onClick = onRetry)
            GameButton("返回选关", Modifier.width(145.dp).height(44.dp), Color(0xFF16A34A), Color(0xFF166534), onClick = onBack)
        }
    }
}

@Composable
private fun FingerKeyboard(
    availableKeys: String,
    target: Char,
    pressed: Char?,
    wrongPressed: Boolean,
    enabled: Boolean,
    onKey: (Char) -> Unit
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        listOf("QWERTYUIOP", "ASDFGHJKL;", "ZXCVBNM").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    val available = key in availableKeys
                    val isTarget = key == target
                    val isWrong = key == pressed && wrongPressed
                    val guide = fingerGuide(key)
                    Box(
                        Modifier.size(58.dp, 48.dp)
                            .background(
                                when {
                                    isWrong -> Color(0xFFDC2626)
                                    isTarget -> guide.color
                                    available -> Color(0xFF334155)
                                    else -> Color(0x661E293B)
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isTarget) 3.dp else 1.dp,
                                if (isTarget) Color.White else Color(0xFF64748B),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = enabled && available) { onKey(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            key.toString(),
                            color = if (isTarget) Color(0xFF082F36) else if (available) Color.White else Color(0xFF64748B),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (key == 'F' || key == 'J') {
                            Box(
                                Modifier.align(Alignment.BottomCenter).offset(y = (-5).dp)
                                    .size(15.dp, 3.dp).background(
                                        if (isTarget) Color(0xFF082F36) else Color.White,
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
        val spaceAvailable = ' ' in availableKeys
        val spaceTarget = target == ' '
        val spaceWrong = pressed == ' ' && wrongPressed
        val spaceGuide = fingerGuide(' ')
        Box(
            Modifier.size(360.dp, 42.dp)
                .background(
                    when {
                        spaceWrong -> Color(0xFFDC2626)
                        spaceTarget -> spaceGuide.color
                        spaceAvailable -> Color(0xFF334155)
                        else -> Color(0x661E293B)
                    },
                    RoundedCornerShape(8.dp)
                )
                .border(
                    if (spaceTarget) 3.dp else 1.dp,
                    if (spaceTarget) Color.White else Color(0xFF64748B),
                    RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled && spaceAvailable) { onKey(' ') },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "空格",
                color = if (spaceTarget) Color(0xFF082F36) else if (spaceAvailable) Color.White else Color(0xFF64748B),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun KeyboardPondBackground() {
    val flow = rememberInfiniteTransition(label = "keyboard_pond")
    val phase by flow.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "pond_phase"
    )
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF0B6073), Color(0xFF0D8C98), Color(0xFF0EA5A2)))
        )
        repeat(8) { index ->
            val y = ((phase * size.height + index * 73f) % (size.height + 60f)) - 30f
            val wave = Path().apply {
                moveTo(size.width * ((index % 4) * .25f), y)
                quadraticBezierTo(
                    size.width * ((index % 4) * .25f + .08f),
                    y + 18f,
                    size.width * ((index % 4) * .25f + .16f),
                    y
                )
            }
            drawPath(wave, Color.White.copy(alpha = .12f), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

private fun fingerGuide(key: Char): FingerGuide = when (key.uppercaseChar()) {
    ' ' -> FingerGuide("双手", "拇指", Color(0xFFFDE047))
    'Q', 'A', 'Z' -> FingerGuide("左手", "小指", Color(0xFFF9A8D4))
    'W', 'S', 'X' -> FingerGuide("左手", "无名指", Color(0xFFC4B5FD))
    'E', 'D', 'C' -> FingerGuide("左手", "中指", Color(0xFF93C5FD))
    'R', 'F', 'V', 'T', 'G', 'B' -> FingerGuide("左手", "食指", Color(0xFF67E8F9))
    'Y', 'H', 'N', 'U', 'J', 'M' -> FingerGuide("右手", "食指", Color(0xFF6EE7B7))
    'I', 'K' -> FingerGuide("右手", "中指", Color(0xFFFDE68A))
    'O', 'L' -> FingerGuide("右手", "无名指", Color(0xFFFDBA74))
    else -> FingerGuide("右手", "小指", Color(0xFFFCA5A5))
}
