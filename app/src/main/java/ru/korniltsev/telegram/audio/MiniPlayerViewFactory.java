package ru.korniltsev.telegram.audio;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class MiniPlayerViewFactory {
    public static MiniPlayerView construct(Context ctx, DpCalculator calc) {
        final MiniPlayerView result = new MiniPlayerView(ctx, null);
        result.setId(R.id.mini_player);
        result.setLayoutParams(
                new LinearLayout.LayoutParams(MATCH_PARENT, calc.dp(35f)));
        result.setOrientation(LinearLayout.HORIZONTAL);


        final ImageButton btnLeft = new ImageButton(ctx);
        btnLeft.setLayoutParams(new LinearLayout.LayoutParams(calc.dp(61f), MATCH_PARENT));
        btnLeft.setId(R.id.btn_play);
        btnLeft.setBackgroundResource(R.drawable.bg_keyboard_tab);
        result.addView(btnLeft);

        final TextView songName = new TextView(ctx);
        songName.setId(R.id.text);
        final LinearLayout.LayoutParams songNameLP = new LinearLayout.LayoutParams(0, MATCH_PARENT);
        songNameLP.weight = 1f;
        songName.setLayoutParams(songNameLP);
        songName.setEllipsize(TextUtils.TruncateAt.END);
        songName.setSingleLine();
        songName.setGravity(Gravity.CENTER_VERTICAL);
        songName.setTextColor(0xFF333333);
        result.addView(songName);

        final ImageButton btnRight = new ImageButton(ctx);
        btnRight.setLayoutParams(new LinearLayout.LayoutParams(calc.dp(61f), MATCH_PARENT));
        btnRight.setId(R.id.btn_stop);
        btnRight.setBackgroundResource(R.drawable.bg_keyboard_tab);
        btnRight.setImageResource(R.drawable.ic_closeplayer);
        result.addView(btnRight);


        result.onFinishInflate();
        return result;
    }
}
