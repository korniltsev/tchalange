package ru.korniltsev.telegram.profile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static ru.korniltsev.telegram.common.AppUtils.call;
import static ru.korniltsev.telegram.common.AppUtils.copy;
import static ru.korniltsev.telegram.common.AppUtils.phoneNumberWithPlus;
import static ru.korniltsev.telegram.common.AppUtils.uiName;

public class ProfileView extends FrameLayout implements HandlesBack{
    @Inject ProfilePresenter presenter;
    @Inject DpCalculator calc;
    @Inject PhoneFormat phoneFormat;

    private RecyclerView list;
    private LinearLayoutManager listLayout;
    private FakeToolbar fakeToolbar;
    private ProfileAdapter adapter;
    private ToolbarUtils toolbar;

    public ProfileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter = new ProfileAdapter(getContext(), presenter);
        adapter.addFirst(new ProfileAdapter.Item(0,"", "", null));//header
        listLayout = new LinearLayoutManager(getContext());
        list = ((RecyclerView) findViewById(R.id.list));
        list.setLayoutManager(listLayout);
        list.setAdapter(adapter);

        toolbar = ToolbarUtils.initToolbar(this)
                .pop();
        fakeToolbar = (FakeToolbar) findViewById(R.id.fake_toolbar);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);


        list.addOnScrollListener(
                fakeToolbar.createScrollListener(listLayout, list));
        fakeToolbar.initPosition(
                toolbar.toolbar);

        offsetFirstItem();

    }

    private void offsetFirstItem() {
        //looks better
        list.addItemDecoration(new MyWhiteRectTopPaddingDecorator(1, calc.dp(16)));
        list.addItemDecoration(new DividerItemDecorator(calc.dp(72), 0xffe5e5e5, 1));
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }



    public void bindUser(@NonNull TdApi.User user) {
        fakeToolbar.bindUser(user);
        List<ProfileAdapter.Item> items = new ArrayList<>();
        if (!TextUtils.isEmpty(user.username)) {
            items.add(new ProfileAdapter.Item(
                    0,
                    "@" + user.username,
                    getContext().getString(R.string.item_type_username),
                    null));
        }
        if (!TextUtils.isEmpty(user.phoneNumber)) {
            final String phone = phoneFormat.format(
                    phoneNumberWithPlus(user));
            items.add(new ProfileAdapter.Item(
                    R.drawable.phone_grey,
                    phone,
                    getContext().getString(R.string.item_type_mobile),
                    createPhoneActions(phone)));
        }
        adapter.addAll(items);
    }

    private List<ListChoicePopup.Item> createPhoneActions(final String phone) {
        
        final ArrayList<ListChoicePopup.Item> data = new ArrayList<>();
        data.add(new ListChoicePopup.Item(getContext().getString(R.string.call_phone), new Runnable(){
            @Override
            public void run() {
                call(getContext(), phone);
            }
        }));
        data.add(new ListChoicePopup.Item(getContext().getString(R.string.copy_phone), new Runnable(){
            @Override
            public void run() {
                copy(getContext(), phone);
            }
        }));
        return data;
    }

    @Override
    public boolean onBackPressed() {
        return presenter.hidePopup();
    }

    public static  class DividerItemDecorator extends RecyclerView.ItemDecoration{
        final int paddingLeft;
        final int color;
        final int itemPosition;
        Paint paint ;
        public DividerItemDecorator(int paddingLeft, int color, int itemPosition) {
            this.paddingLeft = paddingLeft;
            this.color = color;
            this.itemPosition = itemPosition;
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(color);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

            AppUtils.clear(outRect);
            if (parent.getChildViewHolder(view).getAdapterPosition() == itemPosition){
                outRect.bottom = 1;
            }
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            final View targetView = AppUtils.getChildWithAdapterPosition(parent, itemPosition);
            if (targetView != null) {
                drawDivider(c, targetView);
            }
        }

        private void drawDivider(Canvas c, View child) {
            c.drawLine(child.getLeft() + paddingLeft, child.getBottom(), child.getRight(), child.getBottom(), paint);
        }
    }

    private static class MyWhiteRectTopPaddingDecorator extends RecyclerView.ItemDecoration {
        final int position;
        final int height;
        final Paint paint;

        public MyWhiteRectTopPaddingDecorator(int position, int height) {
            this.position = position;
            this.height = height;
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
        }

        @Override
        public void getItemOffsets(Rect outRect, View child, RecyclerView parent, RecyclerView.State state) {
            AppUtils.clear(outRect);
            if (parent.getChildViewHolder(child).getAdapterPosition() == position){
                outRect.top = height;
            }
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            final View targetView = AppUtils.getChildWithAdapterPosition(parent, position);
            if (targetView != null){
                c.drawRect(targetView.getLeft(), targetView.getTop() - height,
                        targetView.getRight(), targetView.getTop(),
                        paint);
            }
        }
    }
}
