package ru.korniltsev.telegram.core;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.crashlytics.android.core.CrashlyticsCore;
import org.drinkless.td.libcore.telegram.TdApi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

/**
 * Created by korniltsev on 23/04/15.
 */
public class Utils {
    public static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        int memoryClass = am.getMemoryClass();
        // Target ~15% of the available heap.
        return 1024 * 1024 * memoryClass / 7;
    }

    public static String textFrom(EditText e) {
        return e.getText().toString();
    }

    public static void hideKeyboard(EditText e) {
        InputMethodManager imm = (InputMethodManager) e.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(e.getWindowToken(), 0);
    }

    public static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new BufferedOutputStream(new FileOutputStream(dest));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    public static int compare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static int compare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static long dateToMillis(long date) {
        return date * 1000;
    }

    public static int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
    }

    @Nullable
    public static String getGalleryPickedFilePath(Context ctx, Intent data) {
        Uri selectedImage = data.getData();
        // h=1;
        //imgui = selectedImage;
        ContentResolver contentResolver = ctx.getContentResolver();

        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor c;
        if (SDK_INT >= 19) {
            // Will return "image:x*"
            String wholeID = DocumentsContract.getDocumentId(selectedImage);
            // Split at colon, use second item in the array
            String id = wholeID.split(":")[1];
            // where id is equal to
            String sel = MediaStore.Images.Media._ID + "=?";
            c = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, sel, new String[]{id}, null);
        } else {
            c = contentResolver.query(selectedImage, projection, null, null, null);
        }
        String picturePath;
        if (c.moveToNext()) {
            picturePath = c.getString(c.getColumnIndex(MediaStore.Images.Media.DATA));
        } else {
            picturePath = null;
        }
        c.close();
        return picturePath;
    }

    public static void event(String eventName) {
        CrashlyticsCore.getInstance()
                .log(Log.INFO, "Event", eventName);
    }

    public static void setStatusBarColor(Activity a, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            a.getWindow().setStatusBarColor(color);
        }
    }

    public static void hideKeyboard(View anchor) {
        Context c = anchor.getContext();
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(anchor.getWindowToken(), 0);
    }

    public static void showKeyboard(View anchor) {
        //        Context c = anchor.getContext();
        InputMethodManager inputManager = (InputMethodManager) anchor.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(anchor, InputMethodManager.SHOW_IMPLICIT);
        //        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        //        imm.showS(InputMethodManager.SHOW_FORCED, 0);
    }

    public static void toggleKeyboard(View anchor) {
        InputMethodManager inputManager = (InputMethodManager) anchor.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        inputManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static void logDuration(long start, long end, String msg) {
        Log.d("Duration", msg + (end - start));
    }

//    public static Iterable<View> childrenOf(final ViewGroup view) {
//        return new Iterable<View>(){
//
//            @Override
//            public Iterator<View> iterator() {
//                return new ViewGroupChildIterator(view);
//            }
//        };
//    }
//    private static class ViewGroupChildIterator implements Iterator<View> {
//        final ViewGroup root;
//        private final int total;
//        int current = 0;
//
//        public ViewGroupChildIterator(ViewGroup root) {
//            this.root = root;
//            total = root.getChildCount();
//        }
//
//        @Override
//        public boolean hasNext() {
//            return current < total;
//        }
//
//        @Override
//        public View next() {
//            final View childAt = root.getChildAt(current);
//            current++;
//            return childAt;
//        }
//
//        @Override
//        public void remove() {
//
//        }
//    }
}
