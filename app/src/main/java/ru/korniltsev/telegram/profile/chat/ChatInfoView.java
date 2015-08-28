package ru.korniltsev.telegram.profile.chat;

import android.content.Context;
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
import ru.korniltsev.telegram.attach_panel.AttachPanelPopup;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.attach_panel.RecentImagesBottomSheet;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.MuteForPopupFactory;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.flow.pathview.TraversalAware;
import ru.korniltsev.telegram.core.flow.pathview.TraversalAwareHelper;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.photoview.PhotoView;
import ru.korniltsev.telegram.profile.other.ProfileView;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.drinkless.td.libcore.telegram.TdApi.File.NO_FILE_ID;
import static ru.korniltsev.telegram.common.AppUtils.flatten;

public class ChatInfoView extends FrameLayout implements HandlesBack , TraversalAware{
    @Inject ChatInfoPresenter presenter;
    @Inject DpCalculator calc;
    @Inject PhoneFormat phoneFormat;
    @Inject ActivityOwner owner;
    final TraversalAwareHelper traversalHelper = new TraversalAwareHelper();

    private RecyclerView list;
    private LinearLayoutManager listLayout;
    private FakeToolbar fakeToolbar;
    private ChatInfoAdapter adapter;
    private ToolbarUtils toolbar;
    @Nullable private ListChoicePopup mutePopup;
    private AttachPanelPopup selectImage;
    @Nullable private TdApi.GroupChatFull chatFull;
    private List<RecyclerView.ItemDecoration> currentDecoration;

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
        list.setItemAnimator(null);
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
                selectImage = RecentImagesBottomSheet.create(owner.expose(), presenter, false);
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
        traversalHelper.setTraversalStarted();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
        if (mutePopup != null){
            mutePopup.dismiss();
        }
    }

    public void bindChat(final TdApi.GroupChatFull chat1, final ChatInfo chatInfo, final TdApi.Messages ms) {
        traversalHelper.runWhenTraversalCompleted(new Runnable() {
            @Override
            public void run() {
                bindChatImpl(chat1, chatInfo, ms);
            }
        });
    }

    private void bindChatImpl(TdApi.GroupChatFull chat1, ChatInfo chatInfo, TdApi.Messages ms) {
        this.chatFull = chat1;
        fakeToolbar.bindChat(chat1);
        final List<ChatInfoAdapter.Item> data = adapter.getData();
        final ChatInfoAdapter.Item header = data.get(0);
        adapter.clearData();
        adapter.add(header);

        if (currentDecoration != null){
            for (RecyclerView.ItemDecoration d: currentDecoration){
                list.removeItemDecoration(d);
            }
        }
        //        List<ChatInfoAdapter.Item> data = new ArrayList<>();
        List<List<ChatInfoAdapter.Item>> sections = new ArrayList<>();
        final ChatInfoAdapter.ButtonItem buttonItem = new ChatInfoAdapter.ButtonItem();
        sections.add(Collections.<ChatInfoAdapter.Item>singletonList(buttonItem));

        final ChatInfoAdapter.MediaItem mediaItem = new ChatInfoAdapter.MediaItem(ms.totalCount,
                AppUtils.filterPhotosAndVideos(
                        asList(ms.messages)));
        sections.add(Collections.<ChatInfoAdapter.Item>singletonList(mediaItem));

        final ArrayList<ChatInfoAdapter.Item> participantsItems = new ArrayList<>();
        sections.add(participantsItems);
        TdApi.ChatParticipant[] participants = chat1.participants;

        for (int i = 0, participantsLength = participants.length; i < participantsLength; i++) {
            TdApi.ChatParticipant participant = participants[i];
            participantsItems.add(new ChatInfoAdapter.ParticipantItem(i == 0, participant.user));
        }
        for (TdApi.User it : chatInfo.addedUsers) {
            participantsItems.add(new ChatInfoAdapter.ParticipantItem(false, it));
        }
        adapter.addAll(flatten(sections));

        currentDecoration = ProfileView.decorate(getContext(), list, calc, sections);
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
        if (selectImage != null && selectImage.isShowing()) {
            selectImage.dismiss();
            selectImage = null;
            return true;
        }
        selectImage = null;
        return false;
    }

    public void hideAttachPannel() {
        if (selectImage != null && selectImage.isShowing()){
            selectImage.dismiss();
            selectImage = null;
        }
    }

    public void bindChatAvatar(TdApi.Chat chat) {
        fakeToolbar.bindChatAvatar(chat);

    }

    public void setChatTitle(String title) {
        fakeToolbar.setTitle(title);
    }

    @Override
    public void onTraversalCompleted() {
        traversalHelper.setTraversalCompleted();
    }
}
