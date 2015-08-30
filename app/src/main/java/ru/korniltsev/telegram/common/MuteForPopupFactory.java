package ru.korniltsev.telegram.common;

import android.app.Activity;
import android.support.annotation.NonNull;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.profile.media.DropdownPopup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MuteForPopupFactory {

    @NonNull public static DropdownPopup create(Activity ctx, final Callback cb){

        final List<DropdownPopup.Item> items = new ArrayList<>();
        items.add(new DropdownPopup.Item(ctx.getString(R.string.notifications_enabled), new Runnable() {
            @Override
            public void run() {
                cb.muteFor(NotificationManager.NOTIFICATIONS_ENABLED);
            }
        }));
        items.add(new DropdownPopup.Item(ctx.getResources().getQuantityString(R.plurals.mute_for_n_hours, 1, 1), new Runnable() {
            @Override
            public void run() {
                cb.muteFor((int) TimeUnit.HOURS.toSeconds(1));
            }
        }));
        items.add(new DropdownPopup.Item(ctx.getResources().getQuantityString(R.plurals.mute_for_n_hours, 2, 2), new Runnable() {
            @Override
            public void run() {
                cb.muteFor((int) TimeUnit.HOURS.toSeconds(2));
            }
        }));
        items.add(new DropdownPopup.Item(ctx.getResources().getQuantityString(R.plurals.mute_for_n_days, 2, 2), new Runnable() {
            @Override
            public void run() {
                cb.muteFor((int) TimeUnit.DAYS.toSeconds(2));
            }
        }));
        items.add(new DropdownPopup.Item(ctx.getString(R.string.notifications_disabled), new Runnable() {
            @Override
            public void run() {
                cb.muteFor(NotificationManager.NOTIFICATIONS_DISABLED_FOREVER);
            }
        }));



        return new DropdownPopup(ctx, items);

    }

     public interface Callback {
        void muteFor(int duration);
    }
}
