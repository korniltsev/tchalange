package ru.korniltsev.telegram.core.recycler;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class EndlessOnScrollListener extends RecyclerView.OnScrollListener {
        final LinearLayoutManager lm;
        final RecyclerView.Adapter a;
        final Runnable run;

        public EndlessOnScrollListener(LinearLayoutManager lm, RecyclerView.Adapter a, Runnable run) {
            this.lm = lm;
            this.a = a;
            this.run = run;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (lm.findLastVisibleItemPosition() == a.getItemCount()-1){
                run.run();
            }
        }
    }