package ru.korniltsev.telegram.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import com.crashlytics.android.answers.Answers;
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
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.profile.other.ProfileAdapter;
import ru.korniltsev.telegram.profile.other.ProfilePresenter;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static android.text.TextUtils.isEmpty;
import static ru.korniltsev.telegram.core.rx.RxChat.isPhotoOrVideo;
import static rx.Observable.zip;

public class AppUtils {
    public static final Looper MAIN_LOOPER = Looper.getMainLooper();
    public static final Handler MAIN_HANDLER = new Handler(MAIN_LOOPER);
    public static final int REQUEST_CHOOS_FROM_GALLERY = 1;
    public static final int REQUEST_TAKE_PHOTO = 2;
    public static final int REQUEST_CHOOS_FROM_GALLERY_MY_AVATAR = 3;
    public static final int REQUEST_TAKE_PHOTO_MY_AVATAR = 4;
    public static final int REQUEST_CHOOS_FROM_GALLERY_CHAT_AVATAR = 5;
    public static final int REQUEST_TAKE_PHOTO_CHAT_AVATAR = 6;
    public static final PeriodFormatter DURATION_FORMATTER = new PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(1).appendMinutes()
            .appendSeparator(":")
            .minimumPrintedDigits(2).printZeroAlways()
            .appendSeconds()
            .toFormatter();
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
        if (lastName.length() != 0) {
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
                } else if (hoursBetween > 0) {
                    //show hours
                    offlineStatusText = res.getQuantityString(R.plurals.user_status_last_seen_n_hours_ago, hoursBetween, hoursBetween);
                } else {
                    //user has wrong date - fallback to SUBTITLE_FORMATTER
                    String date = SUBTITLE_FORMATTER.print(wasOnlineTime);
                    offlineStatusText = res.getString(R.string.user_status_last_seen, date);
                }
            } else if (daysBetween > 0) {
                //show n days ago
                if (daysBetween <= 7) {
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

            return offlineStatusText;
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
        if (user.phoneNumber == null) {
            return "";
        }
        final String phoneNumber = user.phoneNumber;
        if (phoneNumber.startsWith("+")) {
            return phoneNumber;
        } else {
            return "+" + user.phoneNumber;
        }
    }

    public static void clear(Rect outRect) {
        outRect.set(0, 0, 0, 0);
    }

    @Nullable
    public static View getChildWithAdapterPosition(RecyclerView list, int position) {
        for (int i = 0; i < list.getChildCount(); ++i) {
            final View child = list.getChildAt(i);
            final RecyclerView.ViewHolder vh = list.getChildViewHolder(child);
            if (vh.getAdapterPosition() == position) {
                return child;
            }
        }
        return null;
    }

    public static int uiStatusColor(TdApi.UserStatus status) {
        if (status instanceof TdApi.UserStatusOnline) {
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
        if (newTopPath != null) {
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

    public static void executeOnPreDraw(final View view, final Runnable run) {
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
        if (bytes < unit) {
            return bytes + " b";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp - 1) + "";
        return String.format("%.1f %sb", bytes / Math.pow(unit, exp), pre);
    }

    public static String kb(int size) {
        return humanReadableByteCount(size);
    }

    public static void showNoActivityError(Context ctx) {
        Toast.makeText(ctx, "There is no app to view the document. The file is stored in downloads foled", Toast.LENGTH_LONG)
                .show();
    }

    @NonNull
    public static File getTmpFileForCamera() {
        return new File(Environment.getExternalStorageDirectory(), "temp.jpg");
    }

    public static void logEvent(String event) {
        Answers.getInstance().logEvent(event);
    }

    public static String performerOf(TdApi.Audio currentAudio) {
        if (isEmpty(currentAudio.performer)) {
            return currentAudio.fileName;
        } else {
            return currentAudio.performer;
        }
    }

    public static void rtlPerformanceFix(View v) {
        //        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1){
        //            v.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        //        }
    }

    //    @NonNull
    //    public static Observable<TdApi.Messages> getMedia(final RXClient client, long chatId) {
    //        return client.sendRx(new TdApi.GetChat(chatId))
    //                .flatMap(new Func1<TdApi.TLObject, Observable<TdApi.Messages>>() {
    //                    @Override
    //                    public Observable<TdApi.Messages> call(TdApi.TLObject tlObject) {
    //                        TdApi.Chat chat = (TdApi.Chat) tlObject;
    //                        //todo deleted history
    //                        final Observable<TdApi.Chat> justChat = Observable.just(chat);
    //                        final Observable<TdApi.TLObject> messages = client.sendRx(new TdApi.SearchMessages(chat.id, "", chat.topMessage.id, 20, ProfilePresenter.FILTER));
    //                        return zip(justChat, messages, new Func2<TdApi.Chat, TdApi.TLObject, TdApi.Messages>() {
    //                            @Override
    //                            public TdApi.Messages call(TdApi.Chat chat, TdApi.TLObject tlObject) {
    //                                final TdApi.Messages res = (TdApi.Messages) tlObject;
    //                                if (isPhotoOrVideo(chat.topMessage)){
    //                                    final ArrayList<TdApi.Message> m = new ArrayList<>(Arrays.asList(res.messages));
    //                                    m.add(0, chat.topMessage);
    //                                    res.messages = m.toArray(new TdApi.Message[m.size()]);
    //                                    return res;
    //                                } else {
    //                                    return res;
    //                                }
    //                            }
    //                        });
    //                    }
    //                });
    //    }

    public static <T> List<T> flatten(List<List<T>> sections) {
        final ArrayList<T> res = new ArrayList<>();
        for (List<T> section : sections) {
            res.addAll(section);
        }
        return res;
    }

    public static <T> List<List<T>> filterNonEmpty(List<List<T>> sections) {
        final ArrayList<List<T>> lists = new ArrayList<>();
        for (List<T> section : sections) {
            if (!section.isEmpty()) {
                lists.add(section);
            }
        }
        return lists;
    }

    public static List<TdApi.Message> filterPhotosAndVideos(List<TdApi.Message> ms) {
        final ArrayList<TdApi.Message> res = new ArrayList<>();
        for (TdApi.Message msg : ms) {
            if (isPhotoOrVideo(msg)) {
                res.add(msg);
            }
        }
        return res;
    }

    public static boolean isUIThread() {
        return Looper.myLooper() == MAIN_LOOPER;
    }

    public static void runOnUIThread(Runnable r) {
        if (isUIThread()) {
            r.run();
        } else {
            MAIN_HANDLER.post(r);
        }
    }
}
