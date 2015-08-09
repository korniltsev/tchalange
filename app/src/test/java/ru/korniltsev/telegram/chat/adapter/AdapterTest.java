package ru.korniltsev.telegram.chat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import org.drinkless.td.libcore.telegram.TdApi;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.powermock.api.easymock.PowerMock.createMock;

/*
Use the @RunWith(PowerMockRunner.class) annotation at the class-level of the test case.
        Use the @PrepareForTest(ClassThatContainsStaticMethod.class) annotation at the class-level of the test case.
        Use PowerMock.mockStatic(ClassThatContainsStaticMethod.class) to mock all methods of this class.
        Use PowerMock.replay(ClassThatContainsStaticMethod.class) to change the class to replay mode.
        Use PowerMock.verify(ClassThatContainsStaticMethod.class) to change the class to verify mode.*/
@RunWith(PowerMockRunner.class)
@PrepareForTest(LayoutInflater.class)
public class AdapterTest {
    public static final int FROM_USER_A = 1;
    public static final int FROM_USER_B = 2;
    @Test
    public void testFail() {
        PowerMock.mockStatic(LayoutInflater.class);

        final Context ctx = createMock(Context.class);
        final RxGlide rxGlide = createMock(RxGlide.class);
        final Chat chat = createMock(Chat.class);
        final UserHolder holder = createMock(UserHolder.class);

        final Adapter adapter = new Adapter(ctx, rxGlide, 0, 0, chat, holder);//Mockito.mock(Adapter.class);

        List<ChatListItem> cs = new ArrayList<>();
        cs.add(
                createMessage(FROM_USER_A, new TdApi.MessageAudio()));
        cs.add(
                createMessage(FROM_USER_A, new TdApi.MessageAudio()));

        adapter.addAll(cs);


//        stub(mock.getItem(0));
//        for (int i = 0; i < cs.size(); i++) {
//            when(mock.getItem(i)).thenReturn(cs.get(i));
//        }
//        when(mock.getItemCount()).thenReturn(cs.size());
//
//        when(mock.getAudioViewType(0)).thenCallRealMethod();
//        when(mock.getAudioViewType(1)).thenCallRealMethod();

        assert adapter.getAudioViewType(0) == -1;
        assert adapter.getAudioViewType(1) == -1;
    }

    private MessageItem createMessage(int from, TdApi.MessageAudio content) {
        final TdApi.Message msg2 = new TdApi.Message();
        msg2.fromId = from;
        msg2.message = content;
        return new MessageItem(msg2);
    }
}