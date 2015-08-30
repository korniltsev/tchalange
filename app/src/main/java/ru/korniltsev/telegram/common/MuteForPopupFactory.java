package ru.korniltsev.telegram.common;

import android.app.Activity;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.NotificationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MuteForPopupFactory {

    public static ListChoicePopup create(Activity ctx, final Callback cb){
        final List<ListChoicePopup.Item> items = new ArrayList<>();
        items.add(new ListChoicePopup.Item(ctx.getString(R.string.notifications_enabled), new Runnable() {
            @Override
            public void run() {
                cb.muteFor(NotificationManager.NOTIFICATIONS_ENABLED);

            }
        }));
        items.add(new ListChoicePopup.Item(ctx.getResources().getQuantityString(R.plurals.mute_for_n_hours, 1, 1), new Runnable() {
            @Override
            public void run() {
                cb.muteFor((int) TimeUnit.HOURS.toSeconds(1));
            }
        }));
        items.add(new ListChoicePopup.Item(ctx.getResources().getQuantityString(R.plurals.mute_for_n_hours, 2, 2), new Runnable() {
            @Override
            public void run() {
                cb.muteFor((int) TimeUnit.HOURS.toSeconds(2));
            }
        }));
        items.add(new ListChoicePopup.Item(ctx.getResources().getQuantityString(R.plurals.mute_for_n_days, 2, 2), new Runnable() {
            @Override
            public void run() {
                cb.muteFor((int) TimeUnit.DAYS.toSeconds(2));
            }
        }));
        items.add(new ListChoicePopup.Item(ctx.getString(R.string.notifications_disabled), new Runnable() {
            @Override
            public void run() {
                cb.muteFor(NotificationManager.NOTIFICATIONS_DISABLED_FOREVER);
            }
        }));
        return ListChoicePopup.create(ctx, items);
    }

     public interface Callback {
        void muteFor(int duration);
    }
}
