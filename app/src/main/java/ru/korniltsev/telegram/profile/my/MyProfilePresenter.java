package ru.korniltsev.telegram.profile.my;

import android.os.Bundle;
import android.support.annotation.Nullable;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChat;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MyProfilePresenter extends ViewPresenter<MyProfileView> {
    final MyProfilePath path;
    final RXClient client;

    @Inject
    public MyProfilePresenter(MyProfilePath path, RXClient client) {
        this.path = path;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        getView().bindUser(path.user);

    }

    public void logout() {
        client.logout();
    }

    public void editName() {

    }
}
