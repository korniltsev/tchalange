package ru.korniltsev.telegram.profile.media;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

import java.util.List;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class DropdownPopup extends PopupWindow {
    final List<Item> items;

    public DropdownPopup(Context ctx, List<Item> items) {
        super();
        this.items = items;
        final DpCalculator dpCalculator = MyApp.from(ctx).dpCalculator;
        final LinearLayout linearLayout = new LinearLayout(ctx);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int height = 0;
        final int buttonHeight = dpCalculator.dp(56f);
        final int dip16 = dpCalculator.dp(16f);
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
            linearLayout.addView(textView, MATCH_PARENT, buttonHeight);
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            textView.setTextColor(0xff333333);
            textView.setPadding(dip16, 0, dip16, 0);
            height += buttonHeight;
        }
        linearLayout.setBackgroundResource(R.drawable.bg_popup);
        setContentView(linearLayout);
        final int widthSpec = makeMeasureSpec(dpCalculator.dp(200), EXACTLY);
        final int heightSpec = makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        linearLayout.measure(widthSpec, heightSpec);
        setOutsideTouchable(true);
        setWidth(widthSpec);
        setHeight(
                makeMeasureSpec(linearLayout.getMeasuredHeight(), EXACTLY));
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    static class Item {
        final String title;
        final Runnable action;

        Item(String title, Runnable action) {
            this.title = title;
            this.action = action;
        }
    }
}
