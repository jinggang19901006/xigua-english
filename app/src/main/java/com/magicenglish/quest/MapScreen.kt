package com.magicenglish.quest

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MapNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val x: Int,
    val y: Int,
    val colors: List<Color>,
    val unlocked: Boolean,
    val onClick: () -> Unit
)

@Composable
fun MapScreen(
    unit: CourseUnit,
    store: StudentStore,
    onStage: (Int) -> Unit
) {
    val nodes = listOf(
        MapNode("words", "新词启航营", "5步多通道记忆法", "🌱", 58, 340, listOf(Color(0xFF34D399), Color(0xFF15803D)), store.isStageUnlocked(unit.id, 0)) { onStage(0) },
        MapNode("spelling", "键盘拼写营", "完整拼写本单元词汇", "⌨", 330, 165, listOf(Color(0xFF38BDF8), Color(0xFF1D4ED8)), store.isStageUnlocked(unit.id, 1)) { onStage(1) },
        MapNode("grammar", "语法思考工坊", "观察情境后主动输出", "💡", 620, 335, listOf(Color(0xFFC084FC), Color(0xFF7E22CE)), store.isStageUnlocked(unit.id, 2)) { onStage(2) },
        MapNode("boss", "时空遗忘兽 Boss", "单元终极综合挑战", "🐉", 915, 135, listOf(Color(0xFFFB7185), Color(0xFFBE123C)), store.isStageUnlocked(unit.id, 3)) { onStage(3) }
    )

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFFBAE6FD), Color(0xFF99F6E4)))
        )
    ) {
        Box(
            Modifier.align(Alignment.Center).size(1160.dp, 600.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF86EFAC), Color(0xFF22C55E))),
                    RoundedCornerShape(44.dp)
                )
                .border(7.dp, Color(0xFF166534), RoundedCornerShape(44.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = .08f), Color.Transparent)
                    )
                )
                val path = Path().apply {
                    moveTo(size.width * .12f, size.height * .68f)
                    cubicTo(size.width * .25f, size.height * .68f, size.width * .28f, size.height * .34f, size.width * .42f, size.height * .34f)
                    cubicTo(size.width * .56f, size.height * .34f, size.width * .58f, size.height * .68f, size.width * .71f, size.height * .67f)
                    cubicTo(size.width * .84f, size.height * .65f, size.width * .84f, size.height * .28f, size.width * .93f, size.height * .25f)
                }
                drawPath(path, Color(0xFF92400E).copy(alpha = .22f), style = Stroke(25.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Color(0xFFFFF3B0), style = Stroke(18.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Color(0xFFD97706), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "UNIT ${unit.number}",
                        color = Color(0xFF14532D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        unit.title,
                        color = AppColors.Navy,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    "${store.completedStages(unit.id)} / 4 已完成",
                    Modifier.background(AppColors.Navy.copy(.92f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color(0xFFCCFBF1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("⛺", Modifier.offset(290.dp, 78.dp), fontSize = 32.sp)
            Text("🏰", Modifier.offset(1060.dp, 58.dp), fontSize = 42.sp)
            Text("🌴", Modifier.offset(34.dp, 510.dp), fontSize = 42.sp)
            Text("⛵", Modifier.offset(550.dp, 515.dp), fontSize = 42.sp)

            nodes.forEachIndexed { index, node ->
                MapNodeView(
                    node = node,
                    number = index + 1,
                    stars = store.stageStars(unit.id, index)
                )
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).offset(y = (-12).dp)
                .gamePanel(AppColors.Navy.copy(.96f), AppColors.Gold, 28)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("本次随机练习", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            listOf(5, 10, 15, 0).forEach { count ->
                val selected = store.practiceWordCount == count
                GameButton(
                    text = if (count == 0) "全部" else "$count 词",
                    modifier = Modifier.padding(start = 8.dp).width(82.dp).height(38.dp),
                    color = if (selected) AppColors.Gold else Color(0xFF334155),
                    shadow = if (selected) Color(0xFFB87800) else Color(0xFF172033)
                ) { store.updatePracticeWordCount(count) }
            }
            Text(
                "单元完整词库 ${unit.words.size} 词",
                Modifier.padding(start = 16.dp),
                color = Color(0xFF99F6E4),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MapNodeView(node: MapNode, number: Int, stars: Int) {
    Column(
        modifier = Modifier.offset(node.x.dp, node.y.dp).width(198.dp)
            .clickable(enabled = node.unlocked, onClick = node.onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(108.dp, 76.dp)
                .background(Brush.verticalGradient(node.colors), RoundedCornerShape(18.dp))
                .border(3.dp, Color.White.copy(alpha = .9f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (node.unlocked) node.icon else "🔒", fontSize = 34.sp)
            Box(
                Modifier.align(Alignment.TopStart).offset((-7).dp, (-7).dp)
                    .background(AppColors.Navy, RoundedCornerShape(8.dp))
                    .border(2.dp, AppColors.Gold, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text("$number", color = AppColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        Column(
            Modifier.padding(top = 7.dp).fillMaxWidth()
                .background(AppColors.Navy.copy(alpha = .96f), RoundedCornerShape(12.dp))
                .border(2.dp, Color.White.copy(alpha = .8f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                node.title,
                color = if (node.unlocked) Color.White else Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Row {
                repeat(3) {
                    Text(
                        "★",
                        color = if (it < stars) AppColors.Gold else Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
            Text(
                if (node.unlocked) node.subtitle else "完成上一关后解锁",
                color = if (node.unlocked) Color(0xFFCCFBF1) else Color(0xFFFDA4AF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
