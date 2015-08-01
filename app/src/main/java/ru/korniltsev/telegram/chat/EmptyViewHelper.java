package ru.korniltsev.telegram.chat;

import android.support.v7.widget.RecyclerView;

class EmptyViewHelper extends RecyclerView.AdapterDataObserver {
    final Runnable run;

    EmptyViewHelper(Runnable run) {
        this.run = run;
    }

    @Override
    public void onChanged() {
        run.run();
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        onChanged();
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        onChanged();
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        onChanged();
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        onChanged();
    }
}
