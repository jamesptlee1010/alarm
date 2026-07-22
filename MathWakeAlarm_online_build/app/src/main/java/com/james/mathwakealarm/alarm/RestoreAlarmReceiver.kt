package com.james.mathwakealarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestoreAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler(context).scheduleFromStoredSettings()
    }
}
