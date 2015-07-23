package ru.korniltsev.telegram.core.passcode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import mortar.dagger1support.ObjectGraphService;

import javax.inject.Inject;

public class BootReceiver extends BroadcastReceiver {
    @Inject PasscodeManager passcodeManager;
    @Override
    public void onReceive(Context context, Intent intent) {
        ObjectGraphService.inject(context, this);
        passcodeManager.setShouldLockOnceAnyway(true);

    }
}
