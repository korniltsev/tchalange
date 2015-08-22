package ru.korniltsev.telegram.profile.media.controllers;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.SharedMediaHelper;
import ru.korniltsev.telegram.profile.media.SharedMediaPath;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioMessagesController extends MediaController {
    final RecyclerView list;
//    private final SharedMediaHelper.Holder helper;
    private final SharedMediaPath path;
//    private final Subscription subscribe;
//    private final SharedMediaAdapter adapter;
    private final DpCalculator calculator;
    final RXClient client;
    private final Subscription subscription;
    private final AudioMessagesAdapter adapter;

    public AudioMessagesController(RecyclerView list, TextView title, SharedMediaPath path, RXClient client) {
        this.list = list;
        this.path = path;
        this.client = client;
        final Context ctx = list.getContext();
        final MyApp from = MyApp.from(ctx);
        calculator = from.calc;

        title.setText(R.string.audio_files);
        subscription = RXClient.getAllMedia(client, path.chatId)
                .observeOn(AndroidSchedulers.mainThread())
                .cache()
                .subscribe(new ObserverAdapter<List<TdApi.Message>>() {
                    @Override
                    public void onNext(List<TdApi.Message> response) {
                        adapter.addAll(
                                split(
                                        filter(response)));
                    }
                });

        adapter = new AudioMessagesAdapter(ctx);
        list.setAdapter(adapter);
        list.setLayoutManager(new LinearLayoutManager(ctx));
        //        final List<TdApi.Message> msg = helper.msg;
//        adapter = new SharedMediaAdapter(ctx);
//        final GridLayoutManager layout = new GridLayoutManager(ctx, 3);
//        list.setLayoutManager(layout);
//        list.addOnScrollListener(new EndlessOnScrollListener(layout, adapter, new Runnable() {
//            @Override
//            public void run() {
//                requestNewPortion();
//            }
//        }));
//        layout.setSpanSizeLookup(adapter.createLookup());
//        refreshData();
//        list.setAdapter(adapter);
//
//        subscribe = helper.getHistoryListener()
//                .subscribe(new ObserverAdapter<List<TdApi.Message>>() {
//                    @Override
//                    public void onNext(List<TdApi.Message> response) {
//
//                        refreshData();
//                    }
//                });
//        requestNewPortion();
//
//
//        final int dip4 = calculator.dp(4);
//        list.setPadding(dip4, 0, dip4, 0);
//        list.addItemDecoration(new RecyclerView.ItemDecoration() {
//            @Override
//            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
//                final int position = parent.getChildViewHolder(view).getAdapterPosition();
//                final SharedMediaAdapter.Item item = adapter.getItem(position);
//                if (item instanceof SharedMediaAdapter.Section) {
//                    outRect.set(0, 0, 0, 0);
//                } else {
//                    outRect.set(dip4, dip4, dip4, dip4);
//                }
//
//            }
//        });
                // 36
        //15
    }

    private List<TdApi.Message> filter(List<TdApi.Message> response) {
        final ArrayList<TdApi.Message> res = new ArrayList<>();
        for (TdApi.Message message : response) {
            if (message.message instanceof TdApi.MessageAudio) {
                res.add(message);
            }
        }
        return res;
    }

    //    private void refreshData() {
//        final List<SharedMediaAdapter.Item> split = split(helper.msg);
//        adapter.setData(split);
//    }

    private List<AudioMessagesAdapter.Item> split(List<TdApi.Message> msg) {
        if (msg.isEmpty()) {
            return Collections.emptyList();
        }
        final ArrayList<AudioMessagesAdapter.Item> res = new ArrayList<>();
        TdApi.Message first = msg.get(0);
        DateTime prevDate = time(first);
        res.add(new AudioMessagesAdapter.Section(prevDate));
        res.add(new AudioMessagesAdapter.Media(first));
        for (int i = 1; i < msg.size(); ++i) {
            final TdApi.Message message = msg.get(i);
            final DateTime time = time(message);
            if (time.getMonthOfYear() == prevDate.getMonthOfYear()
                    && time.getYear() == prevDate.getYear()) {
            } else {
                res.add(new AudioMessagesAdapter.Section(time));
            }
            res.add(new AudioMessagesAdapter.Media(message));
            prevDate = time;
        }
        return res;
    }

    private DateTime time(TdApi.Message prev) {
        long timeInMillis = Utils.dateToMillis(prev.date);
        long local = DateTimeZone.UTC.convertUTCToLocal(timeInMillis);
        return new DateTime(local);
//        long localTime = DateTimeZone.UTC.convertUTCToLocal(prev.date);
//        final DateTime dateTime = new DateTime(localTime);
//        return dateTime;//new SharedMediaAdapter.Section(dateTime);
    }

//    private void requestNewPortion() {
//        if (helper.isDownloadedAll() || helper.isRequestInProgress()) {
//            return;
//        }
//        helper.request();
//    }

    @Override
    public void drop() {
        subscription.unsubscribe();
//        subscribe.unsubscribe();
    }
}
