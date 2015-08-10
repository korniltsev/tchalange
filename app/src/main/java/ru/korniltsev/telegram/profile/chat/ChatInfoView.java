package ru.korniltsev.telegram.profile.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import flow.Flow;
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
import ru.korniltsev.telegram.photoview.PhotoView;
import ru.korniltsev.telegram.profile.decorators.BottomShadow;
import ru.korniltsev.telegram.profile.decorators.InsetDecorator;
import ru.korniltsev.telegram.profile.decorators.MyWhiteRectTopPaddingDecorator;
import ru.korniltsev.telegram.profile.decorators.TopShadow;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.drinkless.td.libcore.telegram.TdApi.File.NO_FILE_ID;

public class ChatInfoView extends FrameLayout implements HandlesBack {
    @Inject ChatInfoPresenter presenter;
    @Inject DpCalculator calc;
    @Inject PhoneFormat phoneFormat;
    @Inject ActivityOwner owner;

    private RecyclerView list;
    private LinearLayoutManager listLayout;
    private FakeToolbar fakeToolbar;
    private ChatInfoAdapter adapter;
    private ToolbarUtils toolbar;
    @Nullable private ListChoicePopup mutePopup;

    public ChatInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        adapter = new ChatInfoAdapter(getContext(), presenter);
        adapter.addFirst(new ChatInfoAdapter.HeaderItem());
        listLayout = new LinearLayoutManager(getContext());
        list = ((RecyclerView) findViewById(R.id.list));
        list.setLayoutManager(listLayout);
        list.setAdapter(adapter);

        toolbar = ToolbarUtils.initToolbar(this)
                .inflate(R.menu.chat_info)
                .pop();
        toolbar.setMenuClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int itemId = item.getItemId();
                switch (itemId) {
                    case R.id.menu_mute:
                        changeNotificationSettings();
                        return true;
                    case R.id.menu_add_member:
                        presenter.btnAddMemberClicked();
                        return true;
                    case R.id.menu_delete_and_leave:
                        presenter.deleteAndLeave();
                        return true;
                    case R.id.menu_edit_name:
                        presenter.editChatName();
                        return true;
                    default:
                        return false;
                }
            }
        });
        fakeToolbar = (FakeToolbar) findViewById(R.id.fake_toolbar);
        fakeToolbar.bindFAB(R.drawable.ic_camera, new Runnable() {
            @Override
            public void run() {
                presenter.changePhoto();
            }
        });

        fakeToolbar.image.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoView();
            }
        });
        owner.setStatusBarColor(getResources().getColor(R.color.primary_dark));
    }

    private void showPhotoView() {
        final TdApi.TLObject boundObject = fakeToolbar.image.boundObject;
        if (!(boundObject instanceof TdApi.Chat)) {
            return;
        }
        final TdApi.Chat u = (TdApi.Chat) boundObject;
        final TdApi.GroupChat groupChat = ((TdApi.GroupChatInfo) u.type).groupChat;
        if (groupChat.photo.big.id == NO_FILE_ID){
            return;
        }
        Flow.get(getContext())
                .set(new PhotoView(groupChat.photo));
    }

    private void changeNotificationSettings() {
        mutePopup = MuteForPopupFactory.create(owner.expose(), new MuteForPopupFactory.Callback() {
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
        if (mutePopup != null){
            mutePopup.dismiss();
        }
    }

    public void bindUser(@NonNull ChatInfo chat) {
        fakeToolbar.bindChat(chat);
        List<ChatInfoAdapter.Item> data = new ArrayList<>();
        data.add(new ChatInfoAdapter.ButtonItem());

        TdApi.ChatParticipant[] participants = chat.chatFull.participants;
        for (int i = 0, participantsLength = participants.length; i < participantsLength; i++) {
            TdApi.ChatParticipant participant = participants[i];
            data.add(new ChatInfoAdapter.ParticipantItem(i == 0, participant.user));
        }
        for (TdApi.User it : chat.addedUsers) {
            data.add(new ChatInfoAdapter.ParticipantItem(false, it));
        }
        adapter.addAll(data);

        final Context ctx = getContext();
        list.addItemDecoration(new MyWhiteRectTopPaddingDecorator(1, calc.dp(15)));
        list.addItemDecoration(new BottomShadow(ctx, calc, 1));

        if (participants.length >0){
            list.addItemDecoration(new InsetDecorator(2, calc.dp(6)));
            list.addItemDecoration(new TopShadow(ctx, calc, 2));//todo fix when shared media added



            list.addItemDecoration(new BottomShadow(ctx, calc, adapter.getItemCount() -1 ));
        }

    }

    public void bindMuteMenu(boolean muted) {
        final MenuItem muteMenu = toolbar.toolbar.getMenu().findItem(R.id.menu_mute);
        muteMenu.setIcon(muted ? R.drawable.ic_notifications_off : R.drawable.ic_notifications_on);
        muteMenu.setTitle(muted ? R.string.unmute : R.string.mute);
    }

    @Override
    public boolean onBackPressed() {
        if (mutePopup != null && mutePopup.isShowing()){
            mutePopup.dismiss();
            mutePopup = null;
            return true;
        }
        mutePopup = null;
        return false;
    }
}
