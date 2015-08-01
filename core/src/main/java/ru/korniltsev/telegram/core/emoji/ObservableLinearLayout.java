/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package ru.korniltsev.telegram.core.emoji;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import mortar.dagger1support.ObjectGraphService;

import javax.inject.Inject;
import java.lang.reflect.Field;

public class ObservableLinearLayout extends FrameLayout {

    private final int statusBarHeight;
    private final Point displaySize = new Point();
    private final SharedPreferences prefs;
    private Rect rect = new Rect();
    private int keyboardHeight;
    private ObservableLinearLayout.CallBack cb;

    @Inject DpCalculator calc;

    public interface CallBack {
        void onLayout(int keyboardHeight);
    }

    public ObservableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        setWillNotDraw(false);
        prefs = context.getSharedPreferences("EmojiPopup", Context.MODE_PRIVATE);

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        } else {
            statusBarHeight = 0;
        }

        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        if (display != null) {
            display.getSize(displaySize);
        }
    }

    public void setCallback(CallBack cb) {
        this.cb = cb;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        View rootView = this.getRootView();
        int usableViewHeight = rootView.getHeight() - statusBarHeight - getViewInset(rootView);
        this.getWindowVisibleDisplayFrame(rect);
        keyboardHeight = usableViewHeight - (rect.bottom - rect.top);
        if (keyboardHeight >0 ){
            saveKeyboardHeight(keyboardHeight);
        }
        if (cb != null) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (cb != null) {
                        cb.onLayout(keyboardHeight);
                    }
                }
            });
        }
    }

    public int getKeyboardHeight() {
        return keyboardHeight;
    }

    public static int getViewInset(View view) {
        if (view == null || Build.VERSION.SDK_INT < 21) {
            return 0;
        }
        try {
            Field mAttachInfoField = View.class.getDeclaredField("mAttachInfo");
            mAttachInfoField.setAccessible(true);
            Object mAttachInfo = mAttachInfoField.get(view);
            if (mAttachInfo != null) {
                Field mStableInsetsField = mAttachInfo.getClass().getDeclaredField("mStableInsets");
                mStableInsetsField.setAccessible(true);
                Rect insets = (Rect) mStableInsetsField.get(mAttachInfo);
                return insets.bottom;
            }
        } catch (Exception e) {
            Log.e("libtd", "reflection err", e);
        }
        return 0;
    }

    private void saveKeyboardHeight(int keyboardHeight) {
        boolean portrait = isPortrait();
        prefs.edit()
                .putInt(getKeyForConfiguration(portrait), keyboardHeight)
                .apply();
    }

    public int guessKeyboardHeight() {
        boolean portrait = isPortrait();
        String prefKey = getKeyForConfiguration(portrait);
        return prefs.getInt(prefKey, calc.dp(portrait ? 240 : 150));
    }

    private String getKeyForConfiguration(boolean portrait){
        String prefKey;
        prefKey = "keyboard_height_" + portrait;
        return prefKey;
    }

    private boolean isPortrait(){
        int orientation = getContext().getResources().getConfiguration().orientation;
        boolean portrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        return portrait;
    }
}
