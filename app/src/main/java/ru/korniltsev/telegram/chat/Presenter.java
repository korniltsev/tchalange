package ru.korniltsev.telegram.chat;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.AttachPanelPopup;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.chat.bot.BotCommandsAdapter;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.MuteForPopupFactory;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.mortar.ActivityResult;
import ru.korniltsev.telegram.core.rx.EmojiParser;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.rx.VoiceRecorder;
import ru.korniltsev.telegram.profile.chat.ChatInfo;
import ru.korniltsev.telegram.profile.chat.LeaveOnlyChatList;
import ru.korniltsev.telegram.profile.other.ProfilePath;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class Presenter extends ViewPresenter<ChatView>
        implements Toolbar.OnMenuItemClickListener,
        MessagePanel.OnSendListener,
        AttachPanelPopup.Callback

{

    private final Chat path;
    private final RXClient client;
    private final RxChat rxChat;
    private final NotificationManager nm;

    private final Observable<TdApi.GroupChatFull> fullChatInfoRequest;
    private final Observable<TdApi.UserFull> userFullRequest;
    private final boolean isGroupChat;
    @Nullable private final TdApi.User user;//null if groupChat
    private final TdApi.Chat chat;
    private TdApi.ForwardMessages forwardMessages;

    private CompositeSubscription subscription;
    private Subscription openChatSubscription = Subscriptions.empty();

    @Nullable private volatile TdApi.GroupChatFull mGroupChatFull;
    private int botCount;
    private boolean muted;

    public Chat getPath() {
        return path;
    }

    private final ActivityOwner owner;

    final UserHolder uerHolder;
    final ActivityOwner activity;

    public Presenter(Chat c, RXClient client, ChatDB chatDB, NotificationManager nm, ActivityOwner owner, UserHolder uerHolder) {
        path = c;
        this.client = client;
        this.nm = nm;
        this.owner = owner;
        this.uerHolder = uerHolder;
        this.activity = owner;
        this.chat = path.chat;
        rxChat = chatDB.getRxChat(chat.id);

        if (chat.type instanceof TdApi.GroupChatInfo) {
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) chat.type).groupChat;
            fullChatInfoRequest = rxChat.getGroupChatFull(groupChat.id);

            this.user = null;
            isGroupChat = true;
            userFullRequest = Observable.empty();
        } else {
            fullChatInfoRequest = Observable.empty();
            isGroupChat = false;
            final TdApi.PrivateChatInfo info = (TdApi.PrivateChatInfo) chat.type;
            this.user = info.user;
            userFullRequest = uerHolder.getUserFull(client, info.user.id)
                    .take(1);
        }
        forwardMessages = path.forwardMessages;
        rxChat.fetchSharedMediaPreview();
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        //        nm.onLoad(path.chat.id);

        AppUtils.logEvent("EnterCode.onLoad");

        if (!isGroupChat && ((TdApi.PrivateChatInfo) chat.type).user.type instanceof TdApi.UserTypeBot) {
            getView().setBot(true);
        }
        if (path.firstLoad) {
            path.firstLoad = false;
            rxChat.clear();
            if (chat.unreadCount == 0) {
                rxChat.initialRequest(chat);
            } else {
                rxChat.requestUntilLastUnread(chat);
            }
        }
        if (forwardMessages != null) {
            rxChat.forwardMessages(forwardMessages);
            forwardMessages = null;
        }

        shareContact();

        ChatView view = getView();

        view.loadToolBarImage(this.chat);
        initMenu(chat, nm.isMuted(this.chat));

        setViewSubtitle();

        getView().initList(rxChat);
        subscribe();
        if (isGroupChat) {
            TdApi.GroupChatInfo g = (TdApi.GroupChatInfo) this.chat.type;
            showMessagePanel(g.groupChat);
        }

        final ChatDB.UpdateReplyMarkupWithData currentMarkup = getRxChat().getCurrentMarkup();

        if (currentMarkup != null) {
            showBotKeyboardForMessage(currentMarkup);
        }
        //        if (currentMarkup instanceof TdApi.ReplyMarkupShowKeyboard){
        //
        //        }
    }

    private void shareContact() {
        if (path.sharedContact != null) {
            rxChat.sendMessage(path.sharedContact);
            path.sharedContact = null;
        }
    }

    private void setViewSubtitle() {
        TdApi.ChatInfo t = chat.type;
        if (t instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) t).user;
            setViewTitle(user);
        } else {
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) t).groupChat;
            getView().setGroupChatTitle(groupChat, chat);
        }
    }

    private void setViewTitle(TdApi.User user) {
        getView().setPrivateChatTitle(user);
        final TdApi.UserStatus userStatus = rxChat.holder.getUserStatus(user);

        if (user.type instanceof TdApi.UserTypeBot) {
            getView().setPrivateChatSubtitle(getView().getContext().getString(R.string.user_status_bot));
        } else {
            getView().setPrivateChatSubtitle(
                    AppUtils.uiUserStatus(getView().getContext(), userStatus));
        }
    }

    @Override
    public void dropView(ChatView view) {
        super.dropView(view);
        //        nm.dropView(path.chat.id);
        subscription.unsubscribe();
        openChatSubscription.unsubscribe();
        //        Utils.hideKeyboard(view);
    }

    private void subscribe() {
        if (subscription != null) {
            assertTrue(subscription.isUnsubscribed());
        }
        subscription = new CompositeSubscription();

        subscription.add(
                rxChat.history()
                        .subscribe(new ObserverAdapter<RxChat.HistoryResponse>() {
                            @Override
                            public void onNext(RxChat.HistoryResponse history) {
                                //todo if unread messages
                                getView().addHistory(chat, history);
                            }
                        })
        );
        //        subscription.add(
        //                rxChat.messageList()
        //                        .subscribe(new ObserverAdapter<List<TdApi.Message>>() {
        //                            @Override
        //                            public void onNext(List<TdApi.Message> messages) {
        //                                getView()
        //                                        .setMessages(messages);
        //
        //                            }
        //                        }));

        subscription.add(
                rxChat.getDeletedMessagesSubject()
                        .subscribe(new ObserverAdapter<RxChat.DeletedMessages>() {
                            @Override
                            public void onNext(RxChat.DeletedMessages response) {
                                getView().deleteMessages(response);
                            }
                        })
        );
        subscription.add(
                rxChat.getNewMessage()
                        .subscribe(new ObserverAdapter<List<TdApi.Message>>() {
                                       @Override
                                       public void onNext(List<TdApi.Message> ms) {
                                           getView()
                                                   .addNewMessages(ms);
                                           //                                           showBotKeyboard(ms);
                                           rxChat.hackToReadTheMessage(ms);
                                       }
                                   }
                        ));

        requestUpdateOnlineStatus();

        subscription.add(
                nm.updatesForChat(chat)
                        .subscribe(new ObserverAdapter<TdApi.NotificationSettings>() {
                                       @Override
                                       public void onNext(TdApi.NotificationSettings s) {
                                           initMenu(chat, nm.isMuted(s));
                                       }
                                   }
                        ));

        subscription.add(
                updateReadOutbox()
                        .subscribe(new ObserverAdapter<TdApi.UpdateChatReadOutbox>() {
                            @Override
                            public void onNext(TdApi.UpdateChatReadOutbox response) {
                                getView()
                                        .getAdapter()
                                        .setLastReadOutbox(response.lastReadOutboxMessageId);
                            }
                        }));

        subscription.add(
                rxChat.holder.getMessageIdsUpdates(chat.id)
                        .subscribe(new ObserverAdapter<TdApi.UpdateMessageId>() {
                            @Override
                            public void onNext(TdApi.UpdateMessageId response) {
                                getView()
                                        .getAdapter()
                                        .notifyDataSetChanged();
                            }
                        })
        );

        subscription.add(usersStatus()
                .subscribe(new ObserverAdapter<TdApi.UpdateUserStatus>() {
                    @Override
                    public void onNext(TdApi.UpdateUserStatus response) {
                        requestUpdateOnlineStatus();
                    }
                }));

        subscription.add(
                updatesChatsParticipantCount()
                        .subscribe(new ObserverAdapter<TdApi.UpdateChatParticipantsCount>() {
                            @Override
                            public void onNext(TdApi.UpdateChatParticipantsCount response) {
                                requestUpdateOnlineStatus();
                            }
                        }));

        subscription.add(
                rxChat.getMessageChanged()
                        .subscribe(new ObserverAdapter<TdApi.Message>() {
                            @Override
                            public void onNext(TdApi.Message response) {
                                getView().messageChanged(response);
                            }
                        })
        );

        subscription.add(
                owner.activityResult()
                        .subscribe(new ObserverAdapter<ActivityResult>() {
                            @Override
                            public void onNext(ActivityResult response) {
                                onActivityResult(response.request, response.result, response.data);
                            }
                        }));
        subscription.add(
                userFullRequest.subscribe(new ObserverAdapter<TdApi.UserFull>() {
                    @Override
                    public void onNext(TdApi.UserFull response) {
                        bindView(response);
                    }
                }));
        subscription.add(
                rxChat.getMarkup().subscribe(new ObserverAdapter<ChatDB.UpdateReplyMarkupWithData>() {
                    @Override
                    public void onNext(ChatDB.UpdateReplyMarkupWithData response) {
                        showBotKeyboardForMessage(response);
                    }
                }));

        if (isGroupChat) {
            subscription.add(
                    client.updateChatPhoto(chat.id)
                            .subscribe(new ObserverAdapter<TdApi.UpdateChatPhoto>() {
                                @Override
                                public void onNext(TdApi.UpdateChatPhoto response) {
                                    final TdApi.GroupChatInfo type = (TdApi.GroupChatInfo) chat.type;
                                    type.groupChat.photo = response.photo;
                                    getView()
                                            .loadAvatarFor(chat);
                                }
                            }));
            subscription.add(
                    client.updateChatTitle(chat.id)
                            .subscribe(new ObserverAdapter<TdApi.UpdateChatTitle>() {
                                @Override
                                public void onNext(TdApi.UpdateChatTitle response) {
                                    final TdApi.GroupChatInfo type = (TdApi.GroupChatInfo) chat.type;
                                    type.groupChat.title = response.title;
                                    getView().setGroupChatTitle(type.groupChat, chat);
                                }
                            }));
        } else {
            assertNotNull(user);
            subscription.add(
                    client.userUpdates(user.id)
                            .subscribe(new ObserverAdapter<TdApi.UpdateUser>() {
                                @Override
                                public void onNext(TdApi.UpdateUser response) {
                                    getView()
                                            .loadAvatarFor(response.user);
                                    setViewTitle(response.user);
                                }
                            }));
        }
    }

    private void initMenu(TdApi.Chat chat, boolean muted) {
        getView().initMenu(chat, muted);
        this.muted = muted;
    }

    private void bindView(final TdApi.UserFull response) {
        AppUtils.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                final ChatView v = getView();
                if (v == null){
                    return;
                }
                if (response.botInfo instanceof TdApi.BotInfoGeneral) {
                    final TdApi.BotInfoGeneral i = (TdApi.BotInfoGeneral) response.botInfo;
                    List<BotCommandsAdapter.Record> cs = new ArrayList<>();
                    for (TdApi.BotCommand command : i.commands) {
                        cs.add(new BotCommandsAdapter.Record(response.user, command));
                    }

                    v.setCommands(cs);
                    v.addBotInfoHeader(i, response.user);
                }
            }
        });

    }

    private void showBotKeyboardForMessage(@NonNull ChatDB.UpdateReplyMarkupWithData markup) {
        if (markup.msg == null) {
            getView().hideReplyKeyboard();
            return;
        }
        //todo
        //        if (replyMarkup instanceof TdApi.ReplyMarkupForceReply) {
        //            return;//unsupported yet
        //        }
        if (markup.msg.replyMarkup instanceof TdApi.ReplyMarkupShowKeyboard) {
            getView().showBotKeyboard(markup.msg);
        }
    }

    private Observable<TdApi.UpdateChatParticipantsCount> updatesChatsParticipantCount() {
        return client.chatParticipantCount().filter(new Func1<TdApi.UpdateChatParticipantsCount, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateChatParticipantsCount upd) {
                return upd.chatId == chat.id;
            }
        }).observeOn(mainThread());
    }

    private Observable<TdApi.UpdateUserStatus> usersStatus() {
        return client.usersStatus()

                .filter(new Func1<TdApi.UpdateUserStatus, Boolean>() {
                    @Override
                    public Boolean call(TdApi.UpdateUserStatus updateUserStatus) {
                        if (isGroupChat) {
                            TdApi.GroupChatFull mGroupChatFullCopy = Presenter.this.mGroupChatFull;
                            if (mGroupChatFullCopy == null) {
                                return true;
                            }
                            for (TdApi.ChatParticipant p : mGroupChatFullCopy.participants) {
                                if (p.user.id == updateUserStatus.userId) {
                                    return true;
                                }
                            }
                            return false;
                        } else {
                            return getChatUserId() == updateUserStatus.userId;
                        }
                    }
                }).observeOn(mainThread());
    }

    private int getChatUserId() {
        if (isGroupChat) {
            throw new IllegalStateException();
        }
        TdApi.PrivateChatInfo type = (TdApi.PrivateChatInfo) chat.type;
        return type.user.id;
    }

    private void requestUpdateOnlineStatus() {
        checkMainThread();
        if (isGroupChat) {
            subscription.add(
                    fullChatInfoRequest.subscribe(
                            new ObserverAdapter<TdApi.GroupChatFull>() {
                                @Override
                                public void onNext(@NonNull TdApi.GroupChatFull groupChatFull) {
                                    bindGroupChatFull(groupChatFull);
                                }

                                @Override
                                public void onError(Throwable th) {
                                    //todo
                                    super.onError(th);
                                }
                            }
                    ));
        } else {
            subscription.add(
                    getUser().subscribe(new ObserverAdapter<TdApi.User>() {
                        @Override
                        public void onNext(TdApi.User user) {
                            setViewTitle(user);
                        }
                    }));
        }
    }

    private void bindGroupChatFull(@NonNull final TdApi.GroupChatFull groupChatFull) {
        AppUtils.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (getView() == null) {
                    return;
                }
                boolean firstResponse = mGroupChatFull == null;
                mGroupChatFull = groupChatFull;
                updateGroupChatOnlineStatus(groupChatFull);
                if (firstResponse) {
                    showMessagePanel(groupChatFull.groupChat);
                    setBotCommands(groupChatFull);
                }
            }
        });
    }

    private void setBotCommands(TdApi.GroupChatFull groupChatFull) {
        botCount = 0;
        List<BotCommandsAdapter.Record> cs = new ArrayList<>();
        for (TdApi.ChatParticipant p : groupChatFull.participants) {
            if (p.botInfo instanceof TdApi.BotInfoGeneral) {
                botCount++;
                final TdApi.BotCommand[] commands = ((TdApi.BotInfoGeneral) p.botInfo).commands;
                for (TdApi.BotCommand cmd : commands) {
                    cs.add(new BotCommandsAdapter.Record(p.user, cmd));
                }
            }
        }
        getView().setCommands(cs);
    }

    private void showMessagePanel(TdApi.GroupChat groupChat) {
        //        if (g.groupChat.left){
        getView().showMessagePanel(groupChat.left);
        //        }
    }

    private Observable<TdApi.User> getUser() {
        return client.getUser(getChatUserId()).observeOn(mainThread());
    }

    private Observable<TdApi.UpdateChatReadOutbox> updateReadOutbox() {
        return client.updateChatReadOutbox().filter(new Func1<TdApi.UpdateChatReadOutbox, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateChatReadOutbox updateChatReadOutbox) {
                return updateChatReadOutbox.chatId == chat.id;
            }
        }).observeOn(mainThread());
    }

    private void updateGroupChatOnlineStatus(TdApi.GroupChatFull info) {
        int online = 0;
        for (TdApi.ChatParticipant p : info.participants) {
            if (p.user.status instanceof TdApi.UserStatusOnline) {
                online++;
            }
        }
        getView().setwGroupChatSubtitle(info.participants.length, online);
    }

    public void listScrolledToEnd() {
        if (rxChat.isDownloadedAll()) {
            return;
        }
        if (rxChat.isRequestInProgress()) {
            return;
        }
        rxChat.requestNewPotion();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (R.id.menu_leave_group == id) {
            leaveGroup();
            return true;
        } else if (R.id.menu_clear_history == id) {
            clearHistory();
            return true;
        } else if (R.id.menu_mute_unmute == id) {
            getView().showMutePopup();
        }
        return false;
    }

    private void clearHistory() {
        rxChat.deleteHistory();
    }

    private void leaveGroup() {
        //        rxChat.
        //todo mb progress?!
        //todo config changes
        subscription.add(
                client.sendCachedRXUI(
                        new TdApi.DeleteChatParticipant(chat.id, path.me.id)
                ).subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        Flow.get(getView().getContext())
                                .goBack();
                    }
                }));
        ;
    }

    @Override
    public void sendText(final String text) {
        getView().scrollToBottom();
        getView().postDelayed(new Runnable() {
            @Override
            public void run() {
                rxChat.sendMessage(text);
            }
        }, 16);
    }

    public void sendSticker(final TdApi.Sticker sticker) {
        //        stickers.map(stickerFilePath, sticker);
        getView().scrollToBottom();
        getView().postDelayed(new Runnable() {
            @Override
            public void run() {
                rxChat.sendSticker(sticker.sticker);
            }
        }, 32);
    }

    @Override
    public void sendImages(List<String> selecteImages) {
        for (String img : selecteImages) {
            rxChat.sendImage(img);
        }
        getView()
                .hideAttachPannel();
    }

    @Override
    public void chooseFromGallery() {
        String title = getView().getContext().getString(R.string.select_picture);
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        owner.expose()
                .startActivityForResult(Intent.createChooser(intent, title), AppUtils.REQUEST_CHOOS_FROM_GALLERY);
    }

    @Override
    public void takePhoto() {
        File f = AppUtils.getTmpFileForCamera();
        f.delete();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        owner.expose()
                .startActivityForResult(intent, AppUtils.REQUEST_TAKE_PHOTO);
    }

    public void onActivityResult(int request, int result, Intent data) {
        if (result != Activity.RESULT_OK) {
            return;
        }
        if (request == AppUtils.REQUEST_TAKE_PHOTO) {
            File f = AppUtils.getTmpFileForCamera();
            if (f.exists()) {
                rxChat.sendImage(f.getAbsolutePath());
                getView()
                        .hideAttachPannel();
            }
        } else if (request == AppUtils.REQUEST_CHOOS_FROM_GALLERY) {
            String picturePath = Utils.getGalleryPickedFilePath(getView().getContext(), data);
            if (picturePath != null) {
                rxChat.sendImage(picturePath);
                getView()
                        .hideAttachPannel();
            }
        }
    }

    public RxChat getRxChat() {
        return rxChat;
    }

    public void open(TdApi.User user) {
        Flow.get(getView())
                .set(new ProfilePath(chat, path.me, user));
        //        subscription.add(
        //                userFullRequest.subscribe(new ObserverAdapter<TdApi.UserFull>() {
        //                    @Override
        //                    public void onNext(TdApi.UserFull response) {
        //
        //                    }
        //                }));
    }

    public void open(final TdApi.Chat groupChat) {
        Flow.get(getView())
                .set(new ChatInfo(groupChat));
    }

    public void muteFor(int duration) {
        nm.muteChat(chat, duration);
        initMenu(chat, nm.isMuted(this.chat));
    }

    public void sendBotCommand(TdApi.User bot, TdApi.BotCommand cmd) {
        if (isGroupChat) {
            sendText("/" + cmd.command + "@" + bot.username);
        } else {
            sendText("/" + cmd.command);
        }
    }

    public void textSpanCLicked(EmojiParser.ReferenceSpan cmd) {
        final String ref = cmd.reference;
        if (cmd.type == EmojiParser.TYPE_BOT_COMMAND) {
            if (isGroupChat) {
                if (path.me.id == cmd.userId || ref.contains("@")) {
                    sendText(ref.replaceAll("\n", ""));//todo fix regexp!!
                } else {
                    final TdApi.User user = uerHolder.getUser(cmd.userId);
                    if (user == null) {
                        return;
                    }
                    final String text = ref + "@" + user.username;
                    sendText(text.replaceAll("\n", ""));
                }
            } else {
                sendText(ref.replaceAll("\n", ""));
            }
        } else if (cmd.type == EmojiParser.TYPE_URL) {
            String url = ref;
            openBrowser(url);
        } else {
            //todo open dialog with user
        }
    }

    private void openBrowser(String url) {
        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            activity.expose().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://" + url));
                activity.expose().startActivity(intent);
            } catch (ActivityNotFoundException e1) {
                Toast.makeText(getView().getContext(),
                        "Faild to open the link. There is no browser on the phone. ;( ", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    public void sendBotKeyboardCommand(String cmd, TdApi.Message msg) {
        rxChat.sendBotCommand(cmd, msg);
    }

    public void sendVoice(Observable<VoiceRecorder.Record> stop) {
        rxChat.sendVoice(stop);
    }

    public void openChatWithAuthorOf(TdApi.Message msg) {
        final TdApi.User user = uerHolder.getUser(msg.fromId);
        if (user == null) {
            return;
        }
        openChatSubscription.unsubscribe();
        openChatSubscription = client.sendRx(new TdApi.CreatePrivateChat(msg.fromId))
                        .observeOn(mainThread())
                        .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                            @Override
                            public void onNext(TdApi.TLObject response) {
                                final Chat newHead = new Chat((TdApi.Chat) response, user, /* messages to forward */ null);
                                AppUtils.flowPushAndRemove(getView(), newHead, new LeaveOnlyChatList(), Flow.Direction.FORWARD);
                            }
                        });
    }

    public void muteUnmuteClicked() {
        if (muted){
            muteFor(NotificationManager.NOTIFICATIONS_ENABLED);
        } else {
            muteFor(NotificationManager.NOTIFICATIONS_DISABLED_FOREVER);
        }

    }
}
