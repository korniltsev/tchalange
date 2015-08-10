package ru.korniltsev.telegram.profile.chat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.AttachPanelPopup;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat_list.ChatList;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.contacts.ContactList;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.mortar.ActivityResult;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.profile.edit.chat.title.EditChatTitlePath;
import rx.Observable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static ru.korniltsev.telegram.common.AppUtils.getTmpFileForCamera;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class ChatInfoPresenter extends ViewPresenter<ChatInfoView> implements ChatInfoAdapter.CallBack, AttachPanelPopup.Callback {
    final ChatInfo path;
    final ActivityOwner owner;
    final NotificationManager notifications;
    final RXClient client;
    private CompositeSubscription subscriptions;

    TdApi.Chat chat;

    @Inject
    public ChatInfoPresenter(ChatInfo path, ActivityOwner owner, NotificationManager notifications, RXClient client) {
        this.path = path;
        this.owner = owner;
        this.notifications = notifications;
        this.client = client;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        try {
            chat = (TdApi.Chat) client.sendRxBlocking(new TdApi.GetChat(path.chatId));
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
            return;
        }
        subscriptions = new CompositeSubscription();
        getView().bindUser(path, chat);
        //todo
        final boolean muted = chat.notificationSettings.muteFor > 0;//notifications.isMuted(chat.notificationSettings);
        getView().bindMuteMenu(muted);

        subscriptions.add(
                owner.activityResult().subscribe(new ObserverAdapter<ActivityResult>() {
                    @Override
                    public void onNext(ActivityResult response) {
                        onActivityResult(response);
                    }
                }));
        subscriptions.add(client
                .getGlobalObservableWithBackPressure()
                .compose(new RXClient.FilterAndCastToClass<>(TdApi.UpdateChatPhoto.class))
                .filter(new Func1<TdApi.UpdateChatPhoto, Boolean>() {
                    @Override
                    public Boolean call(TdApi.UpdateChatPhoto updateChatPhoto) {
                        return updateChatPhoto.chatId == chat.id;
                    }
                })
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateChatPhoto>() {
                    @Override
                    public void onNext(TdApi.UpdateChatPhoto response) {
                        final TdApi.GroupChatInfo type = (TdApi.GroupChatInfo) chat.type;
                        type.groupChat.photo = response.photo;
                        getView()
                                .bindChatAvatar(chat);
                    }
                }));
        subscriptions.add(client
                .getGlobalObservableWithBackPressure()
                .compose(new RXClient.FilterAndCastToClass<>(TdApi.UpdateChatTitle.class))
                .filter(new Func1<TdApi.UpdateChatTitle, Boolean>() {
                    @Override
                    public Boolean call(TdApi.UpdateChatTitle updateChatPhoto) {
                        return updateChatPhoto.chatId == chat.id;
                    }
                })
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateChatTitle>() {
                    @Override
                    public void onNext(TdApi.UpdateChatTitle response) {
                        getView().setChatTitle(response.title);
                    }
                }));
    }

    private void onActivityResult(ActivityResult response) {
        int result = response.result;
        int request = response.request;
        if (result != Activity.RESULT_OK) {
            return;
        }
        if (request == AppUtils.REQUEST_TAKE_PHOTO_CHAT_AVATAR) {
            File f = AppUtils.getTmpFileForCamera();
            if (f.exists()) {
                setAvatarImage(f.getAbsolutePath());
                getView()
                        .hideAttachPannel();
            }
        } else if (request == AppUtils.REQUEST_CHOOS_FROM_GALLERY_CHAT_AVATAR) {
            String picturePath = Utils.getGalleryPickedFilePath(getView().getContext(), response.data);
            if (picturePath != null) {
                setAvatarImage(picturePath);
                getView()
                        .hideAttachPannel();
            }
        }
    }

    @Override
    public void dropView(ChatInfoView view) {
        super.dropView(view);
        subscriptions.unsubscribe();
    }

    @Override
    public void btnAddMemberClicked() {
        Flow.get(getView())
                .set(new ContactList(createFilter(), chat));
    }

    private List<TdApi.User> createFilter() {
        final ArrayList<TdApi.User> res = new ArrayList<>();
        for (TdApi.ChatParticipant p : path.chatFull.participants) {
            res.add(p.user);
        }
        return res;
    }

    @Override
    public void participantClicked(ChatInfoAdapter.ParticipantItem item) {

    }

    public void deleteAndLeave() {
        subscriptions.add(
                client.sendRx(new TdApi.DeleteChatHistory(chat.id))
                        .flatMap(new Func1<TdApi.TLObject, Observable<TdApi.User>>() {
                            @Override
                            public Observable<TdApi.User> call(TdApi.TLObject tlObject) {
                                return client.getMe();
                            }
                        })
                        .flatMap(new Func1<TdApi.User, Observable<TdApi.TLObject>>() {
                            @Override
                            public Observable<TdApi.TLObject> call(TdApi.User me) {
                                return client.sendRx(new TdApi.DeleteChatParticipant(chat.id, me.id));
                            }
                        })
                        .observeOn(mainThread())
                        .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                            @Override
                            public void onNext(TdApi.TLObject response) {
                                goBackTwice();
                            }
                        }));
    }

    private void goBackTwice() {
        AppUtils.flowPushAndRemove(getView(), null, new FlowHistoryStripper() {
            @Override
            public boolean shouldRemovePath(Object path) {
                return !(path instanceof ChatList);
            }
        }, Flow.Direction.BACKWARD);

    }

    public void editChatName() {
        getView().post(new Runnable() {
            @Override
            public void run() {
                Flow.get(getView()).set(new EditChatTitlePath(chat.id));
            }
        });

    }

    public void muteFor(int durationSeconds) {
        notifications.muteChat(chat, durationSeconds);
        final boolean muted = durationSeconds > 0;
        getView().bindMuteMenu(muted);
    }

    public void changePhoto() {
        AppUtils.toastUnsupported(getView().getContext());
    }

    @Override
    public void sendImages(List<String> selectedImages) {
        if (selectedImages.size() == 1) {
            final String first = selectedImages.get(0);
            setAvatarImage(first);
            getView().hideAttachPannel();
        }
    }

    private void setAvatarImage(String first) {
        client.setChatAvatar(chat.id, first);
    }

    @Override
    public void chooseFromGallery() {
        String title = getView().getResources().getString(R.string.select_picture);
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        owner.expose()
                .startActivityForResult(Intent.createChooser(intent, title), AppUtils.REQUEST_CHOOS_FROM_GALLERY_CHAT_AVATAR);
    }

    @Override
    public void takePhoto() {
        File f = getTmpFileForCamera();
        f.delete();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        owner.expose()
                .startActivityForResult(intent, AppUtils.REQUEST_TAKE_PHOTO_CHAT_AVATAR);
    }
}
