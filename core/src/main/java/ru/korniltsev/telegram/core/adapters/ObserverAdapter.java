package ru.korniltsev.telegram.core.adapters;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.crashlytics.android.core.CrashlyticsCore;
import junit.framework.Assert;
import ru.korniltsev.telegram.utils.BuildConfig;
import rx.Observer;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by korniltsev on 21/04/15.
 */
public class ObserverAdapter<T> implements Observer<T> {
    public static Context ctx;
    public static Handler MAIN_THREAD_HANDLER;
    @Override
    public final void onCompleted() {

    }

    @Override
    public void onError(Throwable th) {
        CrashlyticsCore.getInstance().logException(th);
        Log.e("ObserverAdapter", "err", th);
        if (BuildConfig.DEBUG){
            final StringWriter stringWriter = new StringWriter();
            th.printStackTrace(new PrintWriter(stringWriter));
            MAIN_THREAD_HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ctx, stringWriter.toString(), Toast.LENGTH_LONG).show();
                }
            });

        }
    }

    @Override
    public void onNext(T response) {

    }
}
