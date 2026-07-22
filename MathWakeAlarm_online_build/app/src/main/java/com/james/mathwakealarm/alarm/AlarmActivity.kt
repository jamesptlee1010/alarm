package com.james.mathwakealarm.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.james.mathwakealarm.data.AlarmPreferences
import com.james.mathwakealarm.ui.theme.MathWakeAlarmTheme

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
        )
        getSystemService(KeyguardManager::class.java)
            .requestDismissKeyguard(this, null)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )

        setContent {
            MathWakeAlarmTheme(darkTheme = true) {
                AlarmChallengeScreen(onComplete = ::finishChallenge)
            }
        }
    }

    private fun finishChallenge() {
        startService(
            Intent(this, AlarmService::class.java).setAction(AlarmService.ACTION_STOP),
        )
        finishAndRemoveTask()
    }
}

@Composable
private fun AlarmChallengeScreen(onComplete: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { AlarmSessionRepository(context) }
    val settings = remember { AlarmPreferences(context).load() }
    var session by remember { mutableStateOf(repository.loadOrCreate(settings.requiredCorrectAnswers)) }
    var answer by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isIncorrect by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    fun submit() {
        if (answer.isBlank()) {
            feedback = "Enter an answer first"
            isIncorrect = true
            return
        }

        val (updated, correct) = repository.answer(session, answer)
        if (!correct) {
            feedback = "Not quite — try again"
            isIncorrect = true
            answer = ""
            return
        }

        session = updated
        answer = ""
        isIncorrect = false

        if (session.correctAnswers >= session.requiredCorrectAnswers) {
            feedback = "Alarm stopped"
            focusManager.clearFocus()
            onComplete()
        } else {
            feedback = "Correct"
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 22.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFF0EA5E9), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "WAKE-UP CHALLENGE",
                    color = Color(0xFFBAE6FD),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Solve ${session.requiredCorrectAnswers} questions to stop the alarm",
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Question ${session.correctAnswers + 1}",
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${session.correctAnswers}/${session.requiredCorrectAnswers} correct",
                            color = Color(0xFF7DD3FC),
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "${session.question.prompt} = ?",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 44.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(26.dp))

                    OutlinedTextField(
                        value = answer,
                        onValueChange = { raw ->
                            answer = raw.filter { it.isDigit() || it == '.' || it == ',' }.take(8)
                            feedback = null
                            isIncorrect = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        label = { Text("Your answer") },
                        singleLine = true,
                        isError = isIncorrect,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        supportingText = if (feedback != null) {
                            { Text(feedback.orEmpty()) }
                        } else {
                            null
                        },
                    )

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { submit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text("SUBMIT", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.size(10.dp))
                        Icon(Icons.Rounded.ArrowForward, contentDescription = null)
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            session = repository.skip(session)
                            answer = ""
                            feedback = "Skipped — you still need ${session.requiredCorrectAnswers - session.correctAnswers} correct"
                            isIncorrect = false
                            focusRequester.requestFocus()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Skip question")
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    progress = session.correctAnswers.toFloat() / session.requiredCorrectAnswers,
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF334155),
                    modifier = Modifier.size(42.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "The sound gets louder every 10 seconds",
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
