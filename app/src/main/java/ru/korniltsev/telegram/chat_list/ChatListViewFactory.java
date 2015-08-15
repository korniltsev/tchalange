package ru.korniltsev.telegram.chat_list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.korniltsev.telegram.audio.LinearLayoutWithShadow;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat_list.view.DrawerButtonView;
import ru.korniltsev.telegram.chat_list.view.MyPhoneView;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.views.AvatarView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class ChatListViewFactory {
    @NonNull
    static View construct(Context ctx) {
        final MyApp from = MyApp.from(ctx);
        final DpCalculator calc = from.dpCalculator;
        final ChatListView clv = new ChatListView(ctx, null);
        clv.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        clv.setBackgroundColor(Color.WHITE);

        LinearLayout listRoot = createListRoot(ctx, calc);
        clv.addView(listRoot);

        LinearLayout menu = createMenu(ctx, calc, from);
        clv.addView(menu);

        clv.onFinishInflate();
        return clv;
    }

    static LinearLayout createListRoot(Context ctx, DpCalculator calc) {
        final LinearLayoutWithShadow result = new LinearLayoutWithShadow(ctx, null);
        result.setId(R.id.toolbar_shadow);
        result.setLayoutParams(new DrawerLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        result.setOrientation(LinearLayout.VERTICAL);

        final Toolbar toolbar = new Toolbar(ctx);
        toolbar.setId(R.id.toolbar);
        toolbar.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, calc.dp(56f)));
        toolbar.setPadding(0, 0, 0, calc.dp(4f));
        toolbar.setBackgroundColor(0xFF5D96C0);
        result.addView(toolbar);
        toolbar.setPopupTheme(R.style.Theme_AppCompat_Light);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
//        toolbar.setTitleTextAppearance();


//        final MiniPlayerView miniPlayer = MiniPlayerViewFactory.construct(ctx, calc);
//        result.addView(miniPlayer);

        final RecyclerView list = new RecyclerView(ctx);
        list.setId(R.id.list);
        final LinearLayout.LayoutParams listLP = new LinearLayout.LayoutParams(MATCH_PARENT, 0);
        listLP.weight = 1;
        list.setLayoutParams(listLP);
        result.addView(list);
        return result;





    }

    static LinearLayout createMenu(Context ctx, DpCalculator calc, MyApp from) {

        final LinearLayout menu = new LinearLayout(ctx);
        menu.setBackgroundColor(Color.WHITE);
        final DrawerLayout.LayoutParams menuLp = new DrawerLayout.LayoutParams(calc.dp(304f), MATCH_PARENT);
        menuLp.gravity = Gravity.START;
        menu.setLayoutParams(menuLp);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setClickable(true);

        final LinearLayout blueHeader = createBlueHeader(ctx, calc, from);
        menu.addView(blueHeader);
        final LinearLayout buttons = createMenuButtons(ctx, calc);
        menu.addView(buttons);

        return menu;

    }

    static LinearLayout createMenuButtons(Context ctx, DpCalculator calc) {

        final LinearLayout result = new LinearLayout(ctx);
        final LinearLayout.LayoutParams resultLp = new LinearLayout.LayoutParams(MATCH_PARENT, 0);
        resultLp.weight = 1f;
        result.setLayoutParams(resultLp);
        result.setBackgroundColor(Color.WHITE);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setPadding(0, calc.dp(4), 0, 0);

        createButton(ctx, calc, result, R.id.btn_contacts, R.string.contacts, R.drawable.menu_contacts);
        createButton(ctx, calc, result, R.id.btn_settings, R.string.settings, R.drawable.ic_settings);
        createButton(ctx, calc,result, R.id.btn_logout, R.string.logout, R.drawable.ic_logout);


        return result;
    }

    static void createButton(Context ctx, DpCalculator calc, LinearLayout result, int id, int text, int icon) {
        final Resources res = ctx.getResources();
        final int height = calc.dp(54);
        final DrawerButtonView btn = new DrawerButtonView(ctx, height, calc.dp(304), calc, res.getString(text), res.getDrawable(icon));
        //        final TextView textView = new TextView(ctx);
        btn.setId(id);
//        textView.setText(text);
        final Drawable d = res
                .getDrawable(icon);
        assert d != null;
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
//        textView.setCompoundDrawables(d, null, null, null);

        btn.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

//        textView.setCompoundDrawablePadding(dip16);
//        textView.setPadding(dip16, 0, 0, 0);
        btn.setBackgroundResource(R.drawable.bg_keyboard_tab);
//        textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
//        textView.setTextColor();
//        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, dip16);
        result.addView(btn);

    }

    static LinearLayout createBlueHeader(Context ctx, DpCalculator calc, MyApp from) {
        final int dip16 = calc.dp(16f);
        final int dip18 = calc.dp(18f);

        final LinearLayout blueHeader = new LinearLayout(ctx);
        blueHeader.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, calc.dp(148)));
        blueHeader.setBackgroundColor(0xFF5D96C0);
        blueHeader.setOrientation(LinearLayout.VERTICAL);

        final AvatarView avatarView = new AvatarView(ctx, calc.dp(68), from);
        avatarView.setId(R.id.drawer_avatar);
        final LinearLayout.LayoutParams avatarLP = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        avatarLP.leftMargin = calc.dp(16);
        avatarLP.topMargin = calc.dp(16);
        avatarView.setLayoutParams(avatarLP);
        blueHeader.addView(avatarView);


        final MyPhoneView drawerName = new MyPhoneView(ctx,  calc.dp(16f), true);
        drawerName.setId(R.id.drawer_name);
        final LinearLayout.LayoutParams drawerNameLP = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        drawerNameLP.leftMargin = dip18;
        drawerNameLP.rightMargin = dip18;
        drawerNameLP.topMargin = dip16;
        drawerName.setLayoutParams(drawerNameLP);
        blueHeader.addView(drawerName);

        final MyPhoneView myPhoneView = new MyPhoneView(ctx,  calc.dp(14f), false);
        myPhoneView.setId(R.id.drawer_phone);
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.leftMargin = dip18;
        myPhoneView.setLayoutParams(params);
        blueHeader.addView(myPhoneView);

        return blueHeader;
    }
}
