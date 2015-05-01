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
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxPicasso;

import javax.inject.Singleton;

/**
 * Defines app-wide singletons.
 */
@Module(
        injects = {
                RXClient.class,
                RXAuthState.class,
                RxPicasso.class
        },
        library = true)
public class RootModule {
    private Context ctx;

    public RootModule(Context ctx) {
        this.ctx = ctx;


    }

    @Singleton
    @Provides
    RXClient provideRxClient() {
        return new RXClient(ctx);
    }

    @Singleton
    @Provides
    RXAuthState provideRxAuthState(RXClient client) {
        return new RXAuthState(ctx, client);
    }

    @Singleton
    @Provides
    RxPicasso providePicasso(RXClient client, RXAuthState auth) {
        return new RxPicasso(ctx, client, auth);
    }
}
