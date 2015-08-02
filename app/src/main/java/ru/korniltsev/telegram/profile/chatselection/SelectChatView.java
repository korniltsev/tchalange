package ru.korniltsev.telegram.profile.chatselection;

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
import ru.korniltsev.telegram.chat_list.ChatListAdapter;
import ru.korniltsev.telegram.common.MuteForPopupFactory;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.recycler.EndlessOnScrollListener;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.profile.decorators.BottomShadow;
import ru.korniltsev.telegram.profile.decorators.DividerItemDecorator;
import ru.korniltsev.telegram.profile.decorators.InsetDecorator;
import ru.korniltsev.telegram.profile.decorators.MyWhiteRectTopPaddingDecorator;
import ru.korniltsev.telegram.profile.decorators.TopShadow;
import ru.korniltsev.telegram.profile.other.ProfileAdapter;
import rx.functions.Action1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.isEmpty;
import static ru.korniltsev.telegram.common.AppUtils.call;
import static ru.korniltsev.telegram.common.AppUtils.copy;
import static ru.korniltsev.telegram.common.AppUtils.phoneNumberWithPlus;

public class SelectChatView extends FrameLayout {
    @Inject SelectChatPresenter presenter;

    private RecyclerView list;
    private LinearLayoutManager listLayout;
    private ChatListAdapter adapter;

    public SelectChatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        listLayout = new LinearLayoutManager(getContext());
        list = ((RecyclerView) findViewById(R.id.list));
        list.setLayoutManager(listLayout);
        ToolbarUtils.initToolbar(this)
                .setTitle(R.string.select_group)
                .pop();

    }

    public List<TdApi.Chat> getData(){
        return adapter.getData();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void setData(List<TdApi.Chat> onlyGroupChats) {
        adapter.setData(onlyGroupChats);
    }

    public void init(TdApi.User me, ChatDB chats) {
        adapter = new ChatListAdapter(getContext(), me.id, new Action1<TdApi.Chat>() {
            @Override
            public void call(TdApi.Chat chat) {
                presenter.chatSelected(chat);
            }
        }, chats);
        list.setAdapter(adapter);
        list.setOnScrollListener(
                new EndlessOnScrollListener(listLayout, adapter, new Runnable() {
                    @Override
                    public void run() {
                        presenter.listScrolledToEnd();
                    }
                }, 0));
    }
}
