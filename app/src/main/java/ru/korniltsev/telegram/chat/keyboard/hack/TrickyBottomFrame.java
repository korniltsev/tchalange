package ru.korniltsev.telegram.chat.keyboard.hack;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import ru.korniltsev.telegram.emoji.ObservableLinearLayout;

public class TrickyBottomFrame extends FrameLayout {

    private ObservableLinearLayout parent;

    public TrickyBottomFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }



    public void init(ObservableLinearLayout l){
        this.parent = l;
    }


    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (parent.getKeyboardHeight() == 0){
            super.dispatchDraw(canvas);
        }
    }

}
