package ru.korniltsev.telegram.core.rx;

import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.emoji.images.Emoji;
import rx.subjects.PublishSubject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiParser {
    final Emoji emoji;
    //guarded by client thread
    final Map<String, Spannable> cache = new HashMap<>();
    //    stolen here
    //    https://github.com/regexps/mentions-regex/blob/master/index.js
    private final Pattern userReference = Pattern.compile("(?:^|[^a-zA-Z0-9_＠!@#$%&*])(?:(?:@|＠)(?!/))([a-zA-Z0-9/_]{1,15})(?:\\b(?!@|＠)|$)");

    private PublishSubject<ReferenceSpan> clickedSpans = PublishSubject.create();

    public EmojiParser(Emoji emoji) {
        this.emoji = emoji;
    }

    //    static SimpleDateFormat fuckRuFormatter = new SimpleDateFormat("kk:mm", Locale.US);
    //    public static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormat.forPattern("K:mm a");
    public void parse(TdApi.Message msg) {
        msg.dateFormatted = formatter.get().formatDate(msg);
        if (msg.message instanceof TdApi.MessageText) {
            parseEmojis(msg);
        } else if (msg.message instanceof TdApi.MessagePhoto) {
            pareImageSizes((TdApi.MessagePhoto) msg.message);
        } else if (msg.message instanceof TdApi.MessageWebPage){
            final TdApi.MessageWebPage message = (TdApi.MessageWebPage) msg.message;

            message.parsedText = parseEmoji(message.text, msg.fromId);
        }
    }

    // parses commands of type "/foo_bar"
    private Pattern botCommandsParser = Pattern.compile("(?m)(\\s|^)(/[a-zA-Z@\\d_]{1,255})");

    private void pareImageSizes(TdApi.MessagePhoto message) {
        //todo rename the class
        for (TdApi.PhotoSize photo : message.photo.photos) {
            if (photo.photo.isLocal()) {
                if (photo.width == 0 || photo.height == 0) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(photo.photo.path, o);
                    photo.width = o.outWidth;
                    photo.height = o.outHeight;
                }
            }
        }
    }

    private void parseEmojis(TdApi.Message msg) {
        TdApi.MessageText text = (TdApi.MessageText) msg.message;
        String key = text.text;

        text.textWithSmilesAndUserRefs = parseEmoji(key, msg.fromId);
    }

    public Spannable parseEmoji(final String key, int userId) {
        Spannable fromCache = cache.get(key);
        if (fromCache != null) {
            return fromCache;
        } else {
            CharSequence parsed = emoji.replaceEmoji(key);
            Matcher matcher = userReference.matcher(key);
            final Matcher botCommandsMatcher = botCommandsParser.matcher(key);
            final Matcher urlMatcher = Patterns.WEB_URL.matcher(key);
            Spannable s;
            if (parsed instanceof Spannable) {
                s = (Spannable) parsed;
            } else {
                s = Spannable.Factory.getInstance().newSpannable(parsed);
            }

            while (matcher.find()) {
                s.setSpan(new ReferenceSpan(matcher.group(), userId, TYPE_USER_NAME), matcher.start(), matcher.end(), 0);
            }

            while (botCommandsMatcher.find()) {
                final int start = botCommandsMatcher.start(2);
                final int end = botCommandsMatcher.end(2);
                s.setSpan(new ReferenceSpan(botCommandsMatcher.group(), userId, TYPE_BOT_COMMAND),
                        start, end, 0);
            }

            while (urlMatcher.find()) {
                final String url = urlMatcher.group();
                s.setSpan(new ReferenceSpan(url, userId, TYPE_URL),
                        urlMatcher.start(), urlMatcher.end(), 0);
            }

            cache.put(key, s);
            return s;
        }
    }

    public PublishSubject<ReferenceSpan> getClickedSpans() {
        return clickedSpans;
    }

    public static final int TYPE_USER_NAME = 0;
    public static final int TYPE_BOT_COMMAND = 1;
    public static final int TYPE_URL = 2;

    public class ReferenceSpan extends ForegroundColorSpan {
        public final String reference;
        public final int userId;
        public final int type;

        public ReferenceSpan(String reference, int userId, int type) {
            super(0xff427ab0);
            this.reference = reference;
            this.userId = userId;
            this.type = type;
        }

        @Override
        public String toString() {
            return "ReferenceSpan{" +
                    "reference='" + reference + '\'' +
                    ", userId=" + userId +
                    ", type=" + type +
                    '}';
        }
    }

    final ThreadLocal<MessageDateFormatter> formatter = new ThreadLocal<MessageDateFormatter>() {
        @Override
        protected MessageDateFormatter initialValue() {
            return new MessageDateFormatter();
        }
    };

    class MessageDateFormatter {
        @Nullable final SimpleDateFormat fuckRuFormatter;
        @Nullable final DateTimeFormatter MESSAGE_TIME_FORMAT;

        public MessageDateFormatter() {
            if (Locale.getDefault().getCountry().equals("RU")) {
                fuckRuFormatter = new SimpleDateFormat("kk:mm", Locale.US);
                MESSAGE_TIME_FORMAT = null;
            } else {
                fuckRuFormatter = null;
                MESSAGE_TIME_FORMAT = DateTimeFormat.forPattern("K:mm a");
                ;
            }
        }

        public String formatDate(TdApi.Message msg) {
            long timeInMillis = Utils.dateToMillis(msg.date);
            long local = DateTimeZone.UTC.convertUTCToLocal(timeInMillis);
            if (fuckRuFormatter != null) {
                return fuckRuFormatter.format(local);
            }
            if (MESSAGE_TIME_FORMAT != null) {
                return MESSAGE_TIME_FORMAT.print(local);
            }
            return "";
        }
    }

    //    public static class BotCommand {
    //        public final String cmd;
    //        public final int userId;
    //
    //        BotCommand(String cmd, int userId) {
    //            this.cmd = cmd;
    //            this.userId = userId;
    //        }
    //    }
}
