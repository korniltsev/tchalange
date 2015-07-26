package ru.korniltsev.telegram.profile.other;

import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(ProfilePath.Module.class)
public class ProfilePath extends BasePath implements Serializable{
    public final TdApi.User user;
    public final TdApi.Chat chat;

    public final TdApi.User me;
    //    @Nullable public final TdApi.Chat groupChat;

    public ProfilePath(TdApi.User user, TdApi.Chat chat, TdApi.User me) {
        this.user = user;
        this.chat = chat;
        this.me = me;
    }

//    public ProfilePath(TdApi.Chat groupChat) {
//        user = null;
//        this.groupChat = groupChat;
//    }

    @Override
    public int getRootLayout() {
        return R.layout.profile_view;
    }


    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    ProfileView.class,
                    FakeToolbar.class,
            }
    )
    public static final class Module {
        final ProfilePath path;

        public Module(ProfilePath path) {
            this.path = path;
        }

        @Provides ProfilePath providePath() {
            return path;
        }
    }
}
