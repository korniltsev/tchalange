package ru.korniltsev.telegram.profile.media;

import dagger.Provides;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarflow.NamedPath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(SharedMediaPath.Module.class)
public class SharedMediaPath extends BasePath implements Serializable, NamedPath{

    public static final int TYPE_MEDIA = 0;
    public static final int TYPE_AUDIO = 1;

    public final long chatId;
    public final int type;

    transient int loadCount = 0;

    public SharedMediaPath(long chatId, int type) {
        this.chatId = chatId;
        this.type = type;
    }

    @Override
    public int getRootLayout() {
        return R.layout.shared_media;
    }

    @Override
    public String name() {
        return String.valueOf(type);
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
