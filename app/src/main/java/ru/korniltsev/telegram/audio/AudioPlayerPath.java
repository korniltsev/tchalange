package ru.korniltsev.telegram.audio;

import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

import static junit.framework.Assert.assertTrue;

@WithModule(AudioPlayerPath.Module.class)
public class AudioPlayerPath extends BasePath implements Serializable {


    public AudioPlayerPath() {

    }

    @Override
    public int getRootLayout() {
        return R.layout.audio_player_view;
    }

    @dagger.Module(
            injects = {
                    ru.korniltsev.telegram.audio.AudioPlayerView.class,
            },
            addsTo = RootModule.class)
    public static class Module {
        final AudioPlayerPath path;

        public Module(AudioPlayerPath path) {
            this.path = path;
        }

        @Provides
        public AudioPlayerPath providePath() {
            return path;
        }
    }
}
