package ru.korniltsev.telegram.core.recycler;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

public class CheckRecyclerViewSpan  {
    /**
     * @param target
     * @param callback called if the view span of the target is not filled
     */
    public static void check(final RecyclerView target, final Runnable callback) {
        Log.d("CheckRecyclerViewSpan", "check");
        target.getViewTreeObserver()
                .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        Log.d("CheckRecyclerViewSpan", "onPredraw");
                        ViewTreeObserver v = target.getViewTreeObserver();
                        if (!v.isAlive()) {
                            return true;
                        }
                        v.removeOnPreDrawListener(this);
                        RecyclerView.Adapter a = target.getAdapter();
                        LinearLayoutManager lm = (LinearLayoutManager) target.getLayoutManager();
                        if (lm.findFirstCompletelyVisibleItemPosition() == 0
                                && lm.findLastCompletelyVisibleItemPosition() == a.getItemCount() - 1) {
                            Log.d("CheckRecyclerViewSpan", "run");
                            callback.run();
                        }
                        return true;
                    }
                });
    }


}
