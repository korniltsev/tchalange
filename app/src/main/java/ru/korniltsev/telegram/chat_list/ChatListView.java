package ru.korniltsev.telegram.chat_list;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import dagger.ObjectGraph;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.audio.MiniPlayerViewFactory;
import ru.korniltsev.telegram.chat_list.view.MyPhoneView;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.audio.LinearLayoutWithShadow;
import ru.korniltsev.telegram.audio.MiniPlayerView;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.recycler.CheckRecyclerViewSpan;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

import javax.inject.Inject;
import java.util.List;

import static ru.korniltsev.telegram.core.Utils.event;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class ChatListView extends DrawerLayout {

    private final DpCalculator calc;
    @Inject ChatListPresenter presenter;
    @Inject ChatDB chatDb;
    //    @Inject PhoneFormat phoneFormat;
    @Inject UserHolder userHolder;

    private RecyclerView list;
    private ChatListAdapter adapter;
    private ToolbarUtils toolbar;
    private LinearLayoutManager layout;

    //drawer
    private AvatarView drawerAvatar;
    private TextView drawerName;
    private MyPhoneView drawerPhone;
    private View btnLogout;
    private View btnContacts;
    private View btnSettings;
    private MiniPlayerView miniPlayer;
    private LinearLayoutWithShadow toolbarShadow;
    private Subscription subscription;

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
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        injectViews();
        //list
        adapter = new ChatListAdapter(getContext(), presenter.getCl().myId.id, new Action1<TdApi.Chat>() {
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

        btnLogout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                event("btnLogout.Click");
                presenter.logout();
            }
        });
        btnContacts.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.openContacts();
            }
        });

        btnSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.openSettings();
            }
        });

        toolbarShadow = ((LinearLayoutWithShadow) findViewById(R.id.toolbar_shadow));

        miniPlayer = MiniPlayerViewFactory.construct(getContext(), calc);
        toolbarShadow.addView(miniPlayer, 1);
        toolbarShadow.setShadowOffset(calc.dp(56f));
        miniPlayer.setShadow(toolbarShadow);
    }



    private void injectViews() {
        list = (RecyclerView) this.findViewById(R.id.list);
        drawerAvatar = (AvatarView) this.findViewById(R.id.drawer_avatar);
        drawerName = ((TextView) this.findViewById(R.id.drawer_name));
        drawerPhone = ((MyPhoneView) this.findViewById(R.id.drawer_phone));
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
        drawerPhone.setText(phoneNumber);

        subscription = parsePhone(getContext().getApplicationContext(), phoneNumber)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ObserverAdapter<String>() {
                    @Override
                    public void onNext(String response) {
                        drawerPhone.setText(response);
                    }
                });
    }

    public Observable<String> parsePhone(final Context appCtx, final String phoneNumber) {
        return Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                final ObjectGraph objectGraph = ObjectGraphService.getObjectGraph(appCtx);
                final PhoneFormat formatter = objectGraph.get(PhoneFormat.class);//expensive operation
                final String result = formatter.format(phoneNumber);
                return Observable.just(result);
            }
        });
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

    public void bindToolbar(boolean locked, boolean enabled) {
        if (!enabled) {
            toolbar = initToolbar(this)
                    .setDrawer(this, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            return;
        }
        if (toolbar == null) {
            toolbar = initToolbar(this)
                    .addMenuItem(R.menu.chat_list, R.id.menu_lock_unlock, new Runnable() {
                        @Override
                        public void run() {
                            presenter.lockUnlock();
                        }
                    })
                    .setDrawer(this, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        }

        final MenuItem menu = toolbar.toolbar.getMenu().findItem(R.id.menu_lock_unlock);
        if (locked) {
            menu.setIcon(R.drawable.ic_lock_close);
            menu.setTitle(R.string.action_unlock);
        } else {
            menu.setIcon(R.drawable.ic_lock_open);
            menu.setTitle(R.string.action_lock);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        super.onSaveInstanceState();
        return new SavedState(BaseSavedState.EMPTY_STATE);//do not save state at all
    }
}
