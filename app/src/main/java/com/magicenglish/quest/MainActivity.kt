package com.magicenglish.quest

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Locale

enum class AppScreen {
    BOOKS, UNITS, MAP, WORDS, SPELLING, GRAMMAR, BOSS,
    FROG_HOME, KEYBOARD, FROG, PARENT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = AppColors.Navy) {
                    TabletCanvas { MagicEnglishApp() }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun MagicEnglishApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { StudentStore(context) }
    var screen by remember { mutableStateOf(AppScreen.BOOKS) }
    var activeStage by remember { mutableIntStateOf(0) }
    var keyboardLevel by remember { mutableIntStateOf(0) }
    var showSettlement by remember { mutableStateOf(false) }
    var settlementReward by remember { mutableStateOf(StageReward.NONE) }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 65) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            tone.release()
        }
    }

    val speak: (String) -> Unit = { text ->
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
        tts?.setSpeechRate(0.82f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }
    val successSound: () -> Unit = {
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        Unit
    }
    val errorSound: () -> Unit = {
        tone.startTone(ToneGenerator.TONE_PROP_NACK, 180)
        Unit
    }
    val selectedBook = LearningData.book(store.selectedBookId)
    val selectedUnit = LearningData.unit(store.selectedBookId, store.selectedUnitId)
    var practiceWords by remember(selectedUnit.id, store.practiceWordCount) {
        mutableStateOf(samplePracticeWords(selectedUnit.words, store.practiceWordCount))
    }
    val stageScreens = listOf(AppScreen.WORDS, AppScreen.SPELLING, AppScreen.GRAMMAR, AppScreen.BOSS)
    val openStage: (Int) -> Unit = { stage ->
        activeStage = stage
        practiceWords = samplePracticeWords(selectedUnit.words, store.practiceWordCount)
        screen = stageScreens[stage]
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
    }
    val complete: () -> Unit = {
        settlementReward = store.completeStage(selectedUnit.id, activeStage)
        showSettlement = true
        successSound()
    }

    Column(Modifier.fillMaxSize().background(AppColors.Navy)) {
        AppHeader(
            store = store,
            book = selectedBook,
            unit = selectedUnit,
            screen = screen,
            onBack = {
                screen = when (screen) {
                    AppScreen.BOOKS -> AppScreen.BOOKS
                    AppScreen.UNITS -> AppScreen.BOOKS
                    AppScreen.MAP -> AppScreen.UNITS
                    AppScreen.KEYBOARD, AppScreen.FROG -> AppScreen.FROG_HOME
                    AppScreen.FROG_HOME -> AppScreen.MAP
                    else -> AppScreen.MAP
                }
            },
            onBooks = { screen = AppScreen.BOOKS },
            onFrog = { screen = AppScreen.FROG_HOME },
            onParent = {
                screen = if (screen == AppScreen.PARENT) AppScreen.MAP else AppScreen.PARENT
            }
        )
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (screen) {
                AppScreen.BOOKS -> BookSelectionScreen(store) {
                    screen = AppScreen.UNITS
                }
                AppScreen.UNITS -> UnitSelectionScreen(
                    book = selectedBook,
                    store = store,
                    onUnitSelected = { screen = AppScreen.MAP }
                )
                AppScreen.MAP -> MapScreen(
                    unit = selectedUnit,
                    store = store,
                    onStage = openStage
                )
                AppScreen.WORDS -> WordLearningScreen(practiceWords, store, speak, successSound, errorSound, complete)
                AppScreen.SPELLING -> SpellingScreen(
                    practiceWords, store, speak, successSound, errorSound, complete
                )
                AppScreen.GRAMMAR -> GrammarScreen(selectedUnit.grammar, store, successSound, errorSound, complete)
                AppScreen.BOSS -> BossScreen(practiceWords, store, speak, successSound, errorSound, complete)
                AppScreen.FROG_HOME -> FrogTrainingHome(
                    store = store,
                    onKeyboardLevel = { level ->
                        keyboardLevel = level
                        screen = AppScreen.KEYBOARD
                    },
                    onFlume = {
                        practiceWords = samplePracticeWords(selectedUnit.words, store.practiceWordCount)
                        screen = AppScreen.FROG
                    }
                )
                AppScreen.KEYBOARD -> KeyboardIslandScreen(
                    levelIndex = keyboardLevel,
                    store = store,
                    success = successSound,
                    error = errorSound,
                    onBack = { screen = AppScreen.FROG_HOME }
                )
                AppScreen.FROG -> FrogSpellingScreen(selectedUnit, practiceWords, store, speak, successSound, errorSound)
                AppScreen.PARENT -> ParentDashboard(store, selectedBook, selectedUnit) { screen = AppScreen.MAP }
            }
        }
    }

    if (showSettlement) {
        SettlementDialog(
            reward = settlementReward,
            onMap = {
                showSettlement = false
                screen = AppScreen.MAP
            },
            onNext = {
                showSettlement = false
                if (activeStage < 3) {
                    activeStage += 1
                    practiceWords = samplePracticeWords(selectedUnit.words, store.practiceWordCount)
                    screen = stageScreens[activeStage]
                } else {
                    val nextUnit = selectedBook.units.getOrNull(selectedUnit.number)
                    if (nextUnit != null) {
                        store.selectUnit(nextUnit)
                        activeStage = 0
                        screen = AppScreen.MAP
                    } else {
                        screen = AppScreen.UNITS
                    }
                }
            }
        )
    }
}

@Composable
private fun AppHeader(
    store: StudentStore,
    book: CourseBook,
    unit: CourseUnit,
    screen: AppScreen,
    onBack: () -> Unit,
    onBooks: () -> Unit,
    onFrog: () -> Unit,
    onParent: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(AppColors.Header)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (screen != AppScreen.BOOKS) {
                GameButton(
                    when (screen) {
                        AppScreen.UNITS -> "‹ 教材"
                        AppScreen.MAP -> "‹ 单元"
                        AppScreen.KEYBOARD, AppScreen.FROG -> "‹ 训练中心"
                        else -> "🗺 地图"
                    },
                    Modifier.width(112.dp).height(42.dp),
                    AppColors.Gold,
                    Color(0xFFB87800),
                    onClick = onBack
                )
                Spacer(Modifier.width(12.dp))
            }
            Row(
                Modifier.gamePanel(AppColors.Panel, Color(0xFF40506A), 16)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(34.dp).background(AppColors.Gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(book.shortName, color = AppColors.Navy, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Unit ${unit.number} · ${unit.topic}", color = AppColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(unit.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameButton(
                "📚 切换教材",
                Modifier.width(118.dp).height(42.dp),
                Color(0xFF2563EB),
                Color(0xFF1E40AF),
                onClick = onBooks
            )
            Spacer(Modifier.width(10.dp))
            GameButton(
                if (screen == AppScreen.FROG_HOME || screen == AppScreen.KEYBOARD || screen == AppScreen.FROG) {
                    "🐸 训练中心"
                } else {
                    "🐸 激流勇进"
                },
                Modifier.width(118.dp).height(42.dp),
                if (screen == AppScreen.FROG_HOME || screen == AppScreen.KEYBOARD || screen == AppScreen.FROG) {
                    Color(0xFF16A34A)
                } else {
                    Color(0xFF0F766E)
                },
                if (screen == AppScreen.FROG_HOME || screen == AppScreen.KEYBOARD || screen == AppScreen.FROG) {
                    Color(0xFF166534)
                } else {
                    Color(0xFF115E59)
                },
                onClick = onFrog
            )
            Spacer(Modifier.width(10.dp))
            StatPill("积分", store.points)
            Spacer(Modifier.width(10.dp))
            StatPill("金币", store.coins)
            Spacer(Modifier.width(10.dp))
            GameButton(
                if (screen == AppScreen.PARENT) "📊 返回闯关" else "📊 家长学情报告",
                Modifier.width(150.dp).height(42.dp),
                if (screen == AppScreen.PARENT) AppColors.Purple else Color(0xFF475569),
                if (screen == AppScreen.PARENT) Color(0xFF6B21A8) else Color(0xFF1E293B),
                onClick = onParent
            )
        }
    }
}

private fun samplePracticeWords(words: List<Word>, configuredCount: Int): List<Word> {
    val count = if (configuredCount == 0) words.size else minOf(configuredCount, words.size)
    return words.shuffled().take(count)
}

@Composable
private fun StatPill(icon: String, value: Int) {
    Row(
        Modifier.background(Color(0x3329A3E8), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(6.dp))
        Text("$value", color = AppColors.Gold, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}
