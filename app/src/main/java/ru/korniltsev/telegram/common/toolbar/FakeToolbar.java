package ru.korniltsev.telegram.common.toolbar;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.profile.chat.ChatInfo;

import javax.inject.Inject;

import static ru.korniltsev.telegram.common.AppUtils.uiName;
import static ru.korniltsev.telegram.common.AppUtils.uiUserStatus;

// фейковая панелька для отрисовки схлопывающегося тулбара
// показывает аватарку, заголовок и подзаголовок
// схлопывает при вьюшки при скроле
//
// createScrollListener создает слушателя скрола
// initPosition позиционирует вьюхи первоначально. затем это делает созданый ранее scrollListener
// todo рисовать не большую теньку в замен elevation
// todo выпилить обычный тулбар поскольку он походу ничего толком не делает
public class FakeToolbar extends FrameLayout {
    @Inject DpCalculator calc;
    @Inject ChatDB chats;
    public AvatarView image;
    private TextView title;
    private TextView subTitle;
    private ViewGroup titleParent;
    private int headerHeight;
    private int toolbarHeight;
    private int realHeaderHeight;
    private View fab;
    private ImageView fabIcon;

    public FakeToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        image = ((AvatarView) findViewById(R.id.avatar));
        title = ((TextView) findViewById(R.id.title));
        subTitle = ((TextView) findViewById(R.id.subtitle));
        titleParent = ((ViewGroup) title.getParent());
        fab = findViewById(R.id.fab);
        fabIcon = ((ImageView) findViewById(R.id.fab_icon));

        headerHeight = getLayoutParams().height;
        realHeaderHeight = getResources().getDimensionPixelSize(R.dimen.profile_header_height);
        //        fakeToolbarHeight = getResources().getDP

//        image.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
    }

    private void positionAvatar(float res) {
        int dp4 = calc.dp(4);
        int initialAvatarSize = image.getSize();
        int targetSize = dp4 * 10;
        int fullDiff = targetSize - initialAvatarSize;
        float diffRes = res * fullDiff;
        float scaleResult = 1 + diffRes / initialAvatarSize;
        image.setScaleX(scaleResult);
        image.setScaleY(scaleResult);

        int dp16 = dp4 * 4;
        int avatarMargin = dp16;
        int avatarMargin2 = dp16 + calc.dp(27);
        int ty = headerHeight - initialAvatarSize / 2 - toolbarHeight / 2 - avatarMargin2;
        image.setTranslationY(-avatarMargin2 - ty * res);

        image.setTranslationX(calc.dp(17) + res * dp4 * 7);

        //text

        //width
        int maxWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        int initialTextX = initialAvatarSize + 2 * avatarMargin;
        int initialWidth = maxWidth - initialTextX;
        int targetWidth = initialWidth - dp4 * 24;
        int diff = initialWidth - targetWidth;
        int finalWidth = (int) (initialWidth - res * diff);
        final ViewGroup.LayoutParams lp = title.getLayoutParams();
        if (lp.width != finalWidth) {//todo round!
            lp.width = finalWidth;
            title.setLayoutParams(lp);
        }

        //text ypos
        final int titleParentHeight = titleParent.getLayoutParams().height;
        final int initialTranslationY = -avatarMargin2 + (initialAvatarSize - titleParentHeight) / 2;
        final int targetTranslationY = -(headerHeight - titleParentHeight);
        final int diff2 = targetTranslationY - initialTranslationY;
        final int absDiff2 = (int) (diff2 * res);
        titleParent.setTranslationY(initialTranslationY + absDiff2);
        //text xpos
        final int targetTextX = initialTextX + calc.dp(12);
        final int diff3 = targetTextX - initialTextX;

        titleParent.setTranslationX(initialTextX + res * diff3);

        //fab

        int targetY = -(realHeaderHeight - toolbarHeight);
        fab.setTranslationY(res * targetY);
        if (res >= 0.65 && fabVisible) {
            fabVisible = false;
            fab.clearAnimation();
            fab.animate()
                    .alpha(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(128);
        }
        if (res < 0.65 && !fabVisible) {
            fabVisible = true;
            fab.clearAnimation();
            fab.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(128);
        }
    }

    boolean fabVisible = true;

    public RecyclerView.OnScrollListener createScrollListener(LinearLayoutManager layout, RecyclerView list) {
        return new MyOnScrollListener(layout, list);
    }

    public void initPosition(Toolbar toolbar) {
        toolbarHeight = toolbar.getLayoutParams().height;
        positionAvatar(0f);
    }

    public void bindUser(TdApi.User user) {
        title.setText(
                uiName(user, getContext()));

        image.loadAvatarFor(user);
        if (user.type instanceof TdApi.UserTypeBot) {
            subTitle.setText(R.string.user_status_bot);
        } else {
            subTitle.setText(
                    uiUserStatus(getContext(), chats.getUserStatus(user)));
        }
    }

    public void bindChat(ChatInfo chat) {
        title.setText(chat.chatFull.groupChat.title);
        image.loadAvatarFor(chat.chat);

        int online = 0;
        for (TdApi.ChatParticipant p : chat.chatFull.participants) {
            if (p.user.status instanceof TdApi.UserStatusOnline) {
                online++;
            }
        }
        Resources res = getResources();
        String totalStr = res.getQuantityString(R.plurals.group_chat_members, chat.chatFull.groupChat.participantsCount, chat.chatFull.groupChat.participantsCount);
        String onlineStr = res.getQuantityString(R.plurals.group_chat_members_online, online, online);
        subTitle.setText(
                totalStr + ", " + onlineStr);
        //        subTitle.setText();
    }

    public void bindFAB(int icon, final Runnable runnable) {
        fabIcon.setImageResource(icon);
        fabIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fabVisible) {
                    runnable.run();
                }
            }
        });
    }

    private class MyOnScrollListener extends RecyclerView.OnScrollListener {
        final LinearLayoutManager listLayout;
        final RecyclerView list;

        public MyOnScrollListener(LinearLayoutManager listLayout, RecyclerView list) {
            this.listLayout = listLayout;
            this.list = list;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            final int pos = listLayout.findFirstVisibleItemPosition();
            float res;
            if (pos == 0) {
                final View childAt = list.getChildAt(0);
                final int bottom = childAt.getBottom();
                if (bottom <= toolbarHeight) {
                    res = 1f;
                } else {
                    res = 1f - (float) (bottom - toolbarHeight) / (realHeaderHeight - toolbarHeight);
                }
            } else {
                res = 1f;
            }
            positionAvatar(res);
        }
    }
}
