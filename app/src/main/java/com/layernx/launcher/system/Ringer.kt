package com.layernx.launcher.system

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Vibrator
import com.layernx.launcher.R

/** The three ringer states of the bell button, in the order a tap cycles through them. */
enum class RingerState(val ringerMode: Int, val iconRes: Int, val labelRes: Int, val toastRes: Int) {
    SOUND(AudioManager.RINGER_MODE_NORMAL, R.drawable.ic_bell, R.string.ringer_sound, R.string.ringer_changed_sound),
    VIBRATE(AudioManager.RINGER_MODE_VIBRATE, R.drawable.ic_bell_vibrate, R.string.ringer_vibrate, R.string.ringer_changed_vibrate),
    SILENT(AudioManager.RINGER_MODE_SILENT, R.drawable.ic_bell_off, R.string.ringer_silent, R.string.ringer_changed_silent),
    ;

    companion object {
        fun fromRingerMode(mode: Int): RingerState = when (mode) {
            AudioManager.RINGER_MODE_NORMAL -> SOUND
            AudioManager.RINGER_MODE_VIBRATE -> VIBRATE
            else -> SILENT
        }

        /** Ton → Vibration → Stumm → Ton. Phones without a vibrator skip "Vibration". */
        fun next(current: RingerState, canVibrate: Boolean): RingerState = when (current) {
            SOUND -> if (canVibrate) VIBRATE else SILENT
            VIBRATE -> SILENT
            SILENT -> SOUND
        }
    }
}

/** Switches the phone's ringer mode – the same setting the volume keys change. */
object Ringer {

    sealed interface Result {
        data class Changed(val state: RingerState) : Result
        /** Entering or leaving "Stumm" needs "Bitte nicht stören" access since Android 7. */
        data object NeedsDndAccess : Result
        data object Unavailable : Result
    }

    fun current(context: Context): RingerState {
        val audio = context.getSystemService(AudioManager::class.java) ?: return RingerState.SOUND
        return RingerState.fromRingerMode(audio.ringerMode)
    }

    fun cycle(context: Context): Result {
        val audio = context.getSystemService(AudioManager::class.java) ?: return Result.Unavailable
        val canVibrate = context.getSystemService(Vibrator::class.java)?.hasVibrator() == true
        val current = RingerState.fromRingerMode(audio.ringerMode)
        val target = RingerState.next(current, canVibrate)

        val touchesSilent = current == RingerState.SILENT || target == RingerState.SILENT
        if (touchesSilent && !hasDndAccess(context)) return Result.NeedsDndAccess
        return try {
            audio.ringerMode = target.ringerMode
            // Report what the system actually applied (some firmwares map modes differently).
            Result.Changed(RingerState.fromRingerMode(audio.ringerMode))
        } catch (e: SecurityException) {
            Result.NeedsDndAccess
        }
    }

    fun hasDndAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true
}
