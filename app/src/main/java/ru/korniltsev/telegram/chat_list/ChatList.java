package ru.korniltsev.telegram.chat_list;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
//import ru.korniltsev.telegram.chat_list.view.DividerRelativeLayout;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.ViewPresenterHolder;
import ru.korniltsev.telegram.core.mortar.mortarscreen.ModuleFactory2;
import ru.korniltsev.telegram.core.rx.RXAuthState;

import java.io.Serializable;

import static ru.korniltsev.telegram.chat_list.ChatListViewFactory.construct;


public class ChatList extends BasePath implements Serializable, ModuleFactory2 , ViewPresenterHolder.Factory{

    final RXAuthState.StateAuthorized myId;

    public ChatList(RXAuthState.StateAuthorized myId) {
        this.myId = myId;
    }

    @Override
    public Object createDaggerModule() {
        return new Module(this);
    }

    @Override
    public ViewPresenter create(Context ctx) {
        final MyApp app = MyApp.from(ctx);
        return new ChatListPresenter(this, app.rxClient, app.rxAuthState, app.passcodeManager, app.chatDb);
    }

    @dagger.Module(injects = {
//            ChatListView.class,
//            DividerRelativeLayout.class,
    }, addsTo = RootModule.class)
    public static class Module {
        final ChatList path;

        public Module(ChatList path) {
            this.path = path;
        }

//        @Provides
//        ChatList providePath() {
//            return path;
//        }
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
