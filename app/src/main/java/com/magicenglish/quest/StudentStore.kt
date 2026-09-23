package com.magicenglish.quest

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class StageReward(
    val points: Int,
    val coins: Int,
    val firstClear: Boolean,
    val medal: String
) {
    companion object {
        val NONE = StageReward(0, 0, false, "复习达人")
    }
}

@Stable
class StudentStore(context: Context) {
    private val prefs = context.getSharedPreferences("magic_english_progress", Context.MODE_PRIVATE)

    var points by mutableIntStateOf(
        prefs.getInt("points", prefs.getInt("stars", 42) * 10)
    )
        private set
    var coins by mutableIntStateOf(prefs.getInt("coins", 380))
        private set
    var todayMinutes by mutableIntStateOf(prefs.getInt("today_minutes", 18))
        private set
    var practiceWordCount by mutableIntStateOf(prefs.getInt("practice_word_count", 10))
        private set

    var selectedBookId by mutableStateOf(
        prefs.getString("selected_book", LearningData.DEFAULT_BOOK_ID)
            ?: LearningData.DEFAULT_BOOK_ID
    )
        private set
    var selectedUnitId by mutableStateOf(
        prefs.getString("selected_unit", LearningData.book(selectedBookId).units.first().id)
            ?: LearningData.book(selectedBookId).units.first().id
    )
        private set

    private val stageStars = mutableStateMapOf<String, Int>()
    private val keyboardStars = mutableStateMapOf<Int, Int>()
    val wordErrors = mutableStateMapOf<String, Int>()
    val mastered = mutableStateMapOf<String, Boolean>()
    val grammarCorrect = mutableStateMapOf<String, Int>()
    val grammarAttempts = mutableStateMapOf<String, Int>()
    var totalAnswers by mutableIntStateOf(prefs.getInt("total_answers", 0))
        private set
    var correctAnswers by mutableIntStateOf(prefs.getInt("correct_answers", 0))
        private set
    var bossBattles by mutableIntStateOf(prefs.getInt("boss_battles", 0))
        private set
    var bossWins by mutableIntStateOf(prefs.getInt("boss_wins", 0))
        private set
    var frogRounds by mutableIntStateOf(prefs.getInt("frog_rounds", 0))
        private set
    var frogWordsSolved by mutableIntStateOf(prefs.getInt("frog_words_solved", 0))
        private set
    var frogBestScore by mutableIntStateOf(prefs.getInt("frog_best_score", 0))
        private set
    var frogFailures by mutableIntStateOf(prefs.getInt("frog_failures", 0))
        private set
    var frogLivesLost by mutableIntStateOf(prefs.getInt("frog_lives_lost", 0))
        private set
    var keyboardKeystrokes by mutableIntStateOf(prefs.getInt("keyboard_keystrokes", 0))
        private set
    var keyboardCorrectKeys by mutableIntStateOf(prefs.getInt("keyboard_correct_keys", 0))
        private set
    var keyboardRounds by mutableIntStateOf(prefs.getInt("keyboard_rounds", 0))
        private set
    var keyboardBestAccuracy by mutableIntStateOf(prefs.getInt("keyboard_best_accuracy", 0))
        private set
    var keyboardFailures by mutableIntStateOf(prefs.getInt("keyboard_failures", 0))
        private set
    var keyboardTimeouts by mutableIntStateOf(prefs.getInt("keyboard_timeouts", 0))
        private set
    val keyboardMistakes = mutableStateMapOf<Char, Int>()

    init {
        repeat(5) { level ->
            keyboardStars[level] = prefs.getInt("keyboard_level_${level}_stars", 0)
        }
        "QWERTYUIOPASDFGHJKL;ZXCVBNM ".forEach { key ->
            keyboardMistakes[key] = prefs.getInt("keyboard_error_$key", 0)
        }
        LearningData.books.forEach { book ->
            book.units.forEach { unit ->
                unit.words.forEach { word ->
                    wordErrors[word.id] = prefs.getInt("error_${word.id}", 0)
                    mastered[word.id] = prefs.getBoolean("mastered_${word.id}", false)
                }
                unit.grammar.forEach { point ->
                    grammarCorrect[point.id] = prefs.getInt("correct_${point.id}", 0)
                    grammarAttempts[point.id] = prefs.getInt("attempts_${point.id}", 0)
                }
            }
        }
    }

    fun selectBook(book: CourseBook) {
        selectedBookId = book.id
        val savedUnit = prefs.getString("last_unit_${book.id}", null)
        selectedUnitId = book.units.firstOrNull { it.id == savedUnit }?.id ?: book.units.first().id
        persistSelection()
    }

    fun selectUnit(unit: CourseUnit) {
        selectedUnitId = unit.id
        persistSelection()
    }

    fun updatePracticeWordCount(count: Int) {
        practiceWordCount = count.coerceIn(0, 15)
        prefs.edit().putInt("practice_word_count", practiceWordCount).apply()
    }

    fun stageStars(unitId: String, stageIndex: Int): Int {
        val key = stageKey(unitId, stageIndex)
        return stageStars.getOrPut(key) { prefs.getInt(key, 0) }
    }

    fun isStageUnlocked(unitId: String, stageIndex: Int): Boolean =
        stageIndex == 0 || stageStars(unitId, stageIndex - 1) > 0

    fun isUnitUnlocked(book: CourseBook, unitIndex: Int): Boolean =
        unitIndex == 0 || stageStars(book.units[unitIndex - 1].id, 3) > 0

    fun completedStages(unitId: String): Int =
        (0..3).count { stageStars(unitId, it) > 0 }

    fun completedUnits(book: CourseBook): Int =
        book.units.count { stageStars(it.id, 3) > 0 }

    fun recordWordError(word: Word) {
        wordErrors[word.id] = (wordErrors[word.id] ?: 0) + 1
        totalAnswers += 1
        prefs.edit()
            .putInt("error_${word.id}", wordErrors[word.id] ?: 0)
            .putInt("total_answers", totalAnswers)
            .apply()
    }

    fun recordWordMastered(word: Word) {
        if (mastered[word.id] != true) {
            mastered[word.id] = true
            points += 5
            coins += 3
            totalAnswers += 1
            correctAnswers += 1
        }
        persistTotals(
            prefs.edit().putBoolean("mastered_${word.id}", true)
        )
    }

    fun recordGrammar(id: String, activity: String, correct: Boolean) {
        grammarAttempts[id] = (grammarAttempts[id] ?: 0) + 1
        totalAnswers += 1
        if (correct) {
            grammarCorrect[id] = (grammarCorrect[id] ?: 0) + 1
            correctAnswers += 1
            val rewardKey = "reward_grammar_${id}_$activity"
            if (!prefs.getBoolean(rewardKey, false)) {
                points += 5
                coins += 2
                prefs.edit().putBoolean(rewardKey, true).apply()
            }
        }
        persistTotals(
            prefs.edit()
                .putInt("attempts_$id", grammarAttempts[id] ?: 0)
                .putInt("correct_$id", grammarCorrect[id] ?: 0)
        )
    }

    fun recordBossAnswer(word: Word, correct: Boolean) {
        totalAnswers += 1
        if (correct) {
            correctAnswers += 1
        } else {
            wordErrors[word.id] = (wordErrors[word.id] ?: 0) + 1
        }
        persistTotals(
            prefs.edit().putInt("error_${word.id}", wordErrors[word.id] ?: 0)
        )
    }

    fun recordBossResult(won: Boolean) {
        bossBattles += 1
        if (won) bossWins += 1
        persistTotals(prefs.edit())
    }

    fun recordFrogMistake(word: Word) {
        wordErrors[word.id] = (wordErrors[word.id] ?: 0) + 1
        totalAnswers += 1
        persistTotals(
            prefs.edit().putInt("error_${word.id}", wordErrors[word.id] ?: 0)
        )
    }

    fun recordFrogWordSolved(word: Word): Boolean {
        val firstMastery = mastered[word.id] != true
        frogWordsSolved += 1
        totalAnswers += 1
        correctAnswers += 1
        if (firstMastery) {
            mastered[word.id] = true
            points += 5
            coins += 3
        }
        persistTotals(
            prefs.edit().putBoolean("mastered_${word.id}", true)
        )
        return firstMastery
    }

    fun completeFrogRound(score: Int) {
        frogRounds += 1
        frogBestScore = maxOf(frogBestScore, score)
        todayMinutes += 3
        persistTotals(prefs.edit())
    }

    fun recordFrogLifeLost() {
        frogLivesLost += 1
        persistTotals(prefs.edit())
    }

    fun recordFrogFailure() {
        frogFailures += 1
        persistTotals(prefs.edit())
    }

    fun keyboardLevelStars(levelIndex: Int): Int =
        keyboardStars[levelIndex] ?: 0

    fun isKeyboardLevelUnlocked(levelIndex: Int): Boolean =
        levelIndex == 0 || keyboardLevelStars(levelIndex - 1) > 0

    fun recordKeyboardKey(expected: Char, isCorrect: Boolean) {
        keyboardKeystrokes += 1
        if (isCorrect) {
            keyboardCorrectKeys += 1
        } else {
            val key = expected.uppercaseChar()
            keyboardMistakes[key] = (keyboardMistakes[key] ?: 0) + 1
        }
        persistTotals(
            prefs.edit().putInt(
                "keyboard_error_${expected.uppercaseChar()}",
                keyboardMistakes[expected.uppercaseChar()] ?: 0
            )
        )
    }

    fun recordKeyboardTimeout(expected: Char) {
        val key = expected.uppercaseChar()
        keyboardTimeouts += 1
        keyboardMistakes[key] = (keyboardMistakes[key] ?: 0) + 1
        persistTotals(
            prefs.edit().putInt("keyboard_error_$key", keyboardMistakes[key] ?: 0)
        )
    }

    fun completeKeyboardLevel(levelIndex: Int, accuracy: Int, passed: Boolean = true): Int {
        val stars = if (!passed) 0 else when {
            accuracy >= 95 -> 3
            accuracy >= 85 -> 2
            accuracy >= 70 -> 1
            else -> 0
        }
        val key = "keyboard_level_${levelIndex}_stars"
        val previous = keyboardLevelStars(levelIndex)
        if (stars > previous) {
            val gained = stars - previous
            points += gained * 15
            coins += gained * 5
            keyboardStars[levelIndex] = stars
        }
        keyboardRounds += 1
        if (!passed) keyboardFailures += 1
        keyboardBestAccuracy = maxOf(keyboardBestAccuracy, accuracy)
        persistTotals(
            prefs.edit().putInt(key, maxOf(previous, stars))
        )
        return stars
    }

    fun completeStage(unitId: String, stageIndex: Int): StageReward {
        val key = stageKey(unitId, stageIndex)
        if (stageStars(unitId, stageIndex) == 0) {
            val reward = if (stageIndex == 3) {
                StageReward(100, 50, true, "Boss 征服者")
            } else {
                StageReward(30, 15, true, listOf("词汇先锋", "拼写达人", "语法侦探")[stageIndex])
            }
            stageStars[key] = 3
            points += reward.points
            coins += reward.coins
            todayMinutes += 2
            persistTotals(prefs.edit().putInt(key, 3))
            return reward
        }
        return StageReward.NONE
    }

    private fun persistSelection() {
        prefs.edit()
            .putString("selected_book", selectedBookId)
            .putString("selected_unit", selectedUnitId)
            .putString("last_unit_$selectedBookId", selectedUnitId)
            .apply()
    }

    private fun persistTotals(editor: android.content.SharedPreferences.Editor) {
        editor
            .putInt("points", points)
            .putInt("coins", coins)
            .putInt("today_minutes", todayMinutes)
            .putInt("total_answers", totalAnswers)
            .putInt("correct_answers", correctAnswers)
            .putInt("boss_battles", bossBattles)
            .putInt("boss_wins", bossWins)
            .putInt("frog_rounds", frogRounds)
            .putInt("frog_words_solved", frogWordsSolved)
            .putInt("frog_best_score", frogBestScore)
            .putInt("frog_failures", frogFailures)
            .putInt("frog_lives_lost", frogLivesLost)
            .putInt("keyboard_keystrokes", keyboardKeystrokes)
            .putInt("keyboard_correct_keys", keyboardCorrectKeys)
            .putInt("keyboard_rounds", keyboardRounds)
            .putInt("keyboard_best_accuracy", keyboardBestAccuracy)
            .putInt("keyboard_failures", keyboardFailures)
            .putInt("keyboard_timeouts", keyboardTimeouts)
            .apply()
    }

    private fun stageKey(unitId: String, stageIndex: Int) = "stage_${unitId}_$stageIndex"
}
