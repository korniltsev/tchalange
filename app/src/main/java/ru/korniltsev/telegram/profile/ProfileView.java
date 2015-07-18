package ru.korniltsev.telegram.profile;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.profile.decorators.BottomShadow;
import ru.korniltsev.telegram.profile.decorators.DividerItemDecorator;
import ru.korniltsev.telegram.profile.decorators.MyWhiteRectTopPaddingDecorator;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.isEmpty;
import static ru.korniltsev.telegram.common.AppUtils.call;
import static ru.korniltsev.telegram.common.AppUtils.copy;
import static ru.korniltsev.telegram.common.AppUtils.phoneNumberWithPlus;
import static ru.korniltsev.telegram.common.AppUtils.uiName;

public class ProfileView extends FrameLayout implements HandlesBack {
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
        adapter.addFirst(new ProfileAdapter.Item(0, "", "", null));//header
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
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void bindUser(@NonNull TdApi.User user) {
        fakeToolbar.bindUser(user);
        List<ProfileAdapter.Item> items = new ArrayList<>();
        final boolean hasUserName = !isEmpty(user.username);
        final boolean hasPhoneNumber = !isEmpty(user.phoneNumber);
        if (hasUserName) {
            items.add(new ProfileAdapter.Item(
                    0,
                    "@" + user.username,
                    getContext().getString(R.string.item_type_username),
                    null));
        }

        if (hasPhoneNumber) {
            final String phone = phoneFormat.format(
                    phoneNumberWithPlus(user));
            items.add(new ProfileAdapter.Item(
                    R.drawable.phone_grey,
                    phone,
                    getContext().getString(R.string.item_type_mobile),
                    createPhoneActions(phone)));
        }

        adapter.addAll(items);
        if (hasPhoneNumber || hasUserName){
            list.addItemDecoration(new MyWhiteRectTopPaddingDecorator(1, calc.dp(16)));
            if (hasPhoneNumber && hasUserName) {
                list.addItemDecoration(new DividerItemDecorator(calc.dp(72), 0xffe5e5e5, 1));
                list.addItemDecoration(new BottomShadow(getContext(), calc, 2));
            } else {
                list.addItemDecoration(new BottomShadow(getContext(), calc, 1));
            }
        }
    }

    private List<ListChoicePopup.Item> createPhoneActions(final String phone) {

        final ArrayList<ListChoicePopup.Item> data = new ArrayList<>();
        data.add(new ListChoicePopup.Item(getContext().getString(R.string.call_phone), new Runnable() {
            @Override
            public void run() {
                call(getContext(), phone);
            }
        }));
        data.add(new ListChoicePopup.Item(getContext().getString(R.string.copy_phone), new Runnable() {
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
}
