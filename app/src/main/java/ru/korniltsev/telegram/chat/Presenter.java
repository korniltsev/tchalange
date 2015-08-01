package ru.korniltsev.telegram.chat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import flow.Flow;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.AttachPanelPopup;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.chat.bot.BotCommandsAdapter;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.emoji.Stickers;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.mortar.ActivityResult;
import ru.korniltsev.telegram.core.rx.EmojiParser;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.profile.chat.ChatInfo;
import ru.korniltsev.telegram.profile.other.ProfilePath;
import rx.Observable;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class Presenter extends ViewPresenter<ChatView>
        implements Toolbar.OnMenuItemClickListener,
        MessagePanel.OnSendListener,
        AttachPanelPopup.Callback

{

    public static final int REQUEST_CHOOS_FROM_GALLERY = 1;
    public static final int REQUEST_TAKE_PHOTO = 2;
    private final Chat path;
    private final RXClient client;
    private final RxChat rxChat;
    private final NotificationManager nm;

    private final Observable<TdApi.GroupChatFull> fullChatInfoRequest;
    private final Observable<TdApi.UserFull> userFullRequest;
    private final boolean isGroupChat;
    private final TdApi.Chat chat;
    private CompositeSubscription subscription;
    @Nullable private volatile TdApi.GroupChatFull mGroupChatFull;
    private int botCount;

    public Chat getPath() {
        return path;
    }

    private final ActivityOwner owner;
    private final Stickers stickers;

    @Inject
    public Presenter(Chat c, RXClient client, ChatDB chatDB, NotificationManager nm, ActivityOwner owner, Stickers stickers) {
        path = c;
        this.client = client;
        this.nm = nm;
        this.owner = owner;
        this.stickers = stickers;
        this.chat = path.chat;
        rxChat = chatDB.getRxChat(chat.id);

        if (chat.type instanceof TdApi.GroupChatInfo) {
            TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) chat.type).groupChat;
            fullChatInfoRequest = client.getGroupChatInfo(groupChat.id)
                    .observeOn(mainThread());
            isGroupChat = true;
            userFullRequest = Observable.empty();
        } else {
            fullChatInfoRequest = Observable.empty();
            isGroupChat = false;
            final TdApi.PrivateChatInfo info = (TdApi.PrivateChatInfo) chat.type;
            userFullRequest = client.getUserFull(info.user.id)
                    .observeOn(mainThread());
//            if (info.user.type instanceof TdApi.UserTypeBot) {
//                getView().setBot(assertTrue());
//            }
        }
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
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

        shareContact();

        ChatView view = getView();

        view.loadToolBarImage(this.chat);
        view.initMenu(chat, nm.isMuted(this.chat));
        setViewSubtitle();

        getView().initList(rxChat);
        subscribe();
        if (isGroupChat) {
            TdApi.GroupChatInfo g = (TdApi.GroupChatInfo) this.chat.type;
            showMessagePanel(g.groupChat);
        }
        //todo manage subscription!!!!!!!!!!
        //todo manage subscription!!!!!!!!!!
        //todo manage subscription!!!!!!!!!!
        //todo manage subscription!!!!!!!!!!
        //todo manage subscription!!!!!!!!!!
        //todo manage subscription!!!!!!!!!!
        userFullRequest.subscribe(new ObserverAdapter<TdApi.UserFull>() {
            @Override
            public void onNext(TdApi.UserFull response) {
                System.out.println();
                if (response.botInfo instanceof TdApi.BotInfoGeneral) {
                    final TdApi.BotInfoGeneral i = (TdApi.BotInfoGeneral) response.botInfo;
                    List<BotCommandsAdapter.Record> cs = new ArrayList<>();
                    for (TdApi.BotCommand command : i.commands) {
                        cs.add(new BotCommandsAdapter.Record(response.user, command));
                    }
                    getView().setCommands(cs);
                    getView().addBotInfoHeader(i, response.user);
                }
            }
        });
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
        subscription.unsubscribe();
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
                                           showBotKeyboard(ms);
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
                                           getView().initMenu(chat, nm.isMuted(s));
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
    }

    private void showBotKeyboard(List<TdApi.Message> ms) {
        for (int i = ms.size() - 1; i >= 0; i--) {
            final TdApi.Message msg = ms.get(i);
            if (showKeyboardForMessage(msg)) {
                break;
            }
        }
    }

    private boolean showKeyboardForMessage(TdApi.Message msg) {
        //todo should save last shown, but not here - in RxChat
        if (msg.fromId == path.me.id) {
            return false;
        }
        final TdApi.ReplyMarkup replyMarkup = msg.replyMarkup;
        if (replyMarkup instanceof TdApi.ReplyMarkupForceReply) {
            return true;
        } else if (replyMarkup instanceof TdApi.ReplyMarkupHideKeyboard) {
            getView().hideReplyKeyboard();
            return true;
        } else if (replyMarkup instanceof TdApi.ReplyMarkupShowKeyboard) {
            getView().showBotKeyboard(((TdApi.ReplyMarkupShowKeyboard) replyMarkup));
            return true;
        } else if (replyMarkup instanceof TdApi.ReplyMarkupNone) {
            return false;
        }
        return false;
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
                                public void onNext(TdApi.GroupChatFull groupChatFull) {
                                    mGroupChatFull = groupChatFull;
                                    assertNotNull(mGroupChatFull);

                                    showMessagePanel(mGroupChatFull.groupChat);
                                    updateGroupChatOnlineStatus(groupChatFull);
                                    setBotCommands();
                                }

                                @Override
                                public void onError(Throwable th) {
                                    //todo
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

    private void setBotCommands() {
        botCount = 0;
        List<BotCommandsAdapter.Record> cs = new ArrayList<>();
        for (TdApi.ChatParticipant p : mGroupChatFull.participants) {
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

    public void sendSticker(final String stickerFilePath, final TdApi.Sticker sticker) {
        stickers.map(stickerFilePath, sticker);
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
                .startActivityForResult(Intent.createChooser(intent, title), REQUEST_CHOOS_FROM_GALLERY);
    }

    @Override
    public void takePhoto() {
        File f = getTmpFileForCamera();
        f.delete();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        owner.expose()
                .startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }

    @NonNull
    private File getTmpFileForCamera() {
        return new File(Environment.getExternalStorageDirectory(), "temp.jpg");
    }

    public void onActivityResult(int request, int result, Intent data) {
        if (result != Activity.RESULT_OK) {
            return;
        }
        if (request == REQUEST_TAKE_PHOTO) {
            File f = getTmpFileForCamera();
            if (f.exists()) {
                rxChat.sendImage(f.getAbsolutePath());
                getView()
                        .hideAttachPannel();
            }
        } else if (request == REQUEST_CHOOS_FROM_GALLERY) {
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
                .set(new ProfilePath(user, chat, path.me));
    }

    public void open(final TdApi.Chat groupChat) {
        fullChatInfoRequest.subscribe(new ObserverAdapter<TdApi.GroupChatFull>() {
            @Override
            public void onNext(TdApi.GroupChatFull response) {
                Flow.get(getView())
                        .set(new ChatInfo(response, groupChat));
            }
        });
    }

    public void muteFor(int duration) {
        nm.muteChat(chat, duration);
        getView().initMenu(chat, nm.isMuted(this.chat));
    }

    public void sendBotCommand(TdApi.User bot, TdApi.BotCommand cmd) {
        if (isGroupChat){
            sendText("/" + cmd.command + "@" + bot.username);
        } else {
            sendText("/" + cmd.command);
        }
    }

    public void textSpanCLicked(EmojiParser.BotCommand cmd) {
        if (isGroupChat){
            if (path.me.id == cmd.userId){
                sendText(cmd.cmd);
            } else {
                final TdApi.User user = rxChat.getUser(cmd.userId);
                if (user == null){
                    return;
                }
                sendText(cmd.cmd + "@" + user.username);
            }

        } else {
            sendText(cmd.cmd);
        }
    }
}
