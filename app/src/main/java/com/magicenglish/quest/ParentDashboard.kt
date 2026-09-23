package com.magicenglish.quest

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ParentDashboard(
    store: StudentStore,
    book: CourseBook,
    unit: CourseUnit,
    onBack: () -> Unit
) {
    val bookWords = book.units.flatMap { it.words }
    val mastered = bookWords.count { store.mastered[it.id] == true }
    val overallRate = store.correctAnswers * 100 / store.totalAnswers.coerceAtLeast(1)

    Column(
        Modifier.fillMaxSize().background(Color(0xFF020617)).padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👨‍👩‍👧", fontSize = 36.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("家长学情监控中心 · 横屏仪表盘", color = Color(0xFFD8B4FE), fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("${book.displayName} · Unit ${unit.number} ${unit.title} · 数据保存在平板本地", color = AppColors.Muted, fontSize = 12.sp)
                }
            }
            GameButton("返回闯关地图", Modifier.width(145.dp).height(46.dp), Color(0xFF475569), Color(0xFF1E293B), onClick = onBack)
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Kpi("今日学习时长", "${store.todayMinutes} 分钟", "每日建议 15–20 分钟", Color(0xFF38BDF8), Modifier.weight(1f))
            Kpi("本册词汇掌握率", "$mastered / ${bookWords.size}", "${mastered * 100 / bookWords.size.coerceAtLeast(1)}% 已掌握", Color(0xFF34D399), Modifier.weight(1f))
            Kpi("本册通关进度", "${store.completedUnits(book)} 单元", "共 ${book.units.size} 个核心单元", AppColors.Gold, Modifier.weight(1f))
            Kpi("累计答题正确率", "$overallRate%", "${store.correctAnswers} / ${store.totalAnswers} 次答对", Color(0xFFC084FC), Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                LearningOverview(store, Modifier.weight(.9f))
                ErrorWords(bookWords, store, Modifier.weight(1.15f))
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    Modifier.fillMaxWidth().weight(.9f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    KeyboardReport(store, Modifier.weight(1f).fillMaxHeight())
                    GrammarReport(unit.grammar, store, Modifier.weight(1f).fillMaxHeight())
                }
                ReviewQueue(bookWords, store, Modifier.weight(1.1f))
            }
        }
    }
}

@Composable
private fun Kpi(title: String, value: String, note: String, color: Color, modifier: Modifier) {
    Column(modifier.gamePanel(Color(0xFF111C31), color.copy(.45f), 14).padding(16.dp)) {
        Text(title, color = AppColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(note, color = color.copy(.88f), fontSize = 11.sp)
    }
}

@Composable
private fun LearningOverview(store: StudentStore, modifier: Modifier) {
    Column(modifier.gamePanel(Color(0xFF111C31), Color(0xFF26354B), 14).padding(18.dp)) {
        Text("🏆 学习成果与奖励账户", color = Color.White, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccountValue("累计积分", "${store.points}", Color(0xFF38BDF8), Modifier.weight(1f))
            AccountValue("金币余额", "${store.coins}", AppColors.Gold, Modifier.weight(1f))
            AccountValue("Boss 战绩", "${store.bossWins}/${store.bossBattles}", Color(0xFF34D399), Modifier.weight(1f))
            AccountValue("荷叶最高分", "${store.frogBestScore}", Color(0xFFC084FC), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text("激流勇进：成功 ${store.frogRounds} 轮 / 失败 ${store.frogFailures} 轮 / 损失 ${store.frogLivesLost} 生命", color = AppColors.Muted, fontSize = 11.sp)
        Text("键盘启蒙岛：${store.keyboardRounds} 轮 / 失败 ${store.keyboardFailures} 轮 / 超时 ${store.keyboardTimeouts} 次", color = AppColors.Muted, fontSize = 11.sp)
        Text("普通关首通 +30 分/+15 金币 · Boss 首通 +100 分/+50 金币", color = AppColors.Muted, fontSize = 11.sp)
    }
}

@Composable
private fun AccountValue(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier.background(Color(0xFF1E293B), RoundedCornerShape(9.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(label, color = AppColors.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun EmptyReport(text: String) {
    Box(
        Modifier.fillMaxWidth().background(Color(0xFF1E293B), RoundedCornerShape(9.dp)).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = AppColors.Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ErrorWords(unitWords: List<Word>, store: StudentStore, modifier: Modifier) {
    val words = unitWords
        .filter { (store.wordErrors[it.id] ?: 0) > 0 }
        .sortedByDescending { store.wordErrors[it.id] ?: 0 }
        .take(3)
    Column(modifier.gamePanel(Color(0xFF111C31), Color(0xFF26354B), 14).padding(18.dp)) {
        Text("⚠ 本册拼写错误 TOP 3", color = Color(0xFFFDA4AF), fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        if (words.isEmpty()) {
            EmptyReport("暂无错词，完成练习后将自动生成薄弱项")
        } else {
            words.forEach { word ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(9.dp)).padding(11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${word.word}  ${word.meaning}", color = AppColors.Gold, fontWeight = FontWeight.Bold)
                    Text("拼错 ${store.wordErrors[word.id] ?: 0} 次", color = Color(0xFFFDA4AF), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun GrammarReport(points: List<GrammarPoint>, store: StudentStore, modifier: Modifier) {
    Column(modifier.gamePanel(Color(0xFF111C31), Color(0xFF26354B), 14).padding(18.dp)) {
        Text("🎯 语法薄弱点深度剖析", color = Color(0xFFD8B4FE), fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        points.forEach { point ->
            val attempts = store.grammarAttempts[point.id] ?: 0
            val rate = (store.grammarCorrect[point.id] ?: 0) / attempts.coerceAtLeast(1).toFloat()
            Column(
                Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp)).padding(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(point.title, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(if (attempts == 0) "未练习" else "${(rate * 100).toInt()}%", color = if (rate >= .8f) Color(0xFF34D399) else AppColors.Gold, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { rate },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = if (rate >= .8f) Color(0xFF10B981) else Color(0xFFF59E0B),
                    trackColor = Color(0xFF334155)
                )
            }
        }
    }
}

@Composable
private fun KeyboardReport(store: StudentStore, modifier: Modifier) {
    val accuracy = store.keyboardCorrectKeys * 100 / store.keyboardKeystrokes.coerceAtLeast(1)
    val weakKeys = store.keyboardMistakes.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
        .take(3)
    Column(modifier.gamePanel(Color(0xFF111C31), Color(0xFF155E75), 14).padding(16.dp)) {
        Text("⌨ 键盘启蒙岛", color = Color(0xFF67E8F9), fontWeight = FontWeight.Black)
        Text(
            "正确率 $accuracy% · ${store.keyboardKeystrokes} 次击键",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        if (weakKeys.isEmpty()) {
            EmptyReport("暂无错键记录")
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weakKeys.forEach { (key, count) ->
                    Row(
                        Modifier.weight(1f)
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (key == ' ') "空格" else key.toString(),
                            color = AppColors.Gold,
                            fontWeight = FontWeight.Black
                        )
                        Text("错 $count 次", color = Color(0xFFFDA4AF), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewQueue(words: List<Word>, store: StudentStore, modifier: Modifier) {
    val reviewWords = words
        .filter { (store.wordErrors[it.id] ?: 0) > 0 }
        .sortedByDescending { store.wordErrors[it.id] ?: 0 }
        .take(3)
    Column(modifier.gamePanel(Color(0xFF111C31), Color(0xFF26354B), 14).padding(18.dp)) {
        Text("⏰ 个性化复习队列", color = AppColors.Gold, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        if (reviewWords.isEmpty()) {
            EmptyReport("当前没有需要强化的错词")
        } else {
            reviewWords.forEachIndexed { index, word ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp)).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${word.word}  ${word.meaning}", color = Color.White, fontSize = 12.sp)
                    Text(if (index < 2) "优先复习" else "随后复习", color = if (index < 2) AppColors.Gold else AppColors.Muted, fontSize = 11.sp)
                }
            }
        }
    }
}
