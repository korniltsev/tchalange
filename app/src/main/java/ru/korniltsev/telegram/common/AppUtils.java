package ru.korniltsev.telegram.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import flow.Flow;
import flow.History;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.korniltsev.telegram.chat.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AppUtils {
    private static DateTimeFormatter SUBTITLE_FORMATTER = DateTimeFormat.forPattern("dd/MM/yy");

    public static String uiName(TdApi.User user, Context ctx) {//todo
        if (user == null) {
            return "";
        }
        if (user.type instanceof TdApi.UserTypeDeleted) {
            return ctx.getString(R.string.deleted_account);
        }
        String firstName = user.firstName;
        String lastName = user.lastName;
        String name = uiName(firstName, lastName);
        return name;
    }

    public static String uiName(String firstName, String lastName) {
        String name;
        StringBuilder sb = new StringBuilder();
        if (firstName.length() != 0) {
            sb.append(firstName);
        }
        if (lastName.length() != 0){
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(lastName);
        }
        name = sb.toString();
        return name;
    }

    public static String uiUserStatus(Context context, TdApi.UserStatus status) {
        if (status instanceof TdApi.UserStatusOnline) {
            return context.getString(R.string.user_status_online);
        } else if (status instanceof TdApi.UserStatusOffline) {
            long wasOnline = ((TdApi.UserStatusOffline) status).wasOnline;
            long timeInMillis = wasOnline * 1000;
            //            Date date = new Date(timeInMillis);
            DateTime wasOnlineTime = new DateTime(timeInMillis, DateTimeZone.UTC)
                    .withZone(DateTimeZone.getDefault());

            DateTime now = DateTime.now();


            String offlineStatusText;
            int daysBetween = Days.daysBetween(wasOnlineTime, now)
                    .getDays();
            final Resources res = context.getResources();
            if (daysBetween == 0) {
                int hoursBetween = Hours.hoursBetween(wasOnlineTime, now)
                        .getHours();
                if (hoursBetween == 0) {
                    int minutesBetween = Minutes.minutesBetween(wasOnlineTime, now)
                            .getMinutes();
                    if (minutesBetween == 0) {
                        //just now
                        offlineStatusText = res.getString(R.string.user_status_just_now);
                    } else if (minutesBetween > 0) {
                        //n minutes
                        offlineStatusText = res.getQuantityString(R.plurals.user_status_last_seen_n_minutes_ago, minutesBetween, minutesBetween);
                    } else {
                        //user has wrong date - fallback to SUBTITLE_FORMATTER
                        String date = SUBTITLE_FORMATTER.print(wasOnlineTime);
                        offlineStatusText = res.getString(R.string.user_status_last_seen, date);
                    }
                } else if (hoursBetween > 0){
                    //show hours
                    offlineStatusText = res.getQuantityString(R.plurals.user_status_last_seen_n_hours_ago, hoursBetween, hoursBetween);
                } else {
                    //user has wrong date - fallback to SUBTITLE_FORMATTER
                    String date = SUBTITLE_FORMATTER.print(wasOnlineTime);
                    offlineStatusText = res.getString(R.string.user_status_last_seen, date);
                }
            } else if (daysBetween > 0){
                //show n days ago
                if (daysBetween <= 7){
                    offlineStatusText = res.getQuantityString(R.plurals.user_status_last_seen_n_days_ago, daysBetween, daysBetween);
                } else {
                    String date = SUBTITLE_FORMATTER.print(wasOnlineTime);
                    offlineStatusText = res.getString(R.string.user_status_last_seen, date);
                }
            } else {
                //user has wrong date - fallback to SUBTITLE_FORMATTER
                String date = SUBTITLE_FORMATTER.print(wasOnlineTime);
                offlineStatusText = res.getString(R.string.user_status_last_seen, date);
            }

            return  offlineStatusText;
        } else if (status instanceof TdApi.UserStatusLastWeek) {
            return context.getString(R.string.user_status_last_week);
        } else if (status instanceof TdApi.UserStatusLastMonth) {
            return context.getString(R.string.user_status_last_month);
        } else if (status instanceof TdApi.UserStatusRecently) {
            return context.getString(R.string.user_status_recently);
        } else {
            //empty
            return "";
        }
    }

    public static void copy(Context ctx, String phone) {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("phone", phone);
        clipboard.setPrimaryClip(clip);
    }

    public static void call(Context ctx, String phone) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        ctx.startActivity(intent);
    }

    @NonNull
    public static String phoneNumberWithPlus(@NonNull TdApi.User user) {
        if (user.phoneNumber == null){
            return "";
        }
        final String phoneNumber = user.phoneNumber;
        if (phoneNumber.startsWith("+")){
            return phoneNumber;
        } else {
            return "+" + user.phoneNumber;
        }
    }

    public static void clear(Rect outRect) {
        outRect.set(0,0,0,0);
    }

    @Nullable
    public static View getChildWithAdapterPosition(RecyclerView list, int position){
        for (int i = 0; i < list.getChildCount(); ++i){
            final View child = list.getChildAt(i);
            final RecyclerView.ViewHolder vh = list.getChildViewHolder(child);
            if (vh.getAdapterPosition() == position) {
                return child;

            }
        }
        return null;
    }

    public static int uiStatusColor(TdApi.UserStatus status) {
        if (status instanceof TdApi.UserStatusOnline){
            return 0xff2f6fb3;
        } else {
            return 0xff979797;
        }
    }

    public static void flowPushAndRemove(View ctx, @Nullable Object newTopPath, FlowHistoryStripper f, Flow.Direction forward) {

        final Flow flow = Flow.get(ctx);
        final History history = flow
                .getHistory();
        List<Object> paths = new ArrayList<>();
        final Iterator<Object> it = history.reverseIterator();
        while (it.hasNext()) {
            final Object next = it.next();
            if (f.shouldRemovePath(next)) {
                continue;
            }
            paths.add(next);
        }
        if (newTopPath != null){
            paths.add(newTopPath);
        }
        final History.Builder builder = history.buildUpon();
        builder.clear();
        builder.addAll(paths);
        flow.setHistory(builder.build(), forward);
    }

    public static void toastUnsupported(Context context) {
        Toast.makeText(context, R.string.feature_unsupported, Toast.LENGTH_LONG).show();
    }

    public  static void executeOnPreDraw( final View view, final Runnable run) {
        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                run.run();
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " b";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ( "KMGTPE").charAt(exp-1) + "";
        return String.format("%.1f %sb", bytes / Math.pow(unit, exp), pre);
    }

    public static String kb(int size) {
        return humanReadableByteCount(size);
    }

    public static void logDuration(long start, long end, String msg) {
        Log.d("Duration", msg + (end - start));
    }
}
