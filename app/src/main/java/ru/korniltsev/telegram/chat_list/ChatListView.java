package ru.korniltsev.telegram.chat_list;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.audio.LinearLayoutWithShadow;
import ru.korniltsev.telegram.core.audio.MiniPlayerView;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.recycler.CheckRecyclerViewSpan;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import rx.functions.Action1;

import javax.inject.Inject;
import java.util.List;

import static ru.korniltsev.telegram.core.Utils.event;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class ChatListView extends DrawerLayout {

    private final DpCalculator calc;
    @Inject ChatListPresenter presenter;
    @Inject ChatDB chatDb;
    @Inject PhoneFormat phoneFormat;
    @Inject UserHolder userHolder;

    private RecyclerView list;
    private ChatListAdapter adapter;
    private ToolbarUtils toolbar;
    private LinearLayoutManager layout;

    //drawer
    private AvatarView drawerAvatar;
    private TextView drawerName;
    private TextView drawerPhone;
    private View btnLogout;
    private View btnContacts;
    private View btnSettings;
    private MiniPlayerView miniPlayer;
    private LinearLayoutWithShadow toolbarShadow;

    public ChatListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        calc = MyApp.from(context).dpCalculator;

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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        injectViews();
        //list
        adapter = new ChatListAdapter(getContext(),presenter.getCl().myId.id, new Action1<TdApi.Chat>() {
            @Override
            public void call(TdApi.Chat chat) {
                presenter.openChat(chat);
            }
        }, chatDb, userHolder);
        layout = new LinearLayoutManager(getContext());
        list.setLayoutManager(layout);
        list.setAdapter(adapter);
        list.setOnScrollListener(
                new EndlessOnScrollListener(layout, adapter, /*waitForLastItem*/ new Runnable() {
                    @Override
                    public void run() {
                        presenter.listScrolledToEnd();
                    }
                }));

        //toolbar
        toolbar = initToolbar(this)
                .addMenuItem(R.menu.chat_list, R.id.menu_lock_unlock, new Runnable() {
                    @Override
                    public void run() {
                        presenter.lockUnlock();
                    }
                })
                .setDrawer(this, R.string.navigation_drawer_open, R.string.navigation_drawer_close);//todo what is open and clos?


        btnLogout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                event("btnLogout.Click");
                presenter.logout();
//                closeDrawer(Gravity.LEFT);
            }
        });
        btnContacts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.openContacts();
//                closeDrawer(Gravity.LEFT);
            }
        });

        btnSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.openSettings();
//                closeDrawer(Gravity.LEFT);
            }
        });

        miniPlayer = ((MiniPlayerView) findViewById(R.id.mini_player));
        toolbarShadow = ((LinearLayoutWithShadow) findViewById(R.id.toolbar_shadow));
        toolbarShadow.setShadowOffset(calc.dp(56f));
        miniPlayer.setShadow(toolbarShadow);

    }

    private void injectViews() {
        list = (RecyclerView) this.findViewById(R.id.list);
        drawerAvatar = (AvatarView) this.findViewById(R.id.drawer_avatar);
        drawerName = ((TextView) this.findViewById(R.id.drawer_name));
        drawerPhone = ((TextView) this.findViewById(R.id.drawer_phone));
        btnLogout = this.findViewById(R.id.btn_logout);
        btnContacts = this.findViewById(R.id.btn_contacts);
        btnSettings = this.findViewById(R.id.btn_settings);
    }


    public ChatListAdapter getAdapter() {
        return adapter;
    }

    public void showMe(TdApi.User user) {
        drawerAvatar.loadAvatarFor(user);
        drawerName.setText(
                AppUtils.uiName(user, getContext()));

        String phoneNumber = user.phoneNumber;
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
        }
        drawerPhone.setText(
                phoneFormat.format(phoneNumber));
    }

    public void updateNetworkStatus(boolean connected) {
        toolbar.setTitle(connected ? R.string.messages : R.string.waiting_for_connection);
    }

    public void setData(List<TdApi.Chat> allChats) {
        getAdapter()
                .setData(allChats);
        CheckRecyclerViewSpan.check(list, new Runnable() {
            @Override
            public void run() {
                presenter.listScrolledToEnd();
            }
        });
    }

    public void bindLockButton(boolean locked, boolean enabled) {
        final MenuItem menu = toolbar.toolbar.getMenu().findItem(R.id.menu_lock_unlock);
        if (enabled){
            if (locked) {
                menu.setIcon(R.drawable.ic_lock_close);
                menu.setTitle(R.string.action_unlock);
            } else {
                menu.setIcon(R.drawable.ic_lock_open);
                menu.setTitle(R.string.action_lock);
            }
            menu.setVisible(true);
        } else {
            menu.setVisible(false);
        }




    }

    @Override
    protected Parcelable onSaveInstanceState() {
        super.onSaveInstanceState();
        return  new SavedState(BaseSavedState.EMPTY_STATE);//do not save state at all
    }
}
