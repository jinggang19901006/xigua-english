package com.magicenglish.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

@Composable
fun GrammarScreen(
    points: List<GrammarPoint>,
    store: StudentStore,
    success: () -> Unit,
    error: () -> Unit,
    onComplete: () -> Unit
) {
    var pointIndex by remember { mutableIntStateOf(0) }
    var phase by remember { mutableIntStateOf(1) }
    var feedback by remember { mutableStateOf("") }
    val point = points[pointIndex]
    val submissionGate = remember(point.id, phase) { AtomicBoolean(false) }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF101A2E)).padding(horizontal = 90.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth().gamePanel(AppColors.Panel, Color(0xFF6D3E9C), 16)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("💡 语法思考工坊 ${pointIndex + 1} / ${points.size}", color = Color(0xFFD8B4FE), fontWeight = FontWeight.Black)
                Text(point.title, color = Color.White, fontSize = 14.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("1. 情境发现", phase == 1)
                Pill("2. 规则秘籍", phase == 2)
                Pill("3. 主动实战", phase == 3)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.width(900.dp).gamePanel(AppColors.Panel, Color(0xFF8B5CF6), 22)
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (phase) {
                    1 -> {
                        Text("先观察情境，再自己发现规律", color = AppColors.Gold, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            point.examples.forEach { (speaker, sentence) ->
                                Column(
                                    Modifier.width(390.dp).background(Color(0xFF0F172A), RoundedCornerShape(14.dp)).padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(speaker, color = Color(0xFF7DD3FC), fontWeight = FontWeight.Bold)
                                    Text(sentence, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        Text(point.question, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        point.options.forEachIndexed { index, option ->
                            AnswerRow(option) {
                                if (!submissionGate.compareAndSet(false, true)) return@AnswerRow
                                val correct = index == point.answer
                                store.recordGrammar(point.id, "discover", correct)
                                if (correct) {
                                    success()
                                    feedback = "发现得非常敏锐！"
                                    phase = 2
                                } else {
                                    error()
                                    feedback = "再对比两个句子的主语"
                                    submissionGate.set(false)
                                }
                            }
                        }
                    }
                    2 -> {
                        Text("📜 语法通关秘籍", color = Color(0xFFD8B4FE), fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(
                            point.formula,
                            Modifier.background(Color(0xFF312E81), RoundedCornerShape(14.dp)).padding(20.dp),
                            color = AppColors.Gold,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(point.mnemonic, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("先理解规律，再进入主动输出。", color = AppColors.Muted)
                        GameButton("开始实战 ➜", Modifier.width(190.dp).height(52.dp), AppColors.Purple, Color(0xFF6B21A8)) {
                            phase = 3
                        }
                    }
                    else -> {
                        Text("🎯 主动输出挑战", color = AppColors.Gold, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text(point.practice, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            point.practiceOptions.forEachIndexed { index, option ->
                                GameButton(
                                    option,
                                    Modifier.width(170.dp).height(58.dp),
                                    Color(0xFF475569),
                                    Color(0xFF1E293B)
                                ) {
                                    if (!submissionGate.compareAndSet(false, true)) return@GameButton
                                    val correct = index == point.practiceAnswer
                                    store.recordGrammar(point.id, "practice", correct)
                                    if (correct) {
                                        success()
                                        if (pointIndex == points.lastIndex) onComplete()
                                        else {
                                            pointIndex += 1
                                            phase = 1
                                            feedback = ""
                                        }
                                    } else {
                                        error()
                                        feedback = point.explanation
                                        submissionGate.set(false)
                                    }
                                }
                            }
                        }
                    }
                }
                Text(feedback, color = if (feedback.startsWith("再")) Color(0xFFFB7185) else Color(0xFF34D399), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AnswerRow(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(Color(0xFF334155), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF52647E), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(15.dp)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BossScreen(
    words: List<Word>,
    store: StudentStore,
    speak: (String) -> Unit,
    success: () -> Unit,
    error: () -> Unit,
    onComplete: () -> Unit
) {
    val requiredHits = minOf(5, words.size)
    var bossHp by remember { mutableIntStateOf(100) }
    var playerHp by remember { mutableIntStateOf(100) }
    var question by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var battleOver by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    val word = words[question % words.size]
    val choices = remember(word.id) {
        val distractors = words
            .filter { it.id != word.id }
            .map { it.meaning }
            .distinct()
            .shuffled(Random(word.id.hashCode()))
            .take(2)
        (distractors + word.meaning).shuffled(Random(word.id.hashCode() xor 0x5F3759DF))
    }
    val correctIndex = choices.indexOf(word.meaning)
    val submissionGate = remember(question) { AtomicBoolean(false) }
    val nextQuestionGate = remember(question) { AtomicBoolean(false) }

    fun resetBattle() {
        bossHp = 100
        playerHp = 100
        question = 0
        correctCount = 0
        selectedAnswer = null
        battleOver = false
        feedback = ""
    }

    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF3B0764), Color(0xFF111827), Color(0xFF020617)))
        ).padding(horizontal = 80.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            HealthBar("勇者 Lucas", playerHp, Color(0xFF22C55E))
            HealthBar("时空遗忘巨兽 Boss", bossHp, Color(0xFFF43F5E))
        }
        Row(
            Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛡️", fontSize = 86.sp)
                Text("英语小勇者", color = Color(0xFF7DD3FC), fontWeight = FontWeight.Black)
            }
            Column(
                Modifier.width(600.dp).gamePanel(Color(0xE6221835), Color(0xFFF59E0B), 24).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    if (battleOver) "挑战失败 · 重新整装再战" else "第 ${question + 1} / ${words.size} 题 · 答对 $requiredHits 题可击败 Boss",
                    color = Color(0xFFFDA4AF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                GameButton("🔊 听音识义", Modifier.width(180.dp).height(50.dp), AppColors.Sky, Color(0xFF1272A0)) {
                    speak(word.word)
                }
                Text(word.phonetic, color = AppColors.Muted, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    choices.forEachIndexed { index, choice ->
                        val isCorrect = index == correctIndex
                        val buttonColor = when {
                            selectedAnswer == null -> Color(0xFF475569)
                            isCorrect -> Color(0xFF15803D)
                            selectedAnswer == index -> Color(0xFFBE123C)
                            else -> Color(0xFF334155)
                        }
                        GameButton(
                            choice,
                            Modifier.width(170.dp).height(64.dp),
                            buttonColor,
                            Color(0xFF1E293B),
                            enabled = !battleOver
                        ) {
                            if (!submissionGate.compareAndSet(false, true)) return@GameButton
                            selectedAnswer = index
                            store.recordBossAnswer(word, isCorrect)
                            if (isCorrect) {
                                success()
                                val nextCorrect = correctCount + 1
                                correctCount = nextCorrect
                                bossHp = ((requiredHits - nextCorrect) * 100 / requiredHits).coerceAtLeast(0)
                                feedback = "命中！本题已锁定，请进入下一题"
                                if (nextCorrect >= requiredHits) {
                                    store.recordBossResult(true)
                                    onComplete()
                                }
                            } else {
                                error()
                                val nextHp = (playerHp - 34).coerceAtLeast(0)
                                playerHp = nextHp
                                feedback = "答错了，本题不能重复作答"
                                if (nextHp == 0 || question == words.lastIndex) {
                                    battleOver = true
                                    store.recordBossResult(false)
                                }
                            }
                        }
                    }
                }
                Text(
                    feedback,
                    color = if (selectedAnswer != null && selectedAnswer != correctIndex) Color(0xFFFB7185) else AppColors.Gold,
                    fontWeight = FontWeight.Bold
                )
                when {
                    battleOver -> GameButton(
                        "↻ 重新挑战",
                        Modifier.width(190.dp).height(48.dp),
                        AppColors.Coral,
                        Color(0xFF9F1239),
                        onClick = ::resetBattle
                    )
                    selectedAnswer != null && correctCount < requiredHits -> GameButton(
                        "下一题 ➜",
                        Modifier.width(180.dp).height(48.dp)
                    ) {
                        if (!nextQuestionGate.compareAndSet(false, true)) return@GameButton
                        if (question == words.lastIndex) {
                            battleOver = true
                            store.recordBossResult(false)
                        } else {
                            question += 1
                            selectedAnswer = null
                            feedback = ""
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🐉", fontSize = 112.sp)
                Text("遗忘兽", color = Color(0xFFFDA4AF), fontWeight = FontWeight.Black)
            }
        }
        Text("每题只能提交一次 · 连错 3 题挑战失败 · 正确答案位置会随机变化", color = Color(0xFFA78BFA), fontSize = 12.sp)
    }
}

@Composable
private fun HealthBar(title: String, hp: Int, color: Color) {
    Column(Modifier.width(360.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White, fontWeight = FontWeight.Black)
            Text("$hp / 100", color = color, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { hp / 100f },
            modifier = Modifier.fillMaxWidth().height(16.dp),
            color = color,
            trackColor = Color(0xFF334155)
        )
    }
}

@Composable
fun SettlementDialog(reward: StageReward, onMap: () -> Unit, onNext: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.72f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(620.dp).gamePanel(Color(0xFF1E293B), AppColors.Gold, 26)
                .padding(horizontal = 42.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                if (reward.firstClear) "🏆 首次通关！" else "✅ 复习完成！",
                color = AppColors.Gold,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                if (reward.firstClear) "奖励已存入账户，继续保持稳定学习。" else "本关奖励已领取，本次复习不会重复发放。",
                color = Color.White,
                fontSize = 17.sp
            )
            Text("⭐  ⭐  ⭐", fontSize = 48.sp)
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF0F172A), RoundedCornerShape(14.dp)).padding(18.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Reward("学习积分", "+${reward.points}")
                Reward("探险金币", "+${reward.coins}")
                Reward(if (reward.firstClear) "新勋章" else "复习记录", reward.medal)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                GameButton("🗺 返回地图", Modifier.width(220.dp).height(54.dp), Color(0xFF475569), Color(0xFF1E293B), onClick = onMap)
                GameButton("🚀 继续下一关", Modifier.width(220.dp).height(54.dp), onClick = onNext)
            }
        }
    }
}

@Composable
private fun Reward(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AppColors.Muted, fontSize = 12.sp)
        Text(value, color = AppColors.Gold, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}
