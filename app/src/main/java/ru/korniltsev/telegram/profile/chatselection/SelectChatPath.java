package ru.korniltsev.telegram.profile.chatselection;

import android.support.annotation.Nullable;
import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
//import ru.korniltsev.telegram.chat_list.view.DividerRelativeLayout;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(SelectChatPath.Module.class)
public class SelectChatPath extends BasePath implements Serializable{
    @Nullable public final TdApi.User bot;
    @Nullable public final int[] messagesToForward;
    public final long forwardedMessaesFromChatId;
    public final TdApi.User me;
    public final boolean filterOnlyGroupChats;


    public SelectChatPath(@Nullable TdApi.User bot, @Nullable int[] messagesToForward, long forwardedMessaesFromChatId, TdApi.User me, boolean filterOnlyGroupChats) {
        this.forwardedMessaesFromChatId = forwardedMessaesFromChatId;
        if (bot == null && messagesToForward == null) {
            throw new NullPointerException("one of bot or messagesToForward should be non null");
        }
        this.bot = bot;
        this.messagesToForward = messagesToForward;
        this.me = me;
        this.filterOnlyGroupChats = filterOnlyGroupChats;
    }


    @Override
    public int getRootLayout() {
        return R.layout.select_chat_view;
    }


    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    SelectChatView.class,
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
