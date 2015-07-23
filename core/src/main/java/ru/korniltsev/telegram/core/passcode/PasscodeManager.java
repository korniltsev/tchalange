package ru.korniltsev.telegram.core.passcode;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import org.joda.time.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

//todo should know something about authorization!
@Singleton
public class PasscodeManager {

    public static final String LAST_PAUSE = "last_pause";
    public static final String IGNORE_LAST_SAVED_TIME = "ignore_last_saved_time";
    public static final String AUTO_LOCK_DURATION = "auto_lock_duration";
    public static final String PASSCODE_ENABLED = "passcode_enabled";
    public static final String AUTO_LOCK_TIME = "auto_lock_time";
    public static final String PASSCODE_PASSWORD = "passcode_password";
    public static final String LOCKED_EXPLICITLY = "locked_explicitly";

    private final Context ctx;
    private final SharedPreferences prefs;

    @Inject
    public PasscodeManager(Context ctx) {
        this.ctx = ctx;
        prefs = ctx.getSharedPreferences("PassCodeManager", Context.MODE_PRIVATE);
    }

    public void onPause(boolean isLocked) {
        if (!isLocked) {//save only last unlocked pause
            prefs.edit()
                    .putLong(LAST_PAUSE, currentTime())
                    .apply();
        }
    }

    private long currentTime() {
        return SystemClock.elapsedRealtime();
    }

    public void onResume(Callback cb) {
        //todo manual lock
        if (!passCodeEnabled()) {
            return;
        }

        if (shouldIgnoreTime()) {
            cb.lockUI();
            setShouldLockOnceAnyway(false);
        } else if (isLocked()){
            cb.lockUI();
        } else {
            final long lastPauseTime = prefs.getLong(LAST_PAUSE, -1);
            final long autoLockDurationMillis = getAutoLockTime();
            if (autoLockDurationMillis == 0) {
                return;
            }
            if (lastPauseTime == -1 || autoLockDurationMillis == -1) {
                cb.lockUI();
            } else {
                final Duration autoLockDuration = new Duration(autoLockDurationMillis);
                final Duration timePast = new Duration(lastPauseTime, currentTime());
                if (timePast.isLongerThan(autoLockDuration)) {
                    cb.lockUI();
                }
            }
        }
    }

    private boolean shouldIgnoreTime() {
        return prefs.getBoolean(IGNORE_LAST_SAVED_TIME, false);
    }

    public boolean passCodeEnabled() {
        return prefs.getBoolean(PASSCODE_ENABLED, false);
    }

    public void setPasscodeEnabled(boolean value) {
        prefs.edit()
                .putBoolean(PASSCODE_ENABLED, value)
                .commit();
    }

    @SuppressLint("CommitPrefEdits")
    public void setShouldLockOnceAnyway(boolean shouldIgnore) {
        prefs.edit()
                .putBoolean(IGNORE_LAST_SAVED_TIME, shouldIgnore)
                .commit();
    }

    public void setPassword(String firstPassword) {
        prefs.edit()
                .putString(PASSCODE_PASSWORD, firstPassword)
                .commit();
    }

    public String getPassword() {
        return prefs.getString(PASSCODE_PASSWORD, "");
    }

    public void setAutoLockTiming(long timing) {
        prefs
                .edit()
                .putLong(AUTO_LOCK_TIME, timing)
                .commit();
    }

    //return auto lock time or zero(means disabled)
    public long getAutoLockTime() {
        return prefs.getLong(AUTO_LOCK_TIME, 0);
    }

    public boolean isLocked() {
        return prefs.getBoolean(LOCKED_EXPLICITLY, false);
    }

    public void setLocked(boolean locked) {
        prefs.edit()
                .putBoolean(LOCKED_EXPLICITLY, locked)
                .commit();
    }

    public boolean unlock(String s) {
        if (s.equals(getPassword())){
            setLocked(false);
            return true;
        }
        return false;
    }

    public interface Callback {
        void lockUI();
    }
}
