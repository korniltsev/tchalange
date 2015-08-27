package ru.korniltsev.telegram.core.rx;

import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class SharedMediaHelper {
    final RXClient client;

    //todo move to path
    public SharedMediaHelper(RXClient client) {
        this.client = client;
    }

    Map<Long, Holder> chatIdToHolder = new HashMap<>();

    public Holder getHolder(long chatId) {
        final Holder holder = chatIdToHolder.get(chatId);
        if (holder != null) {
            return holder;
        }
        final Holder value = new Holder(chatId, new ArrayList<TdApi.Message>());
        chatIdToHolder.put(chatId, value);
        return value;
    }

    public class Holder {
        final long chatId;
        public final List<TdApi.Message> msg;
        final PublishSubject<List<TdApi.Message>> historyListener = PublishSubject.create();
        private Subscription subscription = Subscriptions.empty();

        public PublishSubject<List<TdApi.Message>> getHistoryListener() {
            return historyListener;
        }

        private boolean requestInProgress = false;
        private boolean downloadedAll;

        public Holder(long chatId, List<TdApi.Message> msg) {
            this.chatId = chatId;
            this.msg = msg;
        }

        public void request() {
            if (requestInProgress) {
                throw new RuntimeException();
            }
            requestInProgress = true;
            int fromId = 0;
            if (!msg.isEmpty()) {
                fromId = msg.get(msg.size() - 1).id;
            }
            subscription = client.sendRx(new TdApi.SearchMessages(chatId, "", fromId, 50, new TdApi.SearchMessagesFilterPhotoAndVideo()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ObserverAdapter<TdApi.TLObject>() {
                        @Override
                        public void onNext(TdApi.TLObject response) {
                            requestInProgress = false;

                            TdApi.Messages history = (TdApi.Messages) response;
                            if (history.messages.length == 0) {
                                downloadedAll = true;
                            }
                            Collections.addAll(msg, history.messages);
                            historyListener.onNext(
                                    asList(history.messages));
                        }
                    });
        }

        public boolean isRequestInProgress() {
            return requestInProgress;
        }

        public boolean isDownloadedAll() {
            return downloadedAll;
        }

        public void clear() {
            msg.clear();
            downloadedAll = false;
            requestInProgress = false;
            subscription.unsubscribe();
        }
    }
}
