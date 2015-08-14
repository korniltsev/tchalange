package ru.korniltsev.telegram.core.rx;

import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.emoji.images.Emoji;
import rx.subjects.PublishSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
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

    private PublishSubject<BotCommand> clickedSpans = PublishSubject.create();


    public EmojiParser(Emoji emoji) {
        this.emoji = emoji;
    }

    public void parse(TdApi.Message msg) {
        if (msg.message instanceof TdApi.MessageText) {
            parseEmojis(msg);
        } else if (msg.message instanceof TdApi.MessagePhoto) {
            pareImageSizes((TdApi.MessagePhoto) msg.message);
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
            Spannable s;
            if (parsed instanceof Spannable) {
                s = (Spannable) parsed;
            } else {
                s = Spannable.Factory.getInstance().newSpannable(parsed);
            }

            while (matcher.find()) {
                s.setSpan(new ForegroundColorSpan(0xff427ab0), matcher.start(), matcher.end(), 0);
            }

            while (botCommandsMatcher.find()) {
                final int start = botCommandsMatcher.start(2);
                final int end = botCommandsMatcher.end(2);
                s.setSpan(new MyClickableSpan(userId, key, start, end), start, end, 0);
            }

            cache.put(key, s);
            return s;
        }
    }

    public PublishSubject<BotCommand> getClickedSpans() {
        return clickedSpans;
    }

    private class MyClickableSpan extends ClickableSpan {
        final int userId;
        private final String key;
        private final int start;
        private final int end;

        public MyClickableSpan(int userId, String key, int start, int end) {
            this.userId = userId;
            this.key = key;
            this.start = start;
            this.end = end;
        }

        @Override
        public void onClick(View widget) {
            final String cmd = key.substring(start, end);
            clickedSpans.onNext( new BotCommand(cmd, userId));
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }

    public static class BotCommand {
        public final String cmd;
        public final int userId;

        BotCommand(String cmd, int userId) {
            this.cmd = cmd;
            this.userId = userId;
        }
    }
}
