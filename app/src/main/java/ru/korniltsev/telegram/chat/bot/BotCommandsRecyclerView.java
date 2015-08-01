package ru.korniltsev.telegram.chat.bot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

import javax.inject.Inject;

public class BotCommandsRecyclerView extends RecyclerView {

    private final Drawable botShadow;
    private final ColorDrawable bg;

    public BotCommandsRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        botShadow = getResources().getDrawable(R.drawable.bot_shadow);

        bg = new ColorDrawable(Color.WHITE);
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        final int shadowHeight = botShadow.getIntrinsicHeight();
        botShadow.setBounds(0, 0, getRight(), shadowHeight);
        bg.setBounds(0, shadowHeight, getRight(), getBottom());
        botShadow.draw(c);
        bg.draw(c);
    }
}
