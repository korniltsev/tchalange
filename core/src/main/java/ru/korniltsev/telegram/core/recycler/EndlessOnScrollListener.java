package ru.korniltsev.telegram.core.recycler;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class EndlessOnScrollListener extends RecyclerView.OnScrollListener {
    public static final int DEFAULT_DO_NOT_WAIT_FOR_FULL_SCROLL = 5;
    final int doNowWaitForFullScroll ;
    final LinearLayoutManager lm;
    final RecyclerView.Adapter a;
    final Runnable run;
    public EndlessOnScrollListener(LinearLayoutManager lm, RecyclerView.Adapter a, Runnable run0) {
        this(lm, a, run0, DEFAULT_DO_NOT_WAIT_FOR_FULL_SCROLL);
    }
    public EndlessOnScrollListener(LinearLayoutManager lm, RecyclerView.Adapter a, Runnable run, int doNowWaitForFullScroll) {
        this.lm = lm;
        this.a = a;
        this.run = run;
        this.doNowWaitForFullScroll = doNowWaitForFullScroll;
    }

    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

        if (lm.findLastVisibleItemPosition() >= a.getItemCount() - 1 - doNowWaitForFullScroll) {
            run.run();
        }
    }
}