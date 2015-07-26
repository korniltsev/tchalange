package ru.korniltsev.telegram.profile.chat;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import phoneformat.PhoneFormat;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.profile.decorators.BottomShadow;
import ru.korniltsev.telegram.profile.decorators.InsetDecorator;
import ru.korniltsev.telegram.profile.decorators.MyWhiteRectTopPaddingDecorator;
import ru.korniltsev.telegram.profile.decorators.TopShadow;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ChatInfoView extends FrameLayout {
    @Inject ChatInfoPresenter presenter;
    @Inject DpCalculator calc;
    @Inject PhoneFormat phoneFormat;

    private RecyclerView list;
    private LinearLayoutManager listLayout;
    private FakeToolbar fakeToolbar;
    private ChatInfoAdapter adapter;
    private ToolbarUtils toolbar;

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
        list.addItemDecoration(new MyWhiteRectTopPaddingDecorator(1, calc.dp(25)));
        list.addItemDecoration(new BottomShadow(ctx, calc, 1));

        if (participants.length >0){
            list.addItemDecoration(new InsetDecorator(2, calc.dp(6)));
            list.addItemDecoration(new TopShadow(ctx, calc, 2));//todo fix when shared media added



            list.addItemDecoration(new BottomShadow(ctx, calc, adapter.getItemCount() -1 ));
        }

    }


}
