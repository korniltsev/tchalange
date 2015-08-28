package ru.korniltsev.telegram.chat;

import android.support.annotation.Nullable;
import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.adapter.view.GifView;
import ru.korniltsev.telegram.chat.adapter.view.VoiceMessageView;
import ru.korniltsev.telegram.chat.adapter.view.DocumentView;
import ru.korniltsev.telegram.chat.adapter.view.ForwardedMessageView;
import ru.korniltsev.telegram.chat.adapter.view.GeoPointView;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.chat.adapter.view.PhotoMessageView;
import ru.korniltsev.telegram.chat.adapter.view.StickerView;
import ru.korniltsev.telegram.chat.adapter.view.VideoView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarflow.NamedPath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.ModuleFactory2;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.emoji.EmojiKeyboardView;
import ru.korniltsev.telegram.emoji.strip.EmojiPagerStripView;

import java.io.Serializable;


public class Chat extends BasePath implements Serializable, NamedPath, ModuleFactory2 {

    public static final int LIMIT = 15;

    public final TdApi.Chat chat;
    public final TdApi.User me;
    public final TdApi.ForwardMessages forwardMessages;
    public transient boolean firstLoad = true;

    public @Nullable TdApi.User sharedContact;

    public Chat(TdApi.Chat chat, TdApi.User me, TdApi.ForwardMessages  messagesToForward) {
        this.chat = chat;
        this.me = me;
        this.forwardMessages = messagesToForward;
    }

    @Override
    public int getRootLayout() {
        return R.layout.chat_view;
    }

    @Override
    public String name() {
        return String.valueOf(chat.id);
    }

    @Override
    public Object createDaggerModule() {
        return new Module(this);
    }

    @dagger.Module(
            injects = {
                    ChatView.class,
                    PhotoMessageView.class,
                    VoiceMessageView.class ,
//                    GeoPointView.class ,
//                    VideoView.class ,
//                    GifView.class ,
//                    DocumentView.class ,
//                    StickerView.class ,
                    MessagePanel.class ,
                    ForwardedMessageView.class ,
//                    CustomCeilLayout.class,
                    VoiceRecordingOverlay.class,
//                    EmojiKeyboardView.class,
//                    EmojiPagerStripView.class,

            },
            addsTo = RootModule.class)
    public static class Module {
        final Chat chat;

        public Module(Chat chat) {
            this.chat = chat;
        }

        @Provides Chat provideChat() {
            return chat;
        }
    }
}
