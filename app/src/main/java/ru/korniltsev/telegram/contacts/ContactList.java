package ru.korniltsev.telegram.contacts;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import dagger.Provides;
import flow.path.Path;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.ForwardedMessageView;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;
import java.util.List;

@WithModule(ContactList.Module.class)
public class ContactList extends BasePath implements Serializable {
    public static final int TYPE_LIST = 0;
    public static final int TYPE_ADD_MEMBER = 1;
    public static final int TYPE_SHARE_USER = 2;

    final int type;

    //add
    @Nullable final List<TdApi.User> filter;
    @Nullable final TdApi.Chat chat;
    @Nullable final TdApi.User sharedUser;

    public ContactList( @NonNull List<TdApi.User> filter, @NonNull TdApi.Chat chat) {
        this.type = TYPE_ADD_MEMBER;
        this.filter = filter;
        this.chat = chat;
        sharedUser = null;
    }

    public ContactList() {
        this.type = TYPE_LIST;
        this.filter = null;
        this.chat = null;
        sharedUser = null;
    }

    public ContactList(@NonNull TdApi.User user) {
        this.type = TYPE_SHARE_USER;
        this.sharedUser = user;
        filter = null;
        chat = null;
    }

    @Override
    public int getRootLayout() {
        return R.layout.contacts_view;
    }

    @dagger.Module(
            injects = {
                    ContactListView.class,
            },
            addsTo = RootModule.class)
    public static class Module {
        final ContactList path;

        public Module(ContactList path) {
            this.path = path;
        }

        @Provides ContactList providePath(){
            return path;
        }
    }
}
