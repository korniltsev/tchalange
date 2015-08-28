/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.korniltsev.telegram.core.app;

import android.app.DownloadManager;
import android.content.Context;
import dagger.Module;
import dagger.Provides;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.core.audio.VoicePlayer;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.images.Emoji;
import ru.korniltsev.telegram.core.emoji.Stickers;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.passcode.BootReceiver;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.rx.EmojiParser;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.rx.VoiceRecorder;
import ru.korniltsev.telegram.core.views.DownloadView;

import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Module(
        injects = {
                RXClient.class,
                RXAuthState.class,

                RxGlide.class,

                Emoji.class,
                RxDownloadManager.class,
                NotificationManager.class,
                VoicePlayer.class,
                DpCalculator.class,
                ChatDB.class,
                ActivityOwner.class,
                Stickers.class,
                DownloadView.class,
                BootReceiver.class  ,
                PasscodeManager.class,
                UserHolder.class ,
                PhoneFormat.class
        },
        library = true)
public class RootModule {
    private Context ctx;
    private final DpCalculator dpCalculator;
    private final RXClient rxClient;
    private final RXAuthState rxAuthState;
    private final UserHolder userHolder;
    private final RxDownloadManager downloadManager;
    private final RxGlide rxGlide;
    private final Stickers stickers;
    private final NotificationManager notificationManager;
    private final ChatDB chatDb;
    private final PasscodeManager passcodeManager;
    private final ActivityOwner activityOwner;
    private final VoiceRecorder voiceRecorder;
    private final VoicePlayer voicePlayer;

    public RootModule(Context ctx, DpCalculator dpCalculator, RXClient rxClient, RXAuthState rxAuthState,
                      UserHolder userHolder, RxDownloadManager downloadManager, RxGlide rxGlide,
                      Stickers stickers, NotificationManager notificationManager, ChatDB chatDb,
                      PasscodeManager passcodeManager, ActivityOwner activityOwner, VoiceRecorder voiceRecorder,
                      VoicePlayer voicePlayer) {
        this.ctx = ctx;
        this.dpCalculator = dpCalculator;
        this.rxClient = rxClient;
        this.rxAuthState = rxAuthState;
        this.userHolder = userHolder;
        this.downloadManager = downloadManager;
        this.rxGlide = rxGlide;
        this.stickers = stickers;
        this.notificationManager = notificationManager;
        this.chatDb = chatDb;
        this.passcodeManager = passcodeManager;
        this.activityOwner = activityOwner;
        this.voiceRecorder = voiceRecorder;
        this.voicePlayer = voicePlayer;
    }

    @Provides
    @Singleton
    public VoicePlayer voicePlayer(){
        return voicePlayer;
    }

    @Provides
    @Singleton
    public VoiceRecorder provideVoiceRecorder(){
        return voiceRecorder;
    }


    @Provides
    @Singleton
    public PasscodeManager providePasscodeManager(){
        return passcodeManager;
    }

    @Provides
    @Singleton
    public ChatDB provideChatDB() {
        return chatDb;
    }
    @Provides
    @Singleton
    public NotificationManager provideNotificationsManager() {
        return notificationManager;
    }

    @Provides
    @Singleton
    public Stickers provideStickers() {
        return stickers;
    }

    @Provides
    @Singleton
    public RxGlide provideRxGlide() {
        return rxGlide;
    }

    @Provides
    @Singleton
    public RxDownloadManager provideDownlaodManger() {
        return downloadManager;
    }
    @Provides
    @Singleton
    public UserHolder provideUserHolder() {
        return userHolder;
    }

    @Singleton
    @Provides RXAuthState provideAuthState() {
        return rxAuthState;
    }

    @Singleton
    @Provides
    RXClient provideRxClient(){
        return rxClient;
    }

    @Singleton
    @Provides
    Context provideContext(){
        return ctx;
    }


    @Singleton
    @Provides
    PhoneFormat providePhoneFormat() {
        return new PhoneFormat(ctx);
    }



    @Singleton
    @Provides
    DpCalculator provideDpCalc(){
        return dpCalculator;
    }



    @Provides @Singleton ActivityOwner provideActivityOwner() {
        return activityOwner;
    }


}
