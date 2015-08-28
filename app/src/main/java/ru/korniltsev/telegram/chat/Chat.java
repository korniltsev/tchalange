package ru.korniltsev.telegram.chat;

import android.content.Context;
import android.support.annotation.Nullable;
import dagger.Provides;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.adapter.view.VoiceMessageView;
import ru.korniltsev.telegram.chat.adapter.view.ForwardedMessageView;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.chat.adapter.view.PhotoMessageView;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.ViewPresenterHolder;
import ru.korniltsev.telegram.core.mortar.mortarflow.NamedPath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.ModuleFactory2;

import java.io.Serializable;


public class Chat extends BasePath implements Serializable, NamedPath, ModuleFactory2 , ViewPresenterHolder.Factory {


    public static final int LIMIT = 15;

    public final TdApi.Chat chat;
    public final TdApi.User me;
    public final TdApi.ForwardMessages forwardMessages;
    public transient boolean firstLoad = true;

    public @Nullable TdApi.User sharedContact;

    public Chat(TdApi.Chat chat, TdApi.User me, TdApi.ForwardMessages  messagesToForward) {
        this.chat = chat;
        this.me = me;
        this.forwardMessages = messagesToForward;
    }

    @Override
    public int getRootLayout() {
        return R.layout.chat_view;
    }

    @Override
    public String name() {
        return String.valueOf(chat.id);
    }

    @Override
    public Object createDaggerModule() {
        return new Module(this);
    }

    @Override
    public ViewPresenter create(Context ctx) {
        final MyApp app = MyApp.from(ctx);
        return new Presenter(this, app.rxClient, app.chatDb, app.notificationManager, app.activityOwner, app.userHolder);
    }

    @dagger.Module(
            injects = {
            },
            addsTo = RootModule.class)
    public static class Module {
        final Chat chat;

        public Module(Chat chat) {
            this.chat = chat;
        }


    }
}
