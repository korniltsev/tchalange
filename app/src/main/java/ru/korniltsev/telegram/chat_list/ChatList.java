package ru.korniltsev.telegram.chat_list;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import dagger.Provides;
//import ru.korniltsev.telegram.chat_list.view.DividerRelativeLayout;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RXAuthState;

import java.io.Serializable;

import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.chat_list.ChatListViewFactory.construct;

@WithModule(ChatList.Module.class)
public class ChatList extends BasePath implements Serializable {
    final RXAuthState.StateAuthorized myId;

    public ChatList(RXAuthState.StateAuthorized myId) {
        this.myId = myId;
    }

    @dagger.Module(injects = {
            ChatListView.class,
//            DividerRelativeLayout.class,
    }, addsTo = RootModule.class)
    public static class Module {
        final ChatList path;

        public Module(ChatList path) {
            this.path = path;
        }

        @Provides
        ChatList providePath() {
            return path;
        }
    }

    @Override
    public int getRootLayout() {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public View constructViewManually(Context ctx, FrameLayout root) {
        return construct(ctx);
    }


}
