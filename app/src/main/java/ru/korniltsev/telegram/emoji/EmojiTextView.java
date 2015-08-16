package ru.korniltsev.telegram.emoji;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.images.Emoji;
import rx.Subscription;

import javax.inject.Inject;

//invalidates itself when the emojies are loaded
public class EmojiTextView extends TextView{
    Emoji emoji;
    private Subscription s;

    public EmojiTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        emoji = MyApp.from(context).emoji;
    }

    public EmojiTextView(Context context) {
        super(context);
        emoji = MyApp.from(context).emoji;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        s = emoji.pageLoaded()
                .subscribe(new ObserverAdapter<Bitmap>() {
            @Override
            public void onNext(Bitmap response) {
                invalidate();
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        s.unsubscribe();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void drawableStateChanged() {

    }
}
