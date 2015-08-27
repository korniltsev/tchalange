package ru.korniltsev.telegram.profile.media.controllers;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.SharedMediaHelper;
import ru.korniltsev.telegram.profile.media.SharedMediaPath;
import ru.korniltsev.telegram.profile.media.SharedMediaView;
import rx.Subscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SharedMediaController extends MediaController {
    private final SharedMediaView sharedMediaView;
    final RecyclerView list;
    private final SharedMediaHelper.Holder helper;
    private final TextView title;
    private final SharedMediaPath path;
    private final Subscription subscribe;
    private final SharedMediaAdapter adapter;
    private final DpCalculator calculator;

    public SharedMediaController(final SharedMediaView sharedMediaView, RecyclerView list, TextView title, SharedMediaPath path) {
        this.sharedMediaView = sharedMediaView;
        this.list = list;
        this.title = title;
        this.path = path;
        final Context ctx = list.getContext();
        final MyApp from = MyApp.from(ctx);
        calculator = from.calc;
        helper = from.sharedMediaHelper.getHolder(path.chatId);
        title.setText(R.string.shared_media_title);

        adapter = new SharedMediaAdapter(ctx, new SharedMediaAdapter.Callback() {
            @Override
            public void itemsSelected(int selectedItems) {
                sharedMediaView.setToolbarVisible(selectedItems);
            }
        }, list);
        final GridLayoutManager layout = new GridLayoutManager(ctx, 3);
        list.setLayoutManager(layout);
        list.addOnScrollListener(new EndlessOnScrollListener(layout, adapter, new Runnable() {
            @Override
            public void run() {
                requestNewPortion();
            }
        }));
        layout.setSpanSizeLookup(adapter.createLookup());
        refreshData();
        list.setAdapter(adapter);

        subscribe = helper.getHistoryListener()
                .subscribe(new ObserverAdapter<List<TdApi.Message>>() {
                    @Override
                    public void onNext(List<TdApi.Message> response) {

                        refreshData();
                    }
                });
        requestNewPortion();


        final int dip4 = calculator.dp(4);
        list.setPadding(dip4, 0, dip4, 0);
        list.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                final int position = parent.getChildViewHolder(view).getAdapterPosition();
                final SharedMediaAdapter.Item item = adapter.getItem(position);
                if (item instanceof SharedMediaAdapter.Section) {
                    outRect.set(0, 0, 0, 0);
                } else {
                    outRect.set(dip4, dip4, dip4, dip4);
                }

            }
        });
    }

    private void refreshData() {
        final List<SharedMediaAdapter.Item> split = split(AppUtils.filterPhotosAndVideos(helper.msg));
        adapter.setData(split);
    }
    private static int idCounter = -1;
    private List<SharedMediaAdapter.Item> split(List<TdApi.Message> msg) {
        if (msg.isEmpty()) {
            return Collections.emptyList();
        }
        final ArrayList<SharedMediaAdapter.Item> res = new ArrayList<>();
        TdApi.Message first = msg.get(0);
        DateTime prevDate = time(first);
        res.add(new SharedMediaAdapter.Section(prevDate, idCounter--));
        res.add(new SharedMediaAdapter.Media(first));
        for (int i = 1; i < msg.size(); ++i) {
            final TdApi.Message message = msg.get(i);
            final DateTime time = time(message);
            if (time.getMonthOfYear() == prevDate.getMonthOfYear()
                    && time.getYear() == prevDate.getYear()) {
            } else {
                res.add(new SharedMediaAdapter.Section(time, idCounter--));
            }
            res.add(new SharedMediaAdapter.Media(message));
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

    private void requestNewPortion() {
        if (helper.isDownloadedAll() || helper.isRequestInProgress()) {
            return;
        }
        helper.request();
    }

    @Override
    public Set<Integer> getSelectedMessagesIds() {
        return adapter.selectedIds;
    }

    @Override
    public void drop() {
        subscribe.unsubscribe();
    }

    @Override
    public void dropSelection() {
        adapter.dropSelection();
    }

    @Override
    public void messagesDeleted(int[] msgIds) {
        for (Iterator<SharedMediaAdapter.Item> iterator = adapter.getData().iterator(); iterator.hasNext(); ) {
            SharedMediaAdapter.Item i = iterator.next();
            if (i instanceof SharedMediaAdapter.Media) {
                final TdApi.Message msg = ((SharedMediaAdapter.Media) i).msg;
                for (int msgId : msgIds) {
                    if (msgId == msg.id) {
                        iterator.remove();
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
        dropSelection();
    }
}
