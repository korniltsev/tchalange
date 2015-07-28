package ru.korniltsev.telegram.chat;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import flow.Flow;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.bot.BotCommandsAdapter;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.ObservableLinearLayout;
import ru.korniltsev.telegram.chat.adapter.Adapter;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.recycler.CheckRecyclerViewSpan;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.DaySplitter;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.MuteForPopupFactory;
import rx.functions.Action1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class ChatView extends ObservableLinearLayout implements HandlesBack {
    public static final int SHOW_SCROLL_DOWN_BUTTON_ITEMS_COUNT = 10;
    @Inject Presenter presenter;
    @Inject RxGlide picasso;
    @Inject DpCalculator calc;
    @Inject ActivityOwner activity;

    private RecyclerView list;
    private MessagePanel messagePanel;
    private LinearLayoutManager layout;
    private ToolbarUtils toolbar;
    private AvatarView toolbarAvatar;
    private TextView toolbarTitle;
    private TextView toolbarSubtitle;

    private Adapter adapter;
    private final int toolbarAvatarSize;

    private Runnable viewSpanNotFilledAction = new Runnable() {
        @Override
        public void run() {
            presenter.listScrolledToEnd();
        }
    };
    private View btnScrollDown;
    private View emptyView;
    private int myId;
    private View customToolbarView;
    private ListChoicePopup mutePopup;
    private RecyclerView botsCommandList;


    private TdApi.BotInfoGeneral commands;
    private BotCommandsAdapter botsCommandAdapter;

    public ChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        toolbarAvatarSize = context.getResources().getDimensionPixelSize(R.dimen.toolbar_avatar_size);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbar = initToolbar(this)
                .pop(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                        Flow.get(v)
                                .goBack();
                    }
                })
                .customView(R.layout.chat_toolbar_title)
                .inflate(R.menu.chat)
                .setMenuClickListener(presenter);
        customToolbarView = toolbar.getCustomView();
        assertNotNull(customToolbarView);
        toolbarAvatar = ((AvatarView) customToolbarView.findViewById(R.id.chat_avatar));
        toolbarTitle = ((TextView) customToolbarView.findViewById(R.id.title));
        toolbarSubtitle = ((TextView) customToolbarView.findViewById(R.id.subtitle));
        list = (RecyclerView) findViewById(R.id.list);
        messagePanel = (MessagePanel) findViewById(R.id.message_panel);
        btnScrollDown = findViewById(R.id.scroll_down);
        messagePanel.setListener(presenter);

        layout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, true);
        myId = presenter.getPath().me.id;
        adapter = new Adapter(getContext(), picasso, presenter.getPath().chat.lastReadOutboxMessageId, myId, presenter.getPath());
        list.setLayoutManager(layout);
        list.setAdapter(adapter);
        btnScrollDown.setAlpha(0f);
        list.setOnScrollListener(
                new EndlessOnScrollListener(layout, adapter, /*waitForLastItem*/  new Runnable() {
                    @Override
                    public void run() {
                        presenter.listScrolledToEnd();
                    }
                }) {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        boolean newVisible = layout.findFirstVisibleItemPosition() >= SHOW_SCROLL_DOWN_BUTTON_ITEMS_COUNT;
                        animateBtnScrollDown(newVisible);
                    }
                });

        btnScrollDown.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnScrollDown.getAlpha() == 0f) {
                    return;
                }
                layout.scrollToPosition(0);
                list.stopScroll();
                btnScrollDown.clearAnimation();
                btnScrollDown.setAlpha(0);
            }
        });
        emptyView = findViewById(R.id.empty_view);
        adapter.registerAdapterDataObserver(new EmptyViewHelper());
        activity.setStatusBarColor(getResources().getColor(R.color.primary_dark));

        botsCommandList = ((RecyclerView) findViewById(R.id.bot_commands_list));
        botsCommandList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    boolean scrollDownButtonIsVisible = false;

    private void animateBtnScrollDown(boolean newVisible) {
        if (newVisible != scrollDownButtonIsVisible) {
            btnScrollDown.clearAnimation();
            btnScrollDown.animate()
                    .alpha(newVisible ? 1f : 0f);
            scrollDownButtonIsVisible = newVisible;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void initMenu(TdApi.Chat groupChat, boolean muted) {
        boolean isGroupChat = groupChat.type instanceof TdApi.GroupChatInfo;
        if (isGroupChat) {
            final TdApi.GroupChatInfo group = (TdApi.GroupChatInfo) groupChat.type;
            if (group.groupChat.left){
                toolbar.hideMenu(R.id.menu_mute_unmute);
                toolbar.hideMenu(R.id.menu_leave_group);
            }
        } else {
            toolbar.hideMenu(R.id.menu_leave_group);
        }

        final MenuItem muteMenu = toolbar.toolbar.getMenu().findItem(R.id.menu_mute_unmute);
        if (muted) {
            muteMenu.setTitle(R.string.unmute);
        } else {
            muteMenu.setTitle(R.string.mute);
        }
    }

    public void loadToolBarImage(TdApi.Chat chat) {
        toolbarAvatar.loadAvatarFor(chat);
    }

    public void setGroupChatTitle(final TdApi.GroupChat groupChat, final TdApi.Chat chat) {
        toolbarTitle.setText(
                groupChat.title);
        if (groupChat.left){
            return;
        }
        customToolbarView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.open(chat);
            }
        });
    }

    public void setPrivateChatTitle(final TdApi.User user) {
        toolbarTitle.setText(
                AppUtils.uiName(user, getContext()));
        customToolbarView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.open(user);
            }
        });
    }

    public void setwGroupChatSubtitle(int total, int online) {
        //todo updates
        Resources res = getResources();
        String totalStr = res.getQuantityString(R.plurals.group_chat_members, total, total);
        String onlineStr = res.getQuantityString(R.plurals.group_chat_members_online, online, online);
        toolbarSubtitle.setText(
                totalStr + ", " + onlineStr);

    }

//    private static DateTimeFormatter SUBTITLE_FORMATTER = DateTimeFormat.forPattern("dd/MM/yy");

    //    private String lastSeenDaysAgo(int daysBetween) {
//        return getResources().getQuantityString(R.plurals.user_status_last_seen_n_days_ago, daysBetween, daysBetween);
//    }
//
//    private String lastSeenHoursAgo(int hoursBetween) {
//        return getResources().getQuantityString(R.plurals.user_status_last_seen_n_hours_ago, hoursBetween, hoursBetween);
//    }
//
//    private String lastSeenMinutesAgo(int minutesBetween) {
//        return getResources().getQuantityString(R.plurals.user_status_last_seen_n_minutes_ago, minutesBetween,minutesBetween);
//    }

    public void initList(RxChat rxChat) {
        adapter.setChat(rxChat);
        List<TdApi.Message> messages = rxChat.getMessages();
        List<RxChat.ChatListItem> split = splitter.split(messages);
        adapter.setData(split);
        CheckRecyclerViewSpan.check(list, viewSpanNotFilledAction);
    }

    private final DaySplitter splitter = new DaySplitter();

//    public void setMessages( List<TdApi.Message> messages) {
//        List<RxChat.ChatListItem> split = splitter.split(messages);
//        adapter.setData(split);
//    }



    @Override
    public boolean onBackPressed() {
        if (mutePopup != null && mutePopup.isShowing()){
            mutePopup.dismiss();
            mutePopup = null;
            return true;
        }
        mutePopup = null;
        return messagePanel.onBackPressed();
    }

    public void showMessagePanel(boolean left) {
        if (left) {
            messagePanel.setVisibility(View.GONE);
        } else {
            messagePanel.setVisibility(View.VISIBLE);
        }
    }

    public void scrollToBottom() {
        layout.scrollToPosition(0);
    }

    public void hideAttachPannel() {
        messagePanel.hideAttachPannel();
    }



    public void addNewMessage(TdApi.Message message) {
        boolean scrollDown;
        int firstFullVisible = layout.findFirstCompletelyVisibleItemPosition();
        if (firstFullVisible == 0) {
            scrollDown = true;
        } else {
            if (layout.findFirstVisibleItemPosition() == 0) {
                scrollDown = true;
            } else {
                scrollDown = false;
            }
        }
        List<RxChat.ChatListItem> data = adapter.getData();
        List<RxChat.ChatListItem> prepend= splitter.prepend(data, message);

        Collections.reverse(prepend);
        adapter.addFirst(prepend);
        if (scrollDown) {
            layout.scrollToPosition(0);
        }
    }

    public void addHistory(TdApi.Chat chat, RxChat.HistoryResponse history) {
        final List<RxChat.ChatListItem> split = splitter.split(history.ms);
        if (history.showUnreadMessages) {
            final RxChat.NewMessagesItem newItem = splitter.insertNewMessageItem(split, chat, myId);
            adapter.addAll(split);
            final int i = adapter.getData()
                    .indexOf(newItem);
            if (i != -1) {
//                if (list.get)
                final int badgeHeight = calc.dp(26);
                final int nicePadding = calc.dp(8);
                layout.scrollToPositionWithOffset(i, list.getHeight() - badgeHeight - nicePadding);
                if (i >= SHOW_SCROLL_DOWN_BUTTON_ITEMS_COUNT){
                    animateBtnScrollDown(true);
                }
            }
        } else {

            adapter.addAll(split);
        }
    }

    public void deleteMessages(RxChat.DeletedMessages deleted) {
        if (deleted.all){
            adapter.clearData();
        } else {
            for (TdApi.Message m : deleted.ms) {
                deleteMessage(m);
            }
        }
    }

    private void deleteMessage(TdApi.Message deletedMsg) {
        //todo wtf, why so complex
        final List<RxChat.ChatListItem> data = adapter.getData();
        for (int i = 0; i < data.size(); i++) {
            RxChat.ChatListItem item = data.get(i);
            if (item instanceof RxChat.MessageItem) {
                final TdApi.Message msg = ((RxChat.MessageItem) item).msg;
                if (msg == deletedMsg) {
                    adapter.deleteItem(i);
                    if (i != 0){//not first item
                        final RxChat.ChatListItem next = data.get(i-1);
                        //next item is not message
                        if (!(next instanceof RxChat.MessageItem)){
                            if (i < data.size()) {
                                final RxChat.ChatListItem prev = data.get(i);
                                if (prev instanceof RxChat.DaySeparatorItem){
                                    adapter.deleteItem(i);//delete separator
                                }
                            }
                        }
                    } else {
                        if (i < data.size()) {
                            final RxChat.ChatListItem prev = data.get(i);
                            if (prev instanceof RxChat.DaySeparatorItem){
                                adapter.deleteItem(i);//delete separator
                            }
                        }
                    }
                    break;
                }
            }
        }


        if (!data.isEmpty()){
            if (data.get(0) instanceof RxChat.NewMessagesItem) {
                adapter.deleteItem(0);

            }
        }
    }

    public void messageChanged(TdApi.Message response) {
        final List<RxChat.ChatListItem> data = adapter.getData();
        for (int i = 0; i < data.size(); i++) {
            RxChat.ChatListItem it = data.get(i);
            if (it instanceof RxChat.MessageItem) {
                final TdApi.Message msg = ((RxChat.MessageItem) it).msg;
                if (msg == response) {
                    adapter.notifyItemChanged(i);
                }
            }
        }
    }

    public void setPrivateChatSubtitle(String text) {
        toolbarSubtitle.setText(text);
    }

    public void showMutePopup() {
        mutePopup = MuteForPopupFactory.create(activity.expose(), new MuteForPopupFactory.Callback() {
            @Override
            public void muteFor(int duration) {
                presenter.muteFor(duration);
            }
        });
    }

    public void setCommands(TdApi.User user, TdApi.BotInfoGeneral i) {
        this.commands = i;
        List<BotCommandsAdapter.Record> cs = new ArrayList<>();
        for (TdApi.BotCommand command : i.commands) {
            cs.add(new BotCommandsAdapter.Record(user, command));
        }
        botsCommandAdapter = new BotCommandsAdapter(cs, getContext(), new Action1<BotCommandsAdapter.Record>() {
            @Override
            public void call(BotCommandsAdapter.Record record) {
                presenter.sendBotCommand(record.user, record.cmd);
                messagePanel.getInput()
                        .getText()
                        .clear();
            }
        });
        botsCommandList.setAdapter(botsCommandAdapter);
        messagePanel.getInput().addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                final int result = botsCommandAdapter.filter(s.toString());
                if (result == 0){
                    botsCommandList.setVisibility(View.GONE);
                }   else{
                    botsCommandList.setVisibility(View.VISIBLE);
                    int newHeight ;
                    final int commandHeight = calc.dp(36);
                    if (result > 3){
                        newHeight = (int) (3.5f * commandHeight);
                    } else {
                        newHeight = commandHeight * result;
                    }
                    final ViewGroup.LayoutParams lp = botsCommandList.getLayoutParams();
                    lp.height = newHeight;
                    botsCommandList.setLayoutParams(lp);
                }
            }
        });
    }


    private class EmptyViewHelper extends RecyclerView.AdapterDataObserver {
        @Override
        public void onChanged() {
            if (adapter.getItemCount() == 0) {
                if (emptyView.getVisibility() == INVISIBLE) {
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setAlpha(0f);
                    emptyView.animate()
                            .alpha(1);
                }
            } else {
                emptyView.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            onChanged();
        }
    }
}
