package ru.korniltsev.telegram.chat.adapter;

import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.DocumentView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;


public class DocumentVH extends BaseAvatarVH {

    private final DocumentView documentView;

    public DocumentVH(CustomCeilLayout itemView, final Adapter adapter) {
        super(itemView, adapter);
        documentView =
                (DocumentView) adapter.getViewFactory().inflate(R.layout.chat_item_document, itemView, false);
        root.addContentView(documentView);
    }

    private TdApi.MessageDocument docFromItem(ChatListItem item2) {
        final ChatListItem item = item2;
        final MessageItem item1 = (MessageItem) item;
        return (TdApi.MessageDocument) item1.msg.message;
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);


        TdApi.MessageDocument doc = docFromItem(item);
        documentView.set(doc.document);
    }
}
