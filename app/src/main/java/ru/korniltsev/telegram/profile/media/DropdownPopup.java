package ru.korniltsev.telegram.profile.media;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class DropdownPopup extends PopupWindow {
    final List<Item> items;

    public DropdownPopup(Context ctx, List<Item> items) {
        super();
        this.items = items;
        final DpCalculator calc = MyApp.from(ctx).calc;
        final LinearLayout linearLayout = new LinearLayout(ctx);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        final int buttonHeight = calc.dp(50f);
        final int dip16 = calc.dp(16f);
        List<TextView> buttons = new ArrayList<>();
        for (final Item item : items) {
            final TextView textView = new TextView(ctx);
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item.action.run();
                    dismiss();
                }
            });
            textView.setText(item.title);
            textView.setBackgroundResource(R.drawable.bg_keyboard_tab);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            linearLayout.addView(textView, MATCH_PARENT, buttonHeight);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            textView.setTextColor(0xff333333);
            textView.setPadding(dip16, 0, dip16, 0);
        }
        linearLayout.setBackgroundResource(R.drawable.bg_popup);
        setContentView(linearLayout);
//        final int widthSpec = makeMeasureSpec(dpCalculator.dp(200), EXACTLY);
        final int widthSpec = makeMeasureSpec(0, UNSPECIFIED);
        final int heightSpec = makeMeasureSpec(0, UNSPECIFIED);
        linearLayout.measure(widthSpec, heightSpec);
        setOutsideTouchable(true);
        final int w = Math.max(calc.dp(150), linearLayout.getMeasuredWidth());
        setWidth(
                makeMeasureSpec(w, EXACTLY));
        setHeight(
                makeMeasureSpec(linearLayout.getMeasuredHeight(), EXACTLY));
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public static class Item {
        final String title;
        final Runnable action;

        public Item(String title, Runnable action) {
            this.title = title;
            this.action = action;
        }
    }
}
