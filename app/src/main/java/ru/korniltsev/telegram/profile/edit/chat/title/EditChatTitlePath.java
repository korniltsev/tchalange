package ru.korniltsev.telegram.profile.edit.chat.title;

import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.ModuleFactory2;

import java.io.Serializable;


public class EditChatTitlePath extends BasePath implements Serializable, ModuleFactory2{
    final long chatId;
    public EditChatTitlePath( long chatId) {
        this.chatId = chatId;
    }

    @Override
    public int getRootLayout() {
        return R.layout.profile_chat_title;
    }

    @Override
    public Object createDaggerModule() {
        return new Module(this);
    }

    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    EditChatTitleView.class,
                    FakeToolbar.class,
            }
    )
    public static final class Module {
        final EditChatTitlePath path;

        public Module(EditChatTitlePath path) {
            this.path = path;
        }

        @Provides
        EditChatTitlePath providePath() {
            return path;
        }
    }
}
