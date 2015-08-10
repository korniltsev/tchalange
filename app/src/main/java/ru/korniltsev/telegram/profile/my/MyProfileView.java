package ru.korniltsev.telegram.profile.my;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.widget.FrameLayout;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.attach_panel.AttachPanelPopup;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.attach_panel.RecentImagesBottomSheet;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.profile.decorators.BottomShadow;
import ru.korniltsev.telegram.profile.decorators.DividerItemDecorator;
import ru.korniltsev.telegram.profile.decorators.InsetDecorator;
import ru.korniltsev.telegram.profile.decorators.MyWhiteRectTopPaddingDecorator;
import ru.korniltsev.telegram.profile.decorators.TopShadow;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.isEmpty;
import static ru.korniltsev.telegram.common.AppUtils.call;
import static ru.korniltsev.telegram.common.AppUtils.copy;
import static ru.korniltsev.telegram.common.AppUtils.getTmpFileForCamera;
import static ru.korniltsev.telegram.common.AppUtils.phoneNumberWithPlus;

public class MyProfileView extends FrameLayout implements HandlesBack {
    @Inject MyProfilePresenter presenter;
    @Inject ActivityOwner activity;
    @Inject DpCalculator calc;
    @Inject PhoneFormat phoneFormat;

    private RecyclerView list;
    private LinearLayoutManager listLayout;
    private FakeToolbar fakeToolbar;
    private MyProfileAdapter adapter;
    private ToolbarUtils toolbar;
    private AttachPanelPopup selectImage;

    public MyProfileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter = new MyProfileAdapter(getContext(), new Runnable() {
            @Override
            public void run() {

                presenter.passcodeClicked();
            }
        });
        adapter.addFirst(new MyProfileAdapter.KeyValueItem(0, "", "", null));
        listLayout = new LinearLayoutManager(getContext());
        list = ((RecyclerView) findViewById(R.id.list));
        list.setLayoutManager(listLayout);
        list.setAdapter(adapter);

        toolbar = ToolbarUtils.initToolbar(this)
                .inflate(R.menu.profile_my)
                .pop();
        toolbar.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() ==R.id.menu_logout){
                    presenter.logout();
                    return true;
                } else if (item.getItemId() == R.id.menu_edit_name){
                    presenter.editName();
                    return true;
                }
                return false;
            }
        });
        fakeToolbar = (FakeToolbar) findViewById(R.id.fake_toolbar);
        fakeToolbar.bindFAB(R.drawable.ic_camera, new Runnable() {
            @Override
            public void run() {
                selectImage = RecentImagesBottomSheet.create(activity.expose(), presenter, false);
            }
        });

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

    public void bindUser(@NonNull TdApi.User user, boolean passcodeEnabled) {
        bindUserAvatar(user);
        List<MyProfileAdapter.Item> items = new ArrayList<>();
        final boolean hasUserName = !isEmpty(user.username);
        if (hasUserName) {
            items.add(new MyProfileAdapter.KeyValueItem(
                    R.drawable.ic_user,
                    "@" + user.username,
                    getContext().getString(R.string.item_type_username),
                    null));
        }

        final String phone = phoneFormat.format(
                phoneNumberWithPlus(user));
        items.add(new MyProfileAdapter.KeyValueItem(
                R.drawable.phone_grey,
                phone,
                getContext().getString(R.string.item_type_mobile),
                createPhoneActions(phone)));

        items.add(new MyProfileAdapter.PasscodeItem(passcodeEnabled));

        adapter.addAll(items);

        list.addItemDecoration(new MyWhiteRectTopPaddingDecorator(1, calc.dp(15)));
        int passCodePosition;
        if (hasUserName) {
            list.addItemDecoration(new DividerItemDecorator(calc.dp(72), 0xffe5e5e5, 1));
            list.addItemDecoration(new BottomShadow(getContext(), calc, 2));

            passCodePosition = 3;
        } else {
            list.addItemDecoration(new BottomShadow(getContext(), calc, 1));
            passCodePosition = 2;
        }

        list.addItemDecoration(new TopShadow(getContext(), calc, passCodePosition));
        list.addItemDecoration(new BottomShadow(getContext(), calc, passCodePosition));
        list.addItemDecoration(new InsetDecorator(passCodePosition, calc.dp(6)));
    }

    public void bindUserAvatar(@NonNull TdApi.User user) {
        fakeToolbar.bindUser(user);
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
        if (selectImage != null && selectImage.isShowing()) {
            selectImage.dismiss();
            return true;
        }
        selectImage = null;
        return false;
    }

    public void hideAttachPannel() {
        if (selectImage != null) {
            selectImage.dismiss();
            selectImage = null;
        }
    }
}
