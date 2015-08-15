package ru.korniltsev.telegram.core.flow.pathview;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import flow.path.Path;

public abstract class BasePath extends Path {
    public abstract int getRootLayout();

    public View constructViewManually(Context ctx, FrameLayout root) {
        return null;
    }
}
