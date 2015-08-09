package ru.korniltsev.telegram.chat.adapter;

import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.DocumentView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

public class DocumentVH extends RealBaseVH {

    private final DocumentView documentView;
    private final CustomCeilLayout itemView;

    public DocumentVH(CustomCeilLayout itemView, Adapter adapter) {
        super(itemView, adapter);
        this.itemView = itemView;
        documentView =
                (DocumentView) adapter.getViewFactory().inflate(R.layout.chat_item_document, itemView, false);
        itemView.addContentView(documentView);
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        TextMessageVH.newBind(itemView, adapter, item, lastReadOutbox);

        TdApi.Message msg = ((MessageItem) item).msg;
        TdApi.MessageDocument message = (TdApi.MessageDocument) msg.message;
        documentView.set(message.document);
    }
}
