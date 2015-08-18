package ru.korniltsev.telegram.profile.media;

import dagger.Provides;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(SharedMediaPath.Module.class)
public class SharedMediaPath extends BasePath implements Serializable{
    final long chatId;
    public SharedMediaPath(long chatId) {
        this.chatId = chatId;
    }

    @Override
    public int getRootLayout() {
        return R.layout.profile_chat_title;
    }


    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    SharedMediaView.class,
            }
    )
    public static final class Module {
        final SharedMediaPath path;

        public Module(SharedMediaPath path) {
            this.path = path;
        }

        @Provides
        SharedMediaPath providePath() {
            return path;
        }
    }
}
