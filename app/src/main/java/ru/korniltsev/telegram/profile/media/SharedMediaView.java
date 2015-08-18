package ru.korniltsev.telegram.profile.media;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import flow.Flow;
import junit.framework.Assert;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.audio.LinearLayoutWithShadow;
import ru.korniltsev.telegram.audio.MiniPlayerView;
import ru.korniltsev.telegram.audio.MiniPlayerViewFactory;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.profile.media.controllers.SharedMediaController;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;

public class SharedMediaView extends LinearLayoutWithShadow implements HandlesBack{
    public static final int IC_BACK = ru.korniltsev.telegram.utils.R.drawable.abc_ic_ab_back_mtrl_am_alpha;
    @Inject SharedMediaPresenter presenter;
    private ToolbarUtils toolbarUtils;
    private RecyclerView list;
    private DpCalculator dpCalculator;
    private DropdownPopup popup;
    private TextView customView;
    private SharedMediaController mediaController;

    public SharedMediaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        dpCalculator = MyApp.from(this).dpCalculator;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbarUtils = ToolbarUtils.initToolbar(this)
                .customView(R.layout.shared_media_custom_view)
                .pop();
        final Drawable d = getResources().getDrawable(IC_BACK);
        d.setColorFilter(0xff818181, PorterDuff.Mode.MULTIPLY);
        toolbarUtils.toolbar.setNavigationIcon(d);

        list = ((RecyclerView) findViewById(R.id.list));

        final int dp56 = dpCalculator.dp(56f);
        setShadowOffset(dp56);
        customView = (TextView) toolbarUtils.getCustomView();
        Assert.assertNotNull(customView);
        customView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDropdown(customView);
            }
        });

        final MiniPlayerView player = MiniPlayerViewFactory.construct(getContext(), dpCalculator);
        addView(player, 1);
        player.setShadow(this);
    }

    private void showDropdown(View customView) {
        List<DropdownPopup.Item> items = new ArrayList<>();
        final Resources res = getContext().getResources();
        final String sharedMedia = res.getString(R.string.shared_media_title);
        items.add(new DropdownPopup.Item(sharedMedia, new Runnable() {
            @Override
            public void run() {
                toggle(SharedMediaPath.TYPE_MEDIA);
            }
        }));

        items.add(new DropdownPopup.Item(res.getString(R.string.audio_files), new Runnable() {
            @Override
            public void run() {
                toggle(SharedMediaPath.TYPE_AUDIO);
            }
        }));
        popup = new DropdownPopup(getContext(), items);
        final int[] location = new int[2];
        customView.getLocationOnScreen(location);

        popup.showAtLocation(customView, 0, dpCalculator.dp(48), dpCalculator.dp(28));
    }

    private void toggle(int typeMedia) {
        if (presenter.path.type == typeMedia){
            return;
        }
        replace(typeMedia);
    }

    private void replace(int typeMedia) {
        AppUtils.flowPushAndRemove(this, new SharedMediaPath(presenter.path.chatId, typeMedia), new FlowHistoryStripper() {
            @Override
            public boolean shouldRemovePath(Object path) {
                return path instanceof SharedMediaPath;
            }
        }, Flow.Direction.REPLACE);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);

    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
        dismisPopup();
        mediaController.drop();
    }

    @Override
    public boolean onBackPressed() {
        dismisPopup();
        return false;
    }

    private void dismisPopup() {
        if (popup != null && popup.isShowing()){
            popup.dismiss();
        }
    }

    public void bind(SharedMediaPath path) {
        if (path.type == SharedMediaPath.TYPE_MEDIA) {
            mediaController = new SharedMediaController(list, customView, presenter.path);
        } else {
            mediaController = new SharedMediaController(list, customView, presenter.path);
        }
    }
}
