package ru.korniltsev.telegram.photoview;

import android.graphics.Color;
import android.support.annotation.Nullable;
import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;

import java.io.Serializable;

import static junit.framework.Assert.assertTrue;

public class PhotoView extends BasePath implements Serializable {

    public static final int NO_MESSAGE = 0;
    @Nullable public final TdApi.Photo photo;
    public final int messageId;
    public final long chatId;
    @Nullable public final TdApi.ProfilePhoto profilePhoto;

    public PhotoView(TdApi.Photo photo) {
        profilePhoto = null;
        this.photo = photo;
        messageId = NO_MESSAGE;
        chatId = NO_MESSAGE;
    }

    public PhotoView(TdApi.ProfilePhoto photo) {
        this.profilePhoto = photo;
        this.photo = null;
        messageId = NO_MESSAGE;
        chatId = NO_MESSAGE;
    }

    public PhotoView(TdApi.Photo photo, int msgId, long chatId) {
        this.photo = photo;
        profilePhoto = null;

        this.messageId = msgId;
        this.chatId = chatId;
    }

    @Override
    public int getRootLayout() {
        return R.layout.photo_view_view;
    }

    @Override
    public Object createDaggerModule() {
        return new Module(this);

    }

    @dagger.Module(
            injects = {
                    PhotoViewView.class,
            },
            addsTo = RootModule.class)
    public static class Module {
        final PhotoView path;

        public Module(PhotoView path) {
            this.path = path;
        }

        @Provides
        public PhotoView providePath() {
            return path;
        }
    }

    @Override
    public int getBackgroundColor() {
        return Color.BLACK;
    }
}
