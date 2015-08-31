package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.os.SystemClock;
import com.crashlytics.android.core.CrashlyticsCore;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.utils.Preconditions;
import ru.korniltsev.telegram.utils.R;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static rx.android.schedulers.AndroidSchedulers.mainThread;


public class NotificationManager {
    public static final int NOTIFICATIONS_ENABLED = 0;
    public static final int NOTIFICATIONS_DISABLED_FOREVER = (int) TimeUnit.DAYS.toSeconds(365);
    final RXClient client;
    final Context ctx;
//    private final Ringtone ringtone;
    private final Observable<TdApi.UpdateNotificationSettings> settingsUpdate;
//    private final Uri notification;
    private final ThreadLocal<Ringtone> notification;
    private RXAuthState.AuthState state;
    final ThreadLocal<InChatRingtone> inChatRingtone;


    public NotificationManager(RXClient client, final Context ctx, RXAuthState auth) {
        this.client = client;
        this.ctx = ctx;

        notification = new ThreadLocal<Ringtone>(){
            @Override
            protected Ringtone initialValue() {
                return RingtoneManager.getRingtone(ctx,
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        };
        inChatRingtone = new ThreadLocal<InChatRingtone>(){
            @Override
            protected InChatRingtone initialValue() {
                return new InChatRingtone(ctx);
            }
        };

//        alarm = new ThreadLocal<Ringtone>(){
//            @Override
//            protected Ringtone initialValue() {
//                return RingtoneManager.getRingtone(ctx,
//                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
//            }
//        };

        state = auth.getState();
        auth.listen().subscribe(new Action1<RXAuthState.AuthState>() {
            @Override
            public void call(RXAuthState.AuthState authState) {
                state = authState;
            }
        });

        settingsUpdate = client.updateNotificationSettings()
                .map(new Func1<TdApi.UpdateNotificationSettings, TdApi.UpdateNotificationSettings>() {
                    @Override
                    public TdApi.UpdateNotificationSettings call(TdApi.UpdateNotificationSettings updateNotificationSettings) {
                        calculate(updateNotificationSettings.notificationSettings);
                        return updateNotificationSettings;
                    }
                })
                .observeOn(mainThread());
        settingsUpdate.subscribe(new ObserverAdapter<TdApi.UpdateNotificationSettings>() {
            @Override
            public void onNext(TdApi.UpdateNotificationSettings upd) {
                if (upd.scope instanceof TdApi.NotificationSettingsForChat) {
                    TdApi.NotificationSettingsForChat scope = (TdApi.NotificationSettingsForChat) upd.scope;
                    settings.put(scope.chatId, upd.notificationSettings);
                    //                    calculate(scope.chatId, upd.notificationSettings);
                } //else todo
            }
        });
    }

    public Observable<TdApi.NotificationSettings> updatesForChat(final TdApi.Chat c) {
        return settingsUpdate.filter(new Func1<TdApi.UpdateNotificationSettings, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateNotificationSettings updateNotificationSettings) {
                if (updateNotificationSettings.scope instanceof TdApi.NotificationSettingsForChat) {
                    TdApi.NotificationSettingsForChat scope = (TdApi.NotificationSettingsForChat) updateNotificationSettings.scope;
                    return scope.chatId == c.id;
                } else {
                    return false;
                }
            }
        }).map(new Func1<TdApi.UpdateNotificationSettings, TdApi.NotificationSettings>() {
            @Override
            public TdApi.NotificationSettings call(TdApi.UpdateNotificationSettings updateNotificationSettings) {
                return updateNotificationSettings.notificationSettings;
            }
        });
    }

    Map<Long, TdApi.NotificationSettings> settings = new HashMap<>();

    public void updateNotificationScopes(List<TdApi.Chat> csList) {
        for (TdApi.Chat chat : csList) {
            long id = chat.id;
            TdApi.NotificationSettings s = chat.notificationSettings;
            calculate( s);
            settings.put(id, s);
        }
    }

    private void calculate(TdApi.NotificationSettings s) {
        long time = time();
        int secsToMute = s.muteFor;
        s.muteForElapsedRealtime = time + secsToMute * 1000;

    }

    private long time() {
        return SystemClock.elapsedRealtime();
    }

    public void notifyOnce(List<TdApi.UpdateNewMessage> ms){
        for (TdApi.UpdateNewMessage m : ms) {
            if (notifyNewMessage(m.message)){
                break;
            }
        }
    }
    public boolean notifyNewMessage(TdApi.Message msg) {
        if (state == null || !(state instanceof RXAuthState.StateAuthorized)){
            return false;
        }
        int myId = ((RXAuthState.StateAuthorized) state).id;
        if (myId == msg.fromId){//do not notify new messages from self
            return false;
        }

        TdApi.NotificationSettings s = this.settings.get(msg.chatId);
        if (s == null) {
            notifyNewMessageImpl();
            return true;
        } else {
            if (!isMuted(s)) {
                notifyNewMessageImpl();
                return true;
            }
        }
        return false;
    }

    public boolean isMuted(TdApi.NotificationSettings s) {
        if (s.muteFor == 0){
            return false;
        }
        return time() <= s.muteForElapsedRealtime;
    }



    public boolean isMuted(TdApi.Chat chat) {
        TdApi.NotificationSettings s = this.settings.get(chat.id);
        return s != null && isMuted(s);
    }

//    public void mute(TdApi.Chat chat) {
//        chat.notificationSettings.muteFor = 8 * 60 * 60;
//        setSettings(chat);
//    }
//
//
//
//    public void unmute(TdApi.Chat chat) {
//        chat.notificationSettings.muteFor = 0;
//        setSettings(chat);
//    }

//    private void setSettings(TdApi.Chat chat) {
//        TdApi.NotificationSettingsForChat scope = new TdApi.NotificationSettingsForChat(chat.id);
//        calculate(chat.notificationSettings);
//        settings.put(chat.id, chat.notificationSettings);
//        client.sendSilently(new TdApi.SetNotificationSettings(scope, chat.notificationSettings));
//    }

    public void muteChat(TdApi.Chat chat, int durationMillis) {
        Preconditions.checkMainThread();
        TdApi.NotificationSettingsForChat scope = new TdApi.NotificationSettingsForChat(chat.id);
        chat.notificationSettings.muteFor = durationMillis;
        calculate(chat.notificationSettings);
        settings.put(chat.id, chat.notificationSettings);
        client.sendSilently(new TdApi.SetNotificationSettings(scope, chat.notificationSettings));
    }

    private void notifyNewMessageImpl() {
        if (resumed){
            try {
                inChatRingtone.get().play();
            } catch (Exception e) {
                CrashlyticsCore.getInstance().logException(e);
            }
        } else {
            try {
                notification.get().play();
            } catch (Exception e) {
                CrashlyticsCore.getInstance().logException(e);
            }
        }
    }

    boolean resumed = false;

    public void onResume() {
        resumed = true;
    }

    public void onPause() {
        resumed = false;
    }

//    final Map<Long, Boolean> chatIdToResumed = new HashMap<>();
//
//    private boolean isChat
//    public void onLoad(long chatId) {
//        chatIdToResumed.put(chatId, Boolean.TRUE);
//    }
//
//    public void dropView(long chatId) {
//        chatIdToResumed.put(chatId, Boolean.FALSE);
//    }


    class InChatRingtone {
        private final SoundPool soundPool;
        private final int ringtoneId;

        public InChatRingtone(Context ctx) {
            soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    soundPool.play(ringtoneId, 1.0f, 1.0f, 1, 0, 1.0f);
                }
            });
            ringtoneId = soundPool.load(ctx, R.raw.click42, 0);
        }

        boolean first = true;
        public void play() {
            if (first){
                first = false;
                return;
            }
            soundPool.play(ringtoneId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }
}
