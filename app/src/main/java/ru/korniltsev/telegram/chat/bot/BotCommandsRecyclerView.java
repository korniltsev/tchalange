package ru.korniltsev.telegram.chat.bot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

import javax.inject.Inject;

public class BotCommandsRecyclerView extends RecyclerView {


    private final Drawable botShadow;

    public BotCommandsRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        botShadow = getResources().getDrawable(R.drawable.bot_shadow);


    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
        botShadow.setBounds(0, 0, getRight(), botShadow.getIntrinsicHeight());
        botShadow.draw(c);
    }
}
