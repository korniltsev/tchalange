package ru.korniltsev.telegram.audio;

import dagger.Provides;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;

import java.io.Serializable;


public class AudioPlayerPath extends BasePath implements Serializable {


    public AudioPlayerPath() {

    }

    @Override
    public int getRootLayout() {
        return R.layout.audio_player_view;
    }

    @Override
    public Object createDaggerModule() {
        return new Module(this);
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
