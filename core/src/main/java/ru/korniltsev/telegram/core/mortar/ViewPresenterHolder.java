package ru.korniltsev.telegram.core.mortar;

import android.content.Context;
import mortar.ViewPresenter;

public class ViewPresenterHolder {
    public static ViewPresenter get(Context ctx) {
        final ViewPresenterHolder systemService = (ViewPresenterHolder) ctx.getSystemService(SERVICE_NAME);
        return systemService.viewPresenter;
    }
    public static final String SERVICE_NAME = "ru.korniltsev.telegram.core.mortar.ViewPresenterHolder";
    final ViewPresenter viewPresenter;

    public ViewPresenterHolder(ViewPresenter viewPresenter) {
        this.viewPresenter = viewPresenter;
    }

    public interface Factory {
        ViewPresenter create(Context ctx);
    }
}
