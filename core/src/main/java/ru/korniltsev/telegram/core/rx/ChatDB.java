package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import junit.framework.Assert;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.operators.ImmediateBufferOperator;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class ChatDB {

    public static final long IMMEDIATE_BUFFER_INTERVAl = 128l + 64l;
    private final int chatLimit;
    private final int messageLimit;
    //guarded by ui thread
    final List<TdApi.Chat> chatsList = new ArrayList<>();
    //guarded by ui thread
    final PublishSubject<List<TdApi.Chat>> currentChatList = PublishSubject.create();

    private final Context ctx;
    final RXClient client;
    final NotificationManager nm;
    final EmojiParser parser;
    private Observable<ChatPortion> chatsRequest;
    private boolean downloadedAllChats;
    private boolean atLeastOneResponseReturned;
    private Observable<TdApi.UpdateMessageId> messageIdsUpdates;

    public Observable<TdApi.UpdateMessageId> getMessageIdsUpdates(final long id) {
        return messageIdsUpdates.filter(new Func1<TdApi.UpdateMessageId, Boolean>() {
            @Override
            public Boolean call(TdApi.UpdateMessageId updateMessageId) {
                return updateMessageId.chatId == id;
            }
        });
    }

    final UserHolder userHolder;

    @Inject
    public ChatDB(final Context ctx, final RXClient client, NotificationManager nm, RXAuthState auth, UserHolder userHolder) {
        this.ctx = ctx;
        this.client = client;
        final MyApp from = MyApp.from(ctx);
        parser = from.emojiParser;
        DpCalculator calc = from.calc;
//        this.parser = parser;
        this.nm = nm;
        this.userHolder = userHolder;
        prepareForUpdates();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay()
                .getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;
        int maxSize = Math.max(width, height);

        int aproxRowHeight = calc.dp(72);
        int limit = (int) (1.5 * maxSize / aproxRowHeight);
        chatLimit = Math.max(15, limit);

        int aproxMessageHeight = calc.dp(41);
        limit = (int) (1.5 * maxSize / aproxMessageHeight);
        messageLimit = Math.max(limit, 20);
        auth.listen()
                .subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
                    @Override
                    public void onNext(RXAuthState.AuthState authState) {
                        if (authState instanceof RXAuthState.StateLogout) {
                            chatIdToRxChat.clear();
                            chatsList.clear();
                            downloadedAllChats = false;
                            atLeastOneResponseReturned = false;
                        }
                    }
                });


        client.updatesReplyMarkup()
                .flatMap(new Func1<TdApi.UpdateChatReplyMarkup, Observable<UpdateReplyMarkupWithData>>() {
                    @Override
                    public Observable<UpdateReplyMarkupWithData> call(TdApi.UpdateChatReplyMarkup u) {
                        if (u.replyMarkupMessageId == 0) {//hide
//                            final TdApi.ReplyMarkupHideKeyboard markup = new TdApi.ReplyMarkupHideKeyboard();
                            return Observable.just(
                                    new UpdateReplyMarkupWithData(u.chatId, null));
                        }
                        return client.sendRx(new TdApi.GetMessage(u.chatId, u.replyMarkupMessageId))
                                .map(new Func1<TdApi.TLObject, UpdateReplyMarkupWithData>() {
                                    @Override
                                    public UpdateReplyMarkupWithData call(TdApi.TLObject tlObject) {
                                        final TdApi.Message msg = (TdApi.Message) tlObject;
//                                        final TdApi.ReplyMarkup replyMarkup = msg.replyMarkup;
                                        return new UpdateReplyMarkupWithData(msg.chatId, msg);
                                    }
                                });
                    }
                }).observeOn(mainThread())
                .subscribe(new ObserverAdapter<UpdateReplyMarkupWithData>() {
                    @Override
                    public void onNext(UpdateReplyMarkupWithData response) {
                        getRxChat(response.chatId)
                                .handleReplyMarkup(response);
                    }
                });
    }

    public static final class UpdateReplyMarkupWithData {
        public final long chatId;
//        final TdApi.ReplyMarkup markup;
        public final TdApi.Message msg;
        public UpdateReplyMarkupWithData(long chatId, TdApi.Message msg) {
            this.chatId = chatId;
            this.msg = msg;
        }
    }

    private void prepareForUpdates() {
        prepareForUpdateNewMessage();
        prepareForUpdateDeleteMessages();
        prepareForUpdateMessageId();
        prepareForUpdateMessageDate();
        //todo this 3 ones are probably needed
        //        prepareForUpdateChatReadInbox();
        prepareForUpdateChatReadOutbox();
        prepareForUpdateUserStatus();
        prepareForUpdateMessageContent();

        //        prepareForUpdateChatTitle();
        //        prepareForUpdateChatParticipantsCount();
    }

    Map<Integer, TdApi.UserStatus> userIdToUserStatus = new HashMap<>();

    private void prepareForUpdateUserStatus() {
        client.usersStatus()
                .subscribe(new ObserverAdapter<TdApi.UpdateUserStatus>() {
                    @Override
                    public void onNext(TdApi.UpdateUserStatus response) {
                        userIdToUserStatus.put(response.userId, response.status);
                    }
                });
    }

    public TdApi.UserStatus getUserStatus(TdApi.User u) {
        TdApi.UserStatus updatedStatus = userIdToUserStatus.get(u.id);
        if (updatedStatus == null) {
            return u.status;
        } else {
            return updatedStatus;
        }
    }

    private void prepareForUpdateMessageContent() {
        client.updateMessageContent()
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateMessageContent>() {
                    @Override
                    public void onNext(TdApi.UpdateMessageContent updateMessageContent) {
                        getRxChat(updateMessageContent.chatId)
                                .updateContent(updateMessageContent);

                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateChatReadOutbox() {
        client.updateChatReadOutbox()
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateChatReadOutbox>() {
                    @Override
                    public void onNext(TdApi.UpdateChatReadOutbox response) {
                        //                        updateChatMessageList(updateChatReadInbox.chatId);
                        updateCurrentChatList();
                    }
                });
    }

    //    private void prepareForUpdateChatReadInbox() {
    //        client.updateChatReadInbox()
    //                .observeOn(mainThread())
    //                .subscribe(new ObserverAdapter<TdApi.UpdateChatReadInbox>() {
    //                    @Override
    //                    public void onNext(TdApi.UpdateChatReadInbox updateChatReadInbox) {
    //                        updateChatMessageList(updateChatReadInbox.chatId);
    //                        updateCurrentChatList();
    //                    }
    //                });
    //    }

    private void prepareForUpdateMessageDate() {
        client.updateMessageDate()
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateMessageDate>() {
                    @Override
                    public void onNext(TdApi.UpdateMessageDate updateMessageDate) {
                        getRxChat(updateMessageDate.chatId)
                                .updateMessageDate(updateMessageDate);
                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateMessageId() {
        messageIdsUpdates = client.updateMessageId()
                .observeOn(mainThread())
                .map(new Func1<TdApi.UpdateMessageId, TdApi.UpdateMessageId>() {
                    @Override
                    public TdApi.UpdateMessageId call(TdApi.UpdateMessageId updateMessageId) {
                        getRxChat(updateMessageId.chatId)
                                .updateMessageId(updateMessageId);
                        updateCurrentChatList();
                        return updateMessageId;
                    }
                });
        messageIdsUpdates.subscribe(new ObserverAdapter<TdApi.UpdateMessageId>());
    }

    private void prepareForUpdateDeleteMessages() {
        client.updateDeleteMessages()
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.UpdateDeleteMessages>() {
                    @Override
                    public void onNext(TdApi.UpdateDeleteMessages messages) {
                        getRxChat(messages.chatId)
                                .deleteMessageImpl(messages.messages);
                        updateCurrentChatList();
                    }
                });
    }

    private void prepareForUpdateNewMessage() {
        client.updateNewMessages()
                .lift(new ImmediateBufferOperator<TdApi.UpdateNewMessage>(Schedulers.computation(), IMMEDIATE_BUFFER_INTERVAl))
                .map(new Func1<List<TdApi.UpdateNewMessage>, List<TdApi.UpdateNewMessage>>() {
                    @Override
                    public List<TdApi.UpdateNewMessage> call(List<TdApi.UpdateNewMessage> updateNewMessages) {
                        //                        Log.d("ImmediateBufferOperator", "handle  " + updateNewMessages.size());
                        for (TdApi.UpdateNewMessage msg : updateNewMessages) {
                            parser.parse(msg.message);
                        }
                        return updateNewMessages;
                    }
                }).observeOn(mainThread())
                .subscribe(new ObserverAdapter<List<TdApi.UpdateNewMessage>>() {
                    @Override
                    public void onNext(List<TdApi.UpdateNewMessage> response) {
                        tmpChatIds.clear();
                        for (TdApi.UpdateNewMessage u : response) {
                            tmpChatIds.add(u.message.chatId);
                        }
                        for (Long chatId : tmpChatIds) {
                            getRxChat(chatId)
                                    .handleNewMessageList(response);
                        }
                        nm.notifyOnce(response);
                        updateCurrentChatList();
                    }
                });

        //        client.updateNewMessages()
        //                .map(new Func1<TdApi.UpdateNewMessage, TdApi.UpdateNewMessage>() {
        //                    @Override
        //                    public TdApi.UpdateNewMessage call(TdApi.UpdateNewMessage updateNewMessage) {
        //                        parser.parse(updateNewMessage.message);
        //                        return updateNewMessage;
        //                    }
        //                })
        //                .observeOn(mainThread())
        //                .subscribe(new ObserverAdapter<TdApi.UpdateNewMessage>() {
        //                    @Override
        //                    public void onNext(TdApi.UpdateNewMessage updateNewMessage) {
        //                        getRxChat(updateNewMessage.message.chatId)
        //                                .handleNewMessage(updateNewMessage.message);
        //                        nm.notifyNewMessage(updateNewMessage.message);
        //                        updateCurrentChatList();
        //                    }
        //                });
    }

    final Set<Long> tmpChatIds = new HashSet<>();

    //    private void updateChatMessageList(long id){
    //        getRxChat(id)
    //                .updateCurrentMessageList();
    //    }

    public void updateCurrentChatList() {
        checkMainThread();
        if (chatsRequest != null) {
            //todo
        } else {
            requestImpl(0, chatsList.size() + 1, false);
        }
    }

//    public void saveUsers(SparseArray<TdApi.User> us) {
//        for (int i = 0; i < us.size(); i++) {
//            TdApi.User obj = us.get(
//                    us.keyAt(i));
//            saveUser(obj);
//        }
//    }

    class ChatPortion {
        final TdApi.Chats cs;

        public ChatPortion(TdApi.Chats cs) {
            this.cs = cs;
        }
    }

    //request new portion
    public void requestPortion() {
        requestImpl(chatsList.size(), chatLimit, true);
    }

//    final Set<Integer> tmpIds = new HashSet<>();

    private void requestImpl(int offset, int limit, final boolean historyRequest) {
        Assert.assertNull(chatsRequest);
        chatsRequest = client.getChats(offset, limit)
                .flatMap(new Func1<TdApi.Chats, Observable<ChatPortion>>() {
                    @Override
                    public Observable<ChatPortion> call(TdApi.Chats chats) {
                        for (TdApi.Chat chat : chats.chats) {
                            parser.parse(chat.topMessage);
                        }
                        return Observable.just(new ChatPortion(chats));
                    }
                })
                .observeOn(mainThread());

        chatsRequest.subscribe(new ObserverAdapter<ChatPortion>() {
            @Override
            public void onNext(ChatPortion p) {
                atLeastOneResponseReturned = true;
                chatsRequest = null;
                if (p.cs.chats.length == 0) {
                    downloadedAllChats = true;
                }
                List<TdApi.Chat> csList = Arrays.asList(p.cs.chats);
                if (!historyRequest) {
                    chatsList.clear();
                }
                chatsList.addAll(csList);
                nm.updateNotificationScopes(chatsList);
                currentChatList.onNext(chatsList);
            }
        });
    }

    public Observable<List<TdApi.Chat>> chatList() {
        checkMainThread();
        return currentChatList;
    }

    public List<TdApi.Chat> getAllChats() {
        checkMainThread();
        return chatsList;
    }

    public boolean isRequestInProgress() {
        checkMainThread();
        return chatsRequest != null;
    }

    public RxChat getRxChat(long id) {
        checkMainThread();
        RxChat rxChat = chatIdToRxChat.get(id);
        if (rxChat == null) {
            rxChat = new RxChat(id, client, this);
            chatIdToRxChat.put(id, rxChat);
            return rxChat;
        }
        return rxChat;
    }

    //guarded by ui thread
    private final LongSparseArray<RxChat> chatIdToRxChat = new LongSparseArray<>();

    public boolean isDownloadedAllChats() {
        checkMainThread();
        return downloadedAllChats;
    }

    public boolean isAtLeastOneResponseReturned() {
        checkMainThread();
        return atLeastOneResponseReturned;
    }

    public int getMessageLimit() {
        return messageLimit;
    }
}
