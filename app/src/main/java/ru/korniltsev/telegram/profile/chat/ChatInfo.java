package ru.korniltsev.telegram.profile.chat;

import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.ModuleFactory2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class ChatInfo extends BasePath implements Serializable, ModuleFactory2 {
    public final long chatId;
    public List<TdApi.User> addedUsers = new ArrayList<>();


    public ChatInfo(TdApi.Chat chat) {
        this.chatId = chat.id;
    }


    @Override
    public int getRootLayout() {
        return R.layout.chat_info_view;
    }

    @Override
    public Object createDaggerModule() {
        return new Module(this);
    }

    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    ChatInfoView.class,
                    FakeToolbar.class,
            }
    )
    public static final class Module {
        final ChatInfo path;

        public Module(ChatInfo path) {
            this.path = path;
        }

        @Provides
        ChatInfo providePath() {
            return path;
        }
    }
}
