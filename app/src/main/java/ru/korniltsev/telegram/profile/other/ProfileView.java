package ru.korniltsev.telegram.profile.other;

import android.content.Context;
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
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.MuteForPopupFactory;
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
    @Inject ActivityOwner activity;
    @Inject PhoneFormat phoneFormat;

    private RecyclerView list;
    private LinearLayoutManager listLayout;
    private FakeToolbar fakeToolbar;
    private ProfileAdapter adapter;
    private ToolbarUtils toolbar;
    private ListChoicePopup mutePopup;

    public ProfileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter = new ProfileAdapter(getContext(), presenter);
        adapter.addFirst(new ProfileAdapter.KeyValueItem(0, "", "", null));//header
        listLayout = new LinearLayoutManager(getContext());
        list = ((RecyclerView) findViewById(R.id.list));
        list.setLayoutManager(listLayout);
        list.setAdapter(adapter);

        toolbar = ToolbarUtils.initToolbar(this)
                .inflate(R.menu.profile)
                .setMenuClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.menu_mute_unmute:
                                mute();
                                return true;
                            case R.id.menu_share:
                                presenter.share();
                                return true;
                            case R.id.menu_block:
                                presenter.block();
                                return true;
                            case R.id.menu_edit:
                                presenter.edit();
                                return true;
                            case R.id.menu_delete:
                                presenter.delete();
                                return true;
                        }
                        return false;
                    }
                })
                .pop();
        fakeToolbar = (FakeToolbar) findViewById(R.id.fake_toolbar);
        fakeToolbar.bindFAB(R.drawable.ic_message, new Runnable() {
            @Override
            public void run() {
                presenter.startChat();
            }
        });
    }

    private void mute() {
        mutePopup = MuteForPopupFactory.create(activity.expose(), new MuteForPopupFactory.Callback() {
            @Override
            public void muteFor(int duration) {
                presenter.muteFor(duration);
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

    public void bindUser(@NonNull TdApi.UserFull userFill) {
        final TdApi.User user = userFill.user;
        fakeToolbar.bindUser(user);
        List<ProfileAdapter.Item> items = new ArrayList<>();
        final boolean hasUserName = !isEmpty(user.username);
        final boolean hasPhoneNumber = !isEmpty(user.phoneNumber);
        int firstSectionCount = 0;
        if (hasUserName) {
            firstSectionCount++;
            items.add(new ProfileAdapter.KeyValueItem(
                    R.drawable.ic_user,
                    "@" + user.username,
                    getContext().getString(R.string.item_type_username),
                    null));
        }

        if (hasPhoneNumber) {
            firstSectionCount++;
            final String phone = phoneFormat.format(
                    phoneNumberWithPlus(user));
            items.add(new ProfileAdapter.KeyValueItem(
                    R.drawable.phone_grey,
                    phone,
                    getContext().getString(R.string.item_type_mobile),
                    createPhoneActions(phone)));
        }
        List<Integer> singleSections = new ArrayList<>();
        if (userFill.botInfo instanceof TdApi.BotInfoGeneral){
            firstSectionCount++;
            final TdApi.BotInfoGeneral botInfo = (TdApi.BotInfoGeneral) userFill.botInfo;
            items.add(new ProfileAdapter.KeyValueItem(
                    R.drawable.ic_about,
                    botInfo.shareText,
                    getContext().getString(R.string.bot_about),
                    null
            ));

            items.add(new ProfileAdapter.ButtonItem(
                    R.drawable.ic_add,
                    getContext().getString(R.string.bot_add_to_group),
                    new Runnable() {
                        @Override
                        public void run() {
                            presenter.addBotToGroup();
                        }
                    }
            ));
            singleSections.add(items.size());
        }


        adapter.addAll(items);
        int itemsBeforeFirstSection = 1;//blue header
        if (firstSectionCount >0){
            list.addItemDecoration(new MyWhiteRectTopPaddingDecorator(1, calc.dp(15)));
            if (firstSectionCount > 1){
                for (int i = 0; i < firstSectionCount; i++) {
                    int pos = i + itemsBeforeFirstSection;
                    list.addItemDecoration(new DividerItemDecorator(calc.dp(72), 0xffe5e5e5, pos));
                }
            }
            list.addItemDecoration(new BottomShadow(getContext(), calc, firstSectionCount));
        }

        Context ctx = getContext();
        for (Integer integer : singleSections) {
            list.addItemDecoration(new InsetDecorator(integer, calc.dp(6)));
            list.addItemDecoration(new TopShadow(ctx, calc, integer));
            list.addItemDecoration(new BottomShadow(ctx, calc, integer));
        }
    }

    private List<ListChoicePopup.Item> crateteAddBotToGroupAction() {
        final ArrayList<ListChoicePopup.Item> items = new ArrayList<>();
        items.add(new ListChoicePopup.Item("unused", new Runnable() {
            @Override
            public void run() {
                presenter.addBotToGroup();
            }
        }));
        return items;
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
        if (mutePopup != null && mutePopup.isShowing()){
            mutePopup.dismiss();
            mutePopup = null;
            return true;
        }
        mutePopup = null;
        return presenter.hidePopup();
    }

    public void bindMuteMenu(boolean muted) {
        final MenuItem item = toolbar.toolbar.getMenu().findItem(R.id.menu_mute_unmute);
        item.setIcon(muted ? R.drawable.ic_notifications_off : R.drawable.ic_notifications_on);
        item.setTitle(muted ? R.string.unmute : R.string.mute);
    }

    public void bindBlockMenu(boolean blocked) {
        final MenuItem block = toolbar.toolbar.getMenu().findItem(R.id.menu_block);
        block.setTitle(blocked ? R.string.menu_unblock : R.string.menu_block);
    }
}
