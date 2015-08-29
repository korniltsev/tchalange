package ru.korniltsev.telegram.core.app;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextUtils;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.adapter.view.TextMessageView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.StaticLayoutCache;
import ru.korniltsev.telegram.core.rx.UserHolder;

public class MessageLayoutGenerator implements ChatDB.PrepareMessageLayoutHook{
    final StaticLayoutCache cache;
    private final MyApp ctx;
    private final DpCalculator calc;
    private final UserHolder userHolder;

    public MessageLayoutGenerator(StaticLayoutCache cache, MyApp ctx, DpCalculator calc, UserHolder userHolder) {
        this.cache = cache;
        this.ctx = ctx;
        this.calc = calc;
        this.userHolder = userHolder;
    }

    @Override
    public void prepare(TdApi.Message msg, boolean initRequest, int messageNumber) {
        TdApi.User user = userHolder.getUser(msg.fromId);
        if (user.nullableUiName == null) {
            user.nullableUiName = AppUtils.uiName(user, ctx);
        }

        if (initRequest && messageNumber <= 12){
            CustomCeilLayout.initPaints(calc);
            final StaticLayout timeLayout = CustomCeilLayout.getStaticLayoutForTime(cache, msg);
            final int timeWidth = CustomCeilLayout.getTimeWidth(timeLayout);
            final int displayWidth = ctx.displayWidth;
            final int spaceLeftForNick = CustomCeilLayout.getSpaceLeftForNick(displayWidth, timeWidth);
            final CharSequence ellipsized = CustomCeilLayout.getEllipsizedNick(user.nullableUiName, spaceLeftForNick);
            CustomCeilLayout.getStaticLayoutForNick(spaceLeftForNick, ellipsized, cache);


            if (msg.message instanceof TdApi.MessageText) {
                TextMessageView.initPaints(calc);
                int messageWidth = TextMessageView.getTextWidth(displayWidth, calc);
                Spannable textForLayout;
                TdApi.MessageText text = (TdApi.MessageText) msg.message;
                if (text.textWithSmilesAndUserRefs != null) {
                    textForLayout = text.textWithSmilesAndUserRefs;
                } else if (text.text != null) {
                    textForLayout = new SpannableString(text.text);
                } else {
                    textForLayout = new SpannableString("");
                }
                TextMessageView.getLayoutForText(cache, messageWidth, textForLayout);
            }


        }
    }
}
