package ru.korniltsev.telegram.profile.chat;

import android.support.annotation.Nullable;
import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@WithModule(ChatInfo.Module.class)
public class ChatInfo extends BasePath implements Serializable{
    public final TdApi.GroupChatFull chatFull;
    public final TdApi.Chat chat;

    public List<TdApi.User> addedUsers = new ArrayList<>();


    public ChatInfo(TdApi.GroupChatFull chatFull, TdApi.Chat chat) {
        this.chatFull = chatFull;
        this.chat = chat;
    }


    @Override
    public int getRootLayout() {
        return R.layout.chat_info_view;
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
