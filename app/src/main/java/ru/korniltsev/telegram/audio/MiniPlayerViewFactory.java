package ru.korniltsev.telegram.audio;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.korniltsev.telegram.audio.helper.SimpleImageButtonView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class MiniPlayerViewFactory {
    public static MiniPlayerView construct(Context ctx, DpCalculator calc) {




        final MiniPlayerView result = new MiniPlayerView(ctx);
        result.setId(R.id.mini_player);
        result.setLayoutParams(
                new LinearLayout.LayoutParams(MATCH_PARENT, calc.dp(35f)));



        result.onFinishInflate();
        return result;
    }
}
