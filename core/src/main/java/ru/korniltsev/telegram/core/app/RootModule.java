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
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.UserHolder;
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

    public RootModule(Context ctx, DpCalculator dpCalculator, RXClient rxClient, RXAuthState rxAuthState, UserHolder userHolder) {
        this.ctx = ctx;
        this.dpCalculator = dpCalculator;
        this.rxClient = rxClient;
        this.rxAuthState = rxAuthState;
        this.userHolder = userHolder;
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
        return new ActivityOwner();
    }


}
