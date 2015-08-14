package ru.korniltsev.telegram.chat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import flow.Flow;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.chat.adapter.TextMessageVH;
import ru.korniltsev.telegram.chat.bot.BotCommandsAdapter;
import ru.korniltsev.telegram.chat.keyboard.hack.FrameUnderMessagePanelController;
import ru.korniltsev.telegram.chat.keyboard.hack.TrickyBottomFrame;
import ru.korniltsev.telegram.chat.keyboard.hack.TrickyFrameLayout;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;
import ru.korniltsev.telegram.audio.LinearLayoutWithShadow;
import ru.korniltsev.telegram.audio.MiniPlayerView;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.emoji.ObservableLinearLayout;
import ru.korniltsev.telegram.chat.adapter.Adapter;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.flow.pathview.TraversalAware;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.recycler.CheckRecyclerViewSpan;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.DaySplitter;
import ru.korniltsev.telegram.core.rx.EmojiParser;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.rx.items.BotInfoItem;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.DaySeparatorItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.rx.items.NewMessagesItem;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.MuteForPopupFactory;
import rx.Subscription;
import rx.functions.Action1;

import javax.inject.Inject;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class ChatView extends ObservableLinearLayout implements HandlesBack , TraversalAware{
    public static final int SHOW_SCROLL_DOWN_BUTTON_ITEMS_COUNT = 10;
    public static final DecelerateInterpolator INTERPOLATOR = new DecelerateInterpolator(1.5f);
    @Inject Presenter presenter;
    @Inject RxGlide picasso;
    @Inject ActivityOwner activity;


    @Inject UserHolder userHodler;

    DpCalculator calc;
    EmojiParser emojiParser;

    private RecyclerView list;
    private MessagePanel messagePanel;
    private LinearLayoutManager layout;
    private ToolbarUtils toolbar;
    private AvatarView toolbarAvatar;
    private TextView toolbarTitle;
    private TextView toolbarSubtitle;
    private View btnScrollDown;
    private View emptyView;
    private View customToolbarView;
    private RecyclerView botsCommandList;
    private View emptyViewBotInfo;
    private TextView botInfoDescription;
//    private LinearLayout botReplyKeyboard;
    private View botCommandsListConainer;

    private Adapter adapter;

    private ListChoicePopup mutePopup;

    //    private TdApi.BotInfoGeneral commands;
    private BotCommandsAdapter botsCommandAdapter;

    //    @Nullable private TdApi.BotInfoGeneral botInfo;
    @Nullable BotInfoItem botInfoItem;
    private boolean isBot;

    private Runnable viewSpanNotFilledAction = new Runnable() {
        @Override
        public void run() {
            presenter.listScrolledToEnd();
        }
    };
    private int myId;
    private View botStartPanel;
    private TextView btnBotStart;
    private Subscription clickedSpansSubscription;
    private View botCommandsShadow;
    private VoiceRecordingOverlay voiceOverlay;
    private DaySplitter splitter;
    private LinearLayoutWithShadow toolbarShadow;
    private MiniPlayerView miniPlayerView;

    public ChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);

        final MyApp from = MyApp.from(context);
        calc = from.dpCalculator;
        emojiParser = from.emojiParser;
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
        adapter = new Adapter(getContext(), picasso, presenter.getPath().chat.lastReadOutboxMessageId, myId, presenter.getPath(), userHodler);
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
        adapter.registerAdapterDataObserver(new EmptyViewHelper(new Runnable() {
            @Override
            public void run() {
                updateEmptyView();
            }
        }));
        activity.setStatusBarColor(getResources().getColor(R.color.primary_dark));

        botsCommandList = ((RecyclerView) findViewById(R.id.bot_commands_list));
        botsCommandList.setLayoutManager(new LinearLayoutManager(getContext()));
//        botReplyKeyboard = ((LinearLayout) findViewById(R.id.bot_reply_keyboard));
        emptyViewBotInfo = findViewById(R.id.bot_info_root);
        botInfoDescription = ((TextView) findViewById(R.id.bot_info_description));

        botStartPanel = findViewById(R.id.bot_start_panel);
        btnBotStart = (TextView) findViewById(R.id.btn_bot_start);
        botCommandsShadow = findViewById(R.id.bot_command_shadow);
        botCommandsListConainer = findViewById(R.id.bot_commands_list_container);

        final TrickyBottomFrame bottomFrame = (TrickyBottomFrame) findViewById(R.id.frame_under_message_panel);
        final TrickyFrameLayout tricky = (TrickyFrameLayout) findViewById(R.id.list_and_message_panel);
        messagePanel.initBottomFrame(bottomFrame, tricky);
        messagePanel.setOnAnyKeyboardShownListener(new Runnable() {
            @Override
            public void run() {
                if (messagePanel.doNotHideCommandsOnce) {
                    messagePanel.doNotHideCommandsOnce = false;//todo quick hack. delete when have time
                } else {
                    hideCommandList();
                }
            }
        });
        messagePanel.getBottomFrame().setBotCommandClickListener(new FrameUnderMessagePanelController.BotCommandClickListener() {
            @Override
            public void cmdClicked(String cmd, TdApi.Message msg) {
                final TdApi.ReplyMarkupShowKeyboard replyMarkup = (TdApi.ReplyMarkupShowKeyboard) msg.replyMarkup;
                if (replyMarkup.oneTime) {
                    hideReplyKeyboard();
                }
                presenter.sendBotKeyboardCommand(cmd, msg);
            }
        });
        list.setItemAnimator(null);
        voiceOverlay = ((VoiceRecordingOverlay) findViewById(R.id.voice_recording_overlay));
        messagePanel.getInput().addTextChangedListener(new TextWatcherAdapter(){
            @Override
            public void afterTextChanged(Editable s) {
                voiceOverlay.setStateEnabled(s.length() == 0);
            }
        });
        splitter = presenter.getRxChat().daySplitter;
        adapter.setClickListner(new Adapter.Callback() {
            @Override
            public void avatarOfMessageClicked(TdApi.Message msg) {
                presenter.openChatWithAuthorOf(msg);
            }
        });

        messagePanel.getInput().requestFocus();
        toolbarShadow = ((LinearLayoutWithShadow) findViewById(R.id.toolbar_shadow));
        miniPlayerView = ((MiniPlayerView) findViewById(R.id.mini_player));
        miniPlayerView.setShadow(toolbarShadow);

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
        clickedSpansSubscription = emojiParser.getClickedSpans()
                .subscribe(new ObserverAdapter<EmojiParser.BotCommand>() {
                    @Override
                    public void onNext(EmojiParser.BotCommand response) {
                        presenter.textSpanCLicked(response);
                    }
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
        clickedSpansSubscription.unsubscribe();
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public void initMenu(TdApi.Chat groupChat, boolean muted) {
        boolean isGroupChat = groupChat.type instanceof TdApi.GroupChatInfo;
        if (isGroupChat) {
            final TdApi.GroupChatInfo group = (TdApi.GroupChatInfo) groupChat.type;
            if (group.groupChat.left) {
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
        loadAvatarFor(chat);
    }

    public void loadAvatarFor(TdApi.TLObject chat) {
        toolbarAvatar.loadAvatarFor(chat);
    }

    public void setGroupChatTitle(final TdApi.GroupChat groupChat, final TdApi.Chat chat) {
        toolbarTitle.setText(
                groupChat.title);
        if (groupChat.left) {
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
        List<ChatListItem> split = splitter.split(messages);
        adapter.setData(split);
        CheckRecyclerViewSpan.check(list, viewSpanNotFilledAction);
    }

//    private final DaySplitter splitter = new DaySplitter();

    //    public void setMessages( List<TdApi.Message> messages) {
    //        List<RxChat.ChatListItem> split = splitter.split(messages);
    //        adapter.setData(split);
    //    }

    @Override
    public boolean onBackPressed() {
        if (mutePopup != null && mutePopup.isShowing()) {
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

    public void addNewMessages(List<TdApi.Message> ms) {
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
        List<ChatListItem> data = adapter.getData();
        final List<ChatListItem> splitNewMessages = splitter.split(ms);
        List<ChatListItem> prepend = splitter.prepend(data, splitNewMessages);

        adapter.addFirst(prepend);
        if (scrollDown) {
            layout.scrollToPosition(0);
        }

        addBotInfoItem();
    }

    private void addBotInfoItem() {
        if (botInfoItem == null) {
            return;
        }
        final List<ChatListItem> data = adapter.getData();
        if (data.size() == 0) {
            return;
        }

        final ChatListItem last = data.get(data.size() - 1);
        if (last instanceof BotInfoItem) {
            return;
        }
        adapter.add(botInfoItem);
    }

    public void addHistory(TdApi.Chat chat, RxChat.HistoryResponse history) {
        removeBotItem();
        final List<ChatListItem> data = adapter.getData();

        final List<ChatListItem> split = history.split;//splitter.split(history.ms);
        if (history.showUnreadMessages) {
            final NewMessagesItem newItem = splitter.insertNewMessageItem(split, chat, myId);
            adapter.addAll(split);
            final int i = data
                    .indexOf(newItem);
            if (i != -1) {
                final int badgeHeight = calc.dp(26);
                final int nicePadding = calc.dp(8);
                layout.scrollToPositionWithOffset(i, list.getHeight() - badgeHeight - nicePadding);
                if (i >= SHOW_SCROLL_DOWN_BUTTON_ITEMS_COUNT) {
                    animateBtnScrollDown(true);
                }
            }
        } else {
            adapter.addAll(split);
        }
        addBotInfoItem();
    }

    @NonNull
    private List<ChatListItem> removeBotItem() {
        final List<ChatListItem> data = adapter.getData();
        if (data.size() > 0) {
            final ChatListItem last = data.get(data.size() - 1);
            if (last instanceof BotInfoItem) {
                adapter.remove(data.size() - 1);
            }
        }
        return data;
    }

    public void deleteMessages(RxChat.DeletedMessages deleted) {
        if (deleted.all) {
            adapter.clearData();
        } else {
            for (TdApi.Message m : deleted.ms) {
                deleteMessage(m);
            }
        }
    }

    private void deleteMessage(TdApi.Message deletedMsg) {
        //todo wtf, why so complex
        final List<ChatListItem> data = adapter.getData();
        for (int i = 0; i < data.size(); i++) {
            ChatListItem item = data.get(i);
            if (item instanceof MessageItem) {
                final TdApi.Message msg = ((MessageItem) item).msg;
                if (msg == deletedMsg) {
                    adapter.deleteItem(i);
                    if (i != 0) {//not first item
                        final ChatListItem next = data.get(i - 1);
                        //next item is not message
                        if (!(next instanceof MessageItem)) {
                            if (i < data.size()) {
                                final ChatListItem prev = data.get(i);
                                if (prev instanceof DaySeparatorItem) {
                                    adapter.deleteItem(i);//delete separator
                                }
                            }
                        }
                    } else {
                        if (i < data.size()) {
                            final ChatListItem prev = data.get(i);
                            if (prev instanceof DaySeparatorItem) {
                                adapter.deleteItem(i);//delete separator
                            }
                        }
                    }
                    break;
                }
            }
        }

        if (!data.isEmpty()) {
            if (data.get(0) instanceof NewMessagesItem) {
                adapter.deleteItem(0);
            }
        }
    }

    public void messageChanged(TdApi.Message response) {
        final List<ChatListItem> data = adapter.getData();
        for (int i = 0; i < data.size(); i++) {
            ChatListItem it = data.get(i);
            if (it instanceof MessageItem) {
                final TdApi.Message msg = ((MessageItem) it).msg;
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

    boolean botCommandListVisible = false;

    public void setCommands(List<BotCommandsAdapter.Record> cs) {
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
                if (result == 0) {
                    hideCommandList();

                } else {

                    int newHeight;
                    final int commandHeight = calc.dp(36);
                    if (result > 3) {
                        newHeight = (int) (3.5f * commandHeight);
                    } else {
                        newHeight = commandHeight * result;
                    }
                    newHeight += botsCommandList.getPaddingTop();
                    final ViewGroup.LayoutParams lp = botsCommandList.getLayoutParams();
                    lp.height = newHeight;
                    botsCommandList.setLayoutParams(lp);
                    showCommandList(newHeight);
                }
            }
        });

        messagePanel.setCommands(cs);


    }

    private void showCommandList(int newHeight) {
        if (!botCommandListVisible) {
            botCommandsListConainer.setVisibility(View.VISIBLE);
            botsCommandList.clearAnimation();
            botsCommandList.setTranslationY(newHeight);
            botsCommandList.animate()
                    .translationY(0)
                    .setDuration(128)
                    .setInterpolator(INTERPOLATOR)
                    .setListener(null);

            botCommandsShadow.setVisibility(View.VISIBLE);
            botCommandsShadow.clearAnimation();
            botCommandsShadow.setAlpha(0f);
            botCommandsShadow.animate()
                    .setListener(null)
                    .alpha(0.4f);
        }

        botCommandListVisible = true;
    }

    private void hideCommandList() {
        if (botCommandListVisible) {

            botCommandsShadow.clearAnimation();
            botCommandsShadow.animate()
                    .alpha(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            botCommandsShadow.setVisibility(View.GONE);
                        }
                    });

            botsCommandList.clearAnimation();
            botsCommandList.animate()
                    .setDuration(128)
                    .setInterpolator(INTERPOLATOR)
                    .translationY(botsCommandList.getHeight())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            botCommandsListConainer.setVisibility(View.GONE);
                        }
                    });
        }
        botCommandListVisible = false;
    }

    public void showBotKeyboard(TdApi.Message msg) {
        final TdApi.ReplyMarkupShowKeyboard markup = (TdApi.ReplyMarkupShowKeyboard) msg.replyMarkup;
        messagePanel.getBottomFrame().showBotKeyboard( msg);
        messagePanel.setReplyMarkup(msg);
    }

    public void hideReplyKeyboard() {
        messagePanel.getBottomFrame()
                .dismisAnyKeyboard();
        messagePanel.setReplyMarkup(null);

    }

    public void addBotInfoHeader(TdApi.BotInfoGeneral botInfo, final TdApi.User user) {
        final Spannable botDescriptionWithEmoji = emojiParser.parseEmoji(botInfo.description, user.id);
        this.botInfoItem = new BotInfoItem(botInfo, botDescriptionWithEmoji);
        addBotInfoItem();
        adapter.notifyDataSetChanged();//to show emptyView with botinfo
        botInfoDescription.setText(botInfoItem.descriptionWithEmoji);
        botInfoDescription.setMovementMethod(LinkMovementMethod.getInstance());
        TextMessageVH.applyTextStyle(botInfoDescription);

        btnBotStart.setText("START");//todo l10n
        btnBotStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.sendText("/start");
            }
        });


    }

    private void updateEmptyView() {
        final boolean shouldShowEmptyView = adapter.getItemCount() == 0;
        if (isBot) {
            emptyView.setVisibility(View.INVISIBLE);
            if (shouldShowEmptyView && botInfoItem != null) {
                emptyViewBotInfo.setVisibility(View.VISIBLE);
                if (botInfoItem.botInfo.commands.length == 0) {
                    botStartPanel.setVisibility(View.INVISIBLE);
                } else {
                    botStartPanel.setVisibility(View.VISIBLE);
                }
            } else {
                botStartPanel.setVisibility(View.INVISIBLE);
                emptyViewBotInfo.setVisibility(View.INVISIBLE);
            }
        } else {
            emptyViewBotInfo.setVisibility(View.INVISIBLE);
            botStartPanel.setVisibility(View.INVISIBLE);

            if (shouldShowEmptyView) {
                emptyView.setVisibility(View.VISIBLE);
            } else {
                emptyView.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void setBot(boolean isBot) {
        this.isBot = isBot;
    }

    @Override
    public void onTraversalCompleted() {
        list.setItemAnimator(new DefaultItemAnimator());
    }

    //    public void setCommands(TdApi.ChatParticipant[] participants) {
    //
    //        setCommands(cs);
    //    }
}
