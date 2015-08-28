package ru.korniltsev.telegram.core.rx;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.crashlytics.android.core.CrashlyticsCore;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.addAll;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkNotMainThread;
import static rx.Observable.just;
import static rx.Observable.zip;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class RxChat  {
    public static final TdApi.SearchMessagesFilterPhotoAndVideo MEDIA_PREVIEW_FILTER = new TdApi.SearchMessagesFilterPhotoAndVideo();


    public static final int MESSAGE_STATE_READ = 0;
    public static final int MESSAGE_STATE_SENT = 1;
    public static final int MESSAGE_STATE_NOT_SENT = 2;
    private static final int MSG_WITHOUT_VALID_ID = 1000000000;

    //    public static int compareInt(int lhs, int rhs) {
    //        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    //    }

    //    private SortedSet<TdApi.Message> ms = new TreeSet<>(new Comparator<TdApi.Message>() {
    //        @Override
    //        public int compare(TdApi.Message lhs, TdApi.Message rhs) {
    //            int dateCompare = -compareInt(lhs.date, rhs.date);
    //            if (dateCompare == 0) {
    //                return -compareInt(lhs.id, rhs.id);
    //            }
    //            return dateCompare;
    //        }
    //    });

    final List<TdApi.Message> data = new ArrayList<>();
    public final DaySplitter daySplitter;
    //    todo


    private Func1<TdApi.TLObject, TdApi.Message> CAST_TO_MESSAGE_AND_PARSE_EMOJI = new Func1<TdApi.TLObject, TdApi.Message>() {
        @Override
        public TdApi.Message call(TdApi.TLObject tlObject) {
            TdApi.Message msg = (TdApi.Message) tlObject;
            holder.parser.parse(msg);
            return msg;
        }
    };
    private ObserverAdapter<TdApi.Message> HANDLE_NEW_MESSAGE = new ObserverAdapter<TdApi.Message>() {
        @Override
        public void onNext(final TdApi.Message tlObject) {
            simulateUpdateNewMessage(tlObject);
//            handleNewMessage(tlObject);
        }
    };

    private void simulateUpdateNewMessage(TdApi.Message tlObject) {
        client.simulateNewMessage(tlObject);
    }

    final long id;
    final RXClient client;
    public final ChatDB holder;

    //    private final PublishSubject<List<TdApi.Message>> subject = PublishSubject.create();
    private PublishSubject<List<TdApi.Message>> newMessage = PublishSubject.create();
    private PublishSubject<HistoryResponse> historySubject = PublishSubject.create();
    private PublishSubject<DeletedMessages> deletedMessagesSubject = PublishSubject.create();
    private PublishSubject<TdApi.Message> messageChanged = PublishSubject.create();

    private ThreadLocal<Set<Integer>> tmpUIDs = new ThreadLocal<Set<Integer>>() {
        @Override
        protected Set<Integer> initialValue() {
            return new HashSet<>();
        }
    };

    private boolean downloadedAll;
    private Subscription subscription = Subscriptions.empty();
    private Observable<Portion> request;

    public RxChat(long id, RXClient client, ChatDB holder) {
        this.id = id;
        this.client = client;
        this.holder = holder;
        daySplitter = new DaySplitter();
    }

    public Observable<List<TdApi.Message>> getNewMessage() {
        return newMessage;
    }

    public boolean isRequestInProgress() {
        return request != null;
    }


    public List<TdApi.Message> getMessages() {
        return data;
    }

    public boolean isDownloadedAll() {
        return downloadedAll;
    }





    public void handleNewMessage(final List<TdApi.Message> ms) {
        for (int i = 0, msSize = ms.size(); i < msSize; i++) {
            TdApi.Message m = ms.get(i);
            sentPhotoHack(m);
        }
        addNewMessageAndDispatch(ms);
    }

    //hack to prevent reloading of newly added image
    final SparseArray<TdApi.File> sentMessageIdToImageLink = new SparseArray<>();

    private void sentPhotoHack(TdApi.Message msg) {
        if (msg.message instanceof TdApi.MessagePhoto
                && msg.id >= MSG_WITHOUT_VALID_ID) {
            final TdApi.Photo p = ((TdApi.MessagePhoto) msg.message).photo;
            if (p.photos.length == 1) {
                final TdApi.PhotoSize photo = p.photos[0];
                if (photo.type.equals("i") && photo.photo.isLocal()) {
                    sentMessageIdToImageLink.put(msg.id, photo.photo);
                }
            }
        }
    }

    @Nullable public TdApi.File getSentImage(@Nullable TdApi.Message msg) {
        if (msg == null){
            return null;
        }
        return sentMessageIdToImageLink.get(msg.id);
    }

    private void addNewMessageAndDispatch(List<TdApi.Message> ms) {
        Collections.reverse(ms);
        data.addAll(0, ms);
        newMessage.onNext(ms);
    }

    public void deleteHistory() {
        client.sendRx(new TdApi.DeleteChatHistory(id))
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        data.clear();
                        deletedMessagesSubject.onNext(ALL_DELETED_MESSAGES);
                    }
                });
    }

    public static final DeletedMessages ALL_DELETED_MESSAGES = new DeletedMessages();

    final SparseArray<TdApi.UpdateMessageId> newIdToUpdate = new SparseArray<>();
    final SparseArray<TdApi.UpdateMessageId> oldIdToUpdate = new SparseArray<>();

    public void updateMessageId(TdApi.UpdateMessageId upd) {
        //never really changes id of the TdApi.Message
        newIdToUpdate.put(upd.newId, upd);
        oldIdToUpdate.put(upd.oldId, upd);
    }

    public TdApi.UpdateMessageId getUpdForNewId(int newId) {
        return newIdToUpdate.get(newId);
    }

    public TdApi.UpdateMessageId getUpdForOldId(int oldId) {
        return oldIdToUpdate.get(oldId);
    }

    public Observable<TdApi.TLObject> deleteMessage(final int messageId) {
        return client.sendCachedRXUI(new TdApi.DeleteMessages(id, new int[]{messageId}))
                .map(new Func1<TdApi.TLObject, TdApi.TLObject>() {
                    @Override
                    public TdApi.TLObject call(TdApi.TLObject tlObject) {
                        checkMainThread();
                        deleteMessageImpl(new int[]{messageId});
                        return tlObject;
                    }
                });
    }

    public void deleteMessageImpl(int[] ms) {
        //todo O(n*m)
        final ArrayList<TdApi.Message> deletedMessages = new ArrayList<>();
        for (TdApi.Message message : data) {
            for (int messageId : ms) {
                if (message.id == messageId) {
                    deletedMessages.add(message);
                    break;
                }
            }
        }
        data.removeAll(deletedMessages);
        deletedMessagesSubject.onNext(new DeletedMessages(deletedMessages));
    }

    public void sendSticker(TdApi.File f) {
        TdApi.InputMessageSticker content = new TdApi.InputMessageSticker(new TdApi.InputFileId(f.id));
        sendMessageImpl(content);
    }

    public void sendMessage(String text) {
        TdApi.InputMessageText content = new TdApi.InputMessageText(text);
        sendMessageImpl(content);
    }

    private void sendMessageImpl(TdApi.InputMessageContent content) {
        sendMessageImpl(content, null);
    }
    private void sendMessageImpl(TdApi.InputMessageContent content, TdApi.ReplyMarkup markup) {
        client.sendRx(new TdApi.SendMessage(id, 0, true, markup, content))
                .map(CAST_TO_MESSAGE_AND_PARSE_EMOJI)
                .observeOn(mainThread())
                .subscribe(HANDLE_NEW_MESSAGE);
    }

    public void hackToReadTheMessage(List<TdApi.Message> msg) {
        for (TdApi.Message message : msg) {
            client.sendRx(new TdApi.GetChatHistory(id, message.id, -1, 1))
                    .subscribe(new ObserverAdapter<TdApi.TLObject>());
        }

    }

    public void sendImage(String imageFilePath) {
        sendMessageImpl(new TdApi.InputMessagePhoto(new TdApi.InputFileLocal(imageFilePath), ""));
    }

    public void clear() {
        request = null;
        subscription.unsubscribe();
        data.clear();
        downloadedAll = false;
    }

    public void initialRequest(final TdApi.Chat chat) {
        assertNull(request);
        if (chat.topMessage.id > 0) {
            //            parameter from_id must be positive GetChatHistory {
            //                chatId = 98255217
            //                fromId = 0
            //                offset = 0
            //                limit = 23
            //            }
            request = client.getMessages(id, chat.topMessage.id, 0, holder.getMessageLimit())
                    .compose(new GetUsers2(chat.topMessage));
            request.subscribe(new HistoryObserver(false));
        }
    }

    public void requestNewPotion() {
        TdApi.Message lastMessage = getLastMessage();
        if (lastMessage == null) {
            return;
        }
        assertNull(request);
        request = client.getMessages(id, lastMessage.id, 0, holder.getMessageLimit())
                .compose(new GetUsers2(null));
        request.subscribe(new HistoryObserver(false));
    }

    @Nullable
    private TdApi.Message getLastMessage() {
        if (data.isEmpty()) {
            return null;
        } else {
            return data.get(data.size() - 1);
        }
    }

    public void requestUntilLastUnread(TdApi.Chat chat) {
        assertNull(request);
        final int until = chat.lastReadInboxMessageId;
        if (until == chat.topMessage.id) {
            initialRequest(chat);
            return;
        }
        request = requestUntilLastUnreadRecursive(chat.topMessage.id, until)
                .compose(new GetUsers2(chat.topMessage));
        request.subscribe(new HistoryObserver(true));
    }

    private Observable<TdApi.Messages> requestUntilLastUnreadRecursive(int messageId, final int untilId) {
        return client.getMessages(id, messageId, 0, 100)
                .flatMap(new Func1<TdApi.Messages, Observable<TdApi.Messages>>() {
                    @Override
                    public Observable<TdApi.Messages> call(TdApi.Messages messages) {
                        final Observable<TdApi.Messages> just = just(messages);
                        if (contains(messages, untilId)
                                || messages.messages.length == 0) {
                            return just;
                        } else {
                            final TdApi.Message lastMessage = messages.messages[messages.messages.length - 1];
                            final Observable<TdApi.Messages> next = requestUntilLastUnreadRecursive(lastMessage.id, untilId);
                            return just.zipWith(next, new Func2<TdApi.Messages, TdApi.Messages, TdApi.Messages>() {
                                @Override
                                public TdApi.Messages call(TdApi.Messages o, TdApi.Messages o2) {
                                    final ArrayList<TdApi.Message> res = new ArrayList<>(o.messages.length + o2.messages.length);
                                    addAll(res, o.messages);
                                    addAll(res, o2.messages);
                                    return new TdApi.Messages(0, res.toArray(new TdApi.Message[res.size()]));
                                }
                            });
                        }
                    }
                });
    }

    private boolean contains(TdApi.Messages messages, int untilId) {
        for (TdApi.Message it : messages.messages) {
            if (it.id == untilId) {
                return true;
            }
        }
        return false;
    }

    public void updateContent(TdApi.UpdateMessageContent upd) {
        for (TdApi.Message message : data) {
            if (message.id == upd.messageId) {
                message.message = upd.newContent;
                messageChanged.onNext(message);
                return;
            }
        }
    }

    public void updateMessageDate(TdApi.UpdateMessageDate upd) {
        for (TdApi.Message message : data) {
            if (upd.messageId == message.id) {
                message.date = upd.newDate;
                messageChanged.onNext(message);
                return;
            }
        }
    }

    public void sendMessage(TdApi.User sharedContact) {
        TdApi.InputMessageContact content = new TdApi.InputMessageContact(sharedContact.phoneNumber, sharedContact.firstName, sharedContact.lastName, sharedContact.id);
        sendMessageImpl(content);
    }

    public void handleNewMessageList(List<TdApi.UpdateNewMessage> allhatsNewMessagesBuffer) {
        //здесь могут быть сообщения из другого чата!!!
        List<TdApi.Message> newMessages = new ArrayList<>();
        for (TdApi.UpdateNewMessage u : allhatsNewMessagesBuffer) {
            if (u.message.chatId == id) {
                newMessages.add(u.message);

            }
        }

        handleNewMessage(newMessages);
//        handleNewMessage(newMessages);
    }

    public int getMessageState(TdApi.Message msg, long lastReadOutbox, int myId) {
        if (myId != msg.fromId) {
            return MESSAGE_STATE_READ;
        }
        TdApi.UpdateMessageId upd = getUpdForOldId(msg.id);
        if (msg.id >= MSG_WITHOUT_VALID_ID && upd == null) {
            return MESSAGE_STATE_NOT_SENT;
        } else {
            //message sent
            int id = msg.id;
            if (id >= MSG_WITHOUT_VALID_ID) {
                id = upd.newId;
            }
            if (lastReadOutbox < id) {
                return MESSAGE_STATE_SENT;
            } else {
                return MESSAGE_STATE_READ;
            }
        }
    }

    @Nullable ChatDB.UpdateReplyMarkupWithData currentMarkup;
    private PublishSubject<ChatDB.UpdateReplyMarkupWithData> markup = PublishSubject.create();
    public void handleReplyMarkup(ChatDB.UpdateReplyMarkupWithData response) {
        //save
        currentMarkup = response;//.replyMarkup;
        //todo serialize async
        //show to user
        markup.onNext(response);
    }

    public Observable<ChatDB.UpdateReplyMarkupWithData> getMarkup() {
        return markup
                .observeOn(mainThread());
    }

    @Nullable
    public ChatDB.UpdateReplyMarkupWithData getCurrentMarkup() {
        return currentMarkup;
    }

    public void sendBotCommand(String cmd, TdApi.Message msg) {
        sendMessageImpl(new TdApi.InputMessageText(cmd), null);
    }

    public void sendVoice(Observable<VoiceRecorder.Record> stop) {
        stop.subscribe(new ObserverAdapter<VoiceRecorder.Record>() {
            @Override
            public void onNext(VoiceRecorder.Record response) {
                VoiceRecorder.log("send voice");
                float duration = response.duration;
                if (duration > 0.4f) {
                    final String file = response.file.getAbsolutePath();
                    sendMessageImpl(new TdApi.InputMessageVoice(new TdApi.InputFileLocal(file), (int) duration));
                }
            }
        });
    }

    public void forwardMessages(TdApi.ForwardMessages forwardMessages) {
        client.sendRx(forwardMessages)
                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                    @Override
                    public void onNext(TdApi.TLObject response) {
                        System.out.println(response);
                        final TdApi.Messages ms = (TdApi.Messages) response;
                        for (TdApi.Message m : ms.messages) {
                            simulateUpdateNewMessage(m);
                        }
                    }
                });
    }



    private class GetUsers implements Func1<TdApi.Messages, Observable<? extends Portion>> {
        private final TdApi.Message initMessage;

        public GetUsers(TdApi.Message initMessage) {
            this.initMessage = initMessage;
        }

        @Override
        public Observable<? extends Portion> call(TdApi.Messages portion) {
            checkNotMainThread();

            final List<TdApi.Message> messageList = new ArrayList<>();
            if (initMessage != null) {
                boolean foundDuplicate = false;
                for (TdApi.Message msg : portion.messages) {
                    if (msg.id == initMessage.id){
                        foundDuplicate = true;
                        break;
                    }
                }
                if (!foundDuplicate){
                    messageList.add(initMessage);
                } else {
                    CrashlyticsCore.getInstance().logException(new IllegalStateException("duplicate in messages"));
                }
            }
            addAll(messageList, portion.messages);

            for (TdApi.Message message : messageList) {
                holder.parser.parse(message);
            }
            final List<ChatListItem> split = daySplitter.split(messageList);
            Portion res = new Portion(messageList, split);
            return just(res);
        }
    }

    public Observable<HistoryResponse> history() {
        return historySubject;
    }

    public class HistoryResponse {
        public final List<TdApi.Message> ms;
        /**
         * true если зашли в чат с непрочитанными сообщениями
         */
        public final boolean showUnreadMessages;
        public final List<ChatListItem> split;

        public HistoryResponse(List<TdApi.Message> ms, boolean showUnreadMessages, List<ChatListItem> split) {
            this.ms = ms;
            this.showUnreadMessages = showUnreadMessages;
            this.split = split;
        }
    }

    private class GetUsers2 implements Observable.Transformer<TdApi.Messages, Portion> {
        private final TdApi.Message topMessage;

        public GetUsers2(TdApi.Message topMessage) {
            this.topMessage = topMessage;
        }

        @Override
        public Observable<Portion> call(Observable<TdApi.Messages> original) {
            return original.flatMap(new GetUsers(topMessage))
                    .observeOn(mainThread());
        }
    }

    private class HistoryObserver extends ObserverAdapter<Portion> {
        final boolean unreadRequest;

        public HistoryObserver(boolean unreadRequest) {
            this.unreadRequest = unreadRequest;
        }

        @Override
        public void onNext(Portion response) {
            checkMainThread();
            request = null;
            if (response.ms.isEmpty()) {
                downloadedAll = true;
            } else {
                data.addAll(response.ms);
                historySubject.onNext(new HistoryResponse(response.ms, unreadRequest, response.split));
            }
        }
        //todo on error
    }

    public static class DeletedMessages {
        public final boolean all;
        public final List<TdApi.Message> ms;

        public DeletedMessages() {
            this.all = true;
            ms = Collections.emptyList();
        }

        public DeletedMessages(List<TdApi.Message> ms) {
            this.all = false;
            this.ms = ms;
        }
    }

    public Observable<DeletedMessages> getDeletedMessagesSubject() {
        return deletedMessagesSubject;
    }

    public Observable<TdApi.Message> getMessageChanged() {
        return messageChanged;
    }

    @NonNull
    public Observable<TdApi.Messages> getMediaPreview() {
        final TdApi.Messages lastKnownMedia = this.lastKnownMedia;
        final Observable<TdApi.Messages> request = getMediaPreviewImpl()
                .map(new Func1<TdApi.Messages, TdApi.Messages>() {
                    @Override
                    public TdApi.Messages call(TdApi.Messages messages) {
                        RxChat.this.lastKnownMedia = messages;
                        return messages;
                    }
                });
        if (lastKnownMedia != null){
            final Observable<TdApi.Messages> just = just(lastKnownMedia);
            return just.concatWith(request);
        }
        return request;
    }

    private volatile TdApi.Messages lastKnownMedia;

    @NonNull
    private Observable<TdApi.Messages> getMediaPreviewImpl() {
        return client.sendRx(new TdApi.GetChat(id))
                .flatMap(new Func1<TdApi.TLObject, Observable<TdApi.Messages>>() {
                    @Override
                    public Observable<TdApi.Messages> call(TdApi.TLObject tlObject) {
                        TdApi.Chat chat = (TdApi.Chat) tlObject;
                        //todo deleted history
                        final Observable<TdApi.Chat> justChat = just(chat);
                        final Observable<TdApi.TLObject> messages = client.sendRx(new TdApi.SearchMessages(chat.id, "", chat.topMessage.id, 20, MEDIA_PREVIEW_FILTER));
                        return zip(justChat, messages, new Func2<TdApi.Chat, TdApi.TLObject, TdApi.Messages>() {
                            @Override
                            public TdApi.Messages call(TdApi.Chat chat, TdApi.TLObject tlObject) {
                                final TdApi.Messages res = (TdApi.Messages) tlObject;
                                if (isPhotoOrVideo(chat.topMessage)){
                                    final ArrayList<TdApi.Message> m = new ArrayList<>(Arrays.asList(res.messages));
                                    m.add(0, chat.topMessage);
                                    res.messages = m.toArray(new TdApi.Message[m.size()]);
                                    return res;
                                } else {
                                    return res;
                                }
                            }
                        });
                    }
                });
    }

    public static boolean isPhotoOrVideo(TdApi.Message msg) {
        return msg.message instanceof TdApi.MessagePhoto
                || msg.message instanceof TdApi.MessageVideo;
    }

    private volatile TdApi.GroupChatFull lastKnownGroupChat;

    public Observable<TdApi.GroupChatFull> getGroupChatFull(int groupChatId) {
        final TdApi.GroupChatFull lastKnownGroupChat = this.lastKnownGroupChat;
        final Observable<TdApi.GroupChatFull> request = client.getGroupChatFull(groupChatId).map(new Func1<TdApi.GroupChatFull, TdApi.GroupChatFull>() {
            @Override
            public TdApi.GroupChatFull call(TdApi.GroupChatFull groupChatFull) {
                RxChat.this.lastKnownGroupChat = groupChatFull;
                return groupChatFull;
            }
        });

        if (lastKnownGroupChat == null){
            return request;
        }
        return just(lastKnownGroupChat)
                .concatWith(request);

    }
}
