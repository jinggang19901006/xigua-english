package com.magicenglish.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

@Composable
fun BookSelectionScreen(
    store: StudentStore,
    onBookSelected: (CourseBook) -> Unit
) {
    var grade by remember { mutableIntStateOf(LearningData.book(store.selectedBookId).grade) }
    val books = LearningData.books.filter { it.grade == grade }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF123D66), Color(0xFF0C1A30))))
            .padding(horizontal = 86.dp, vertical = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("选择你的英语探险教材", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Text("人教版 PEP · 小学英语三至六年级完整课程", color = AppColors.Gold, fontSize = 15.sp)
        Spacer(Modifier.height(28.dp))

        Row(
            Modifier.gamePanel(Color(0xFF132943), Color(0xFF365675), 18).padding(9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            (3..6).forEach { item ->
                GameButton(
                    text = "$item 年级",
                    modifier = Modifier.width(150.dp).height(48.dp),
                    color = if (grade == item) AppColors.Sky else Color(0xFF334155),
                    shadow = if (grade == item) Color(0xFF075985) else Color(0xFF172033)
                ) { grade = item }
            }
        }

        Spacer(Modifier.height(34.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            books.forEach { book ->
                val completed = store.completedUnits(book)
                Column(
                    Modifier.width(410.dp).height(300.dp)
                        .gamePanel(Color(0xFF1B2B43), if (book.id == store.selectedBookId) AppColors.Gold else Color(0xFF40506A), 22)
                        .clickable {
                            store.selectBook(book)
                            onBookSelected(book)
                        }
                        .padding(28.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(
                            Modifier.size(76.dp).background(
                                if (book.volume == Volume.FIRST) AppColors.Sky else AppColors.Purple,
                                RoundedCornerShape(18.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(book.shortName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            if (book.id == store.selectedBookId) "正在学习" else "可选择",
                            Modifier.background(
                                if (book.id == store.selectedBookId) Color(0xFF14532D) else Color(0xFF334155),
                                CircleShape
                            ).padding(horizontal = 13.dp, vertical = 7.dp),
                            color = if (book.id == store.selectedBookId) Color(0xFF86EFAC) else AppColors.Muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(book.displayName, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
                        Text("${book.units.size} 个核心单元 · ${book.units.sumOf { it.words.size }} 个新词/短语", color = AppColors.Muted, fontSize = 13.sp)
                    }
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("全册进度", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$completed / ${book.units.size}", color = AppColors.Gold, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { completed / book.units.size.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                            color = Color(0xFF22C55E),
                            trackColor = Color(0xFF334155)
                        )
                    }
                    Text(
                        "点击进入单元目录  ➜",
                        color = AppColors.Gold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun UnitSelectionScreen(
    book: CourseBook,
    store: StudentStore,
    onUnitSelected: (CourseUnit) -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF0C1A30)).padding(horizontal = 48.dp, vertical = 26.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("${book.series} · ${book.displayName}", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("按顺序完成每个单元，击败 Boss 后自动解锁下一单元", color = AppColors.Muted, fontSize = 13.sp)
                }
            }
            Text(
                "已完成 ${store.completedUnits(book)} / ${book.units.size}",
                Modifier.background(Color(0xFF173B5A), RoundedCornerShape(12.dp)).padding(horizontal = 18.dp, vertical = 10.dp),
                color = AppColors.Gold,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(24.dp))

        book.units.chunked(3).forEachIndexed { rowIndex, rowUnits ->
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                rowUnits.forEach { unit ->
                    val unitIndex = unit.number - 1
                    val unlocked = store.isUnitUnlocked(book, unitIndex)
                    val completedStages = store.completedStages(unit.id)
                    UnitCard(
                        unit = unit,
                        unlocked = unlocked,
                        completedStages = completedStages,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (unlocked) {
                                store.selectUnit(unit)
                                onUnitSelected(unit)
                            }
                        }
                    )
                }
                repeat(3 - rowUnits.size) { Spacer(Modifier.weight(1f)) }
            }
            if (rowIndex < book.units.chunked(3).lastIndex) Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun UnitCard(
    unit: CourseUnit,
    unlocked: Boolean,
    completedStages: Int,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val completed = completedStages == 4
    Column(
        modifier.height(250.dp)
            .gamePanel(
                if (unlocked) Color(0xFF182A43) else Color(0xFF182131),
                when {
                    completed -> Color(0xFF22C55E)
                    unlocked -> Color(0xFF405A78)
                    else -> Color(0xFF273449)
                },
                18
            )
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(
                    if (unlocked) AppColors.Gold else Color(0xFF475569),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("${unit.number}", color = AppColors.Navy, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Text(
                when {
                    completed -> "已通关 ✓"
                    unlocked -> "$completedStages / 4 关"
                    else -> "🔒 前置未完成"
                },
                color = when {
                    completed -> Color(0xFF86EFAC)
                    unlocked -> AppColors.Gold
                    else -> Color(0xFF64748B)
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column {
            Text(
                "Unit ${unit.number}",
                color = if (unlocked) AppColors.Sky else Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                unit.title,
                color = if (unlocked) Color.White else Color(0xFF64748B),
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2
            )
            Text("${unit.topic} · ${unit.words.size} 个新词/短语", color = if (unlocked) AppColors.Muted else Color(0xFF475569), fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("词", "拼", "语", "Boss").forEachIndexed { index, label ->
                Box(
                    Modifier.size(66.dp, 35.dp)
                        .background(
                            if (index < completedStages) Color(0xFF166534) else Color(0xFF273449),
                            RoundedCornerShape(9.dp)
                        )
                        .border(1.dp, if (index < completedStages) Color(0xFF22C55E) else Color(0xFF334155), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (index < completedStages) Color(0xFF86EFAC) else Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
