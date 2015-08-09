package ru.korniltsev.telegram.profile.my;

import android.content.Intent;
import android.os.Bundle;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.mortar.ActivityResult;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.main.passcode.PasscodePath;
import ru.korniltsev.telegram.profile.edit.name.EditNamePath;
import ru.korniltsev.telegram.profile.edit.passcode.EditPasscode;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MyProfilePresenter extends ViewPresenter<MyProfileView> {
    final MyProfilePath path;
    final RXClient client;
    final PasscodeManager passcodeManager;
    private final ActivityOwner owner;
    private CompositeSubscription subscription;

    @Inject
    public MyProfilePresenter(MyProfilePath path, RXClient client, PasscodeManager passcodeManager, ActivityOwner owner) {
        this.path = path;
        this.client = client;
        this.passcodeManager = passcodeManager;
        this.owner = owner;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        subscription = new CompositeSubscription();
        try {
            getView()
                    .bindUser(
                            client.getMeBlocking(), passcodeManager.passCodeEnabled());
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }

        subscription.add(owner.activityResult().subscribe(new ObserverAdapter<ActivityResult>() {
            @Override
            public void onNext(ActivityResult response) {
                onActivityResult(response);
            }
        }));
    }

    private void onActivityResult(ActivityResult response) {
        System.out.println("asdasd");
    }

    @Override
    public void dropView(MyProfileView view) {
        super.dropView(view);
        subscription.unsubscribe();
    }

    public void logout() {
        client.logout();
    }

    public void editName() {
        getView().post(new Runnable() {
            @Override
            public void run() {
                Flow.get(getView())
                        .set(new EditNamePath());
            }
        });
    }

    public void passcodeClicked() {
        if (passcodeManager.passCodeEnabled()) {
            Flow.get(getView())
                    .set(new PasscodePath(PasscodePath.TYPE_LOCK_TO_CHANGE));
        } else {
            Flow.get(getView())
                    .set(new EditPasscode());
        }
    }

    public void changePhoto() {
        String title = getView().getContext().getString(R.string.select_picture);
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        owner.expose()
                .startActivityForResult(Intent.createChooser(intent, title), AppUtils.REQUEST_CHOOS_FROM_GALLERY_MY_AVATAR);
    }
}
