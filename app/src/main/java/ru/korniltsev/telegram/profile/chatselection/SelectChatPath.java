package ru.korniltsev.telegram.profile.chatselection;

import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
//import ru.korniltsev.telegram.chat_list.view.DividerRelativeLayout;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(SelectChatPath.Module.class)
public class SelectChatPath extends BasePath implements Serializable{
    public final TdApi.User user;
    public final TdApi.User me;

    public SelectChatPath(TdApi.User user, TdApi.User me) {
        this.user = user;
        this.me = me;
    }


    @Override
    public int getRootLayout() {
        return R.layout.select_chat_view;
    }


    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    SelectChatView.class,
//                    DividerRelativeLayout.class,
            }
    )
    public static final class Module {
        final SelectChatPath path;

        public Module(SelectChatPath path) {
            this.path = path;
        }

        @Provides
        SelectChatPath providePath() {
            return path;
        }
    }
}
