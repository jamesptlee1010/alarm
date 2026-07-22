package com.james.mathwakealarm.alarm

import android.content.Context
import com.james.mathwakealarm.math.MathQuestion
import com.james.mathwakealarm.math.MathQuestionGenerator
import com.james.mathwakealarm.math.Operator
import java.math.BigDecimal

data class AlarmSession(
    val active: Boolean,
    val correctAnswers: Int,
    val requiredCorrectAnswers: Int,
    val question: MathQuestion,
)

class AlarmSessionRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val generator = MathQuestionGenerator()

    fun startNew(requiredCorrectAnswers: Int): AlarmSession {
        val question = generator.generate()
        val session = AlarmSession(
            active = true,
            correctAnswers = 0,
            requiredCorrectAnswers = requiredCorrectAnswers.coerceIn(1, 20),
            question = question,
        )
        write(session)
        return session
    }

    fun loadOrCreate(requiredCorrectAnswers: Int): AlarmSession {
        if (!preferences.getBoolean(KEY_ACTIVE, false)) {
            return startNew(requiredCorrectAnswers)
        }

        val operator = runCatching {
            Operator.valueOf(preferences.getString(KEY_OPERATOR, Operator.ADD.name).orEmpty())
        }.getOrDefault(Operator.ADD)

        return AlarmSession(
            active = true,
            correctAnswers = preferences.getInt(KEY_CORRECT, 0),
            requiredCorrectAnswers = preferences.getInt(KEY_REQUIRED, requiredCorrectAnswers),
            question = MathQuestion(
                left = preferences.getInt(KEY_LEFT, 1),
                operator = operator,
                right = preferences.getInt(KEY_RIGHT, 1),
                answer = preferences.getString(KEY_ANSWER, "2")?.toBigDecimalOrNull() ?: BigDecimal(2),
            ),
        )
    }

    fun answer(session: AlarmSession, submitted: String): Pair<AlarmSession, Boolean> {
        if (!session.question.isCorrect(submitted)) return session to false

        val newCorrectCount = session.correctAnswers + 1
        val next = session.copy(
            correctAnswers = newCorrectCount,
            question = generator.generate(excludingPrompt = session.question.prompt),
        )
        write(next)
        return next to true
    }

    fun skip(session: AlarmSession): AlarmSession {
        val next = session.copy(
            question = generator.generate(excludingPrompt = session.question.prompt),
        )
        write(next)
        return next
    }

    fun complete() {
        preferences.edit().clear().apply()
    }

    private fun write(session: AlarmSession) {
        preferences.edit()
            .putBoolean(KEY_ACTIVE, session.active)
            .putInt(KEY_CORRECT, session.correctAnswers)
            .putInt(KEY_REQUIRED, session.requiredCorrectAnswers)
            .putInt(KEY_LEFT, session.question.left)
            .putString(KEY_OPERATOR, session.question.operator.name)
            .putInt(KEY_RIGHT, session.question.right)
            .putString(KEY_ANSWER, session.question.answer.toPlainString())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "alarm_session"
        private const val KEY_ACTIVE = "active"
        private const val KEY_CORRECT = "correct"
        private const val KEY_REQUIRED = "required"
        private const val KEY_LEFT = "left"
        private const val KEY_OPERATOR = "operator"
        private const val KEY_RIGHT = "right"
        private const val KEY_ANSWER = "answer"
    }
}
