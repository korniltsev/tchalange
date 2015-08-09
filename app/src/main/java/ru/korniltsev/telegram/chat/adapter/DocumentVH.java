package ru.korniltsev.telegram.chat.adapter;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.DocumentView;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

import java.io.File;

public class DocumentVH extends RealBaseVH {

    private final DocumentView documentView;
    private final CustomCeilLayout itemView;

    public DocumentVH(CustomCeilLayout itemView, final Adapter adapter) {
        super(itemView, adapter);
        this.itemView = itemView;
        documentView =
                (DocumentView) adapter.getViewFactory().inflate(R.layout.chat_item_document, itemView, false);
        itemView.addContentView(documentView);
//        documentView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final TdApi.MessageDocument doc = docFromItem(adapter.getItem(getAdapterPosition()));
//                open(doc);
//            }
//        });
    }

//    private void open(TdApi.MessageDocument doc) {
//        File exposed = documentView.downloader.exposeFile(src, Environment.DIRECTORY_DOWNLOADS, null);
//
//        Uri uri = Uri.fromFile(exposed);
//        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        intent.setDataAndType(uri, "video/*");
//        try {
//            getContext().startActivity(intent);
//        } catch (ActivityNotFoundException e) {
//            //todo error
//        }
//    }

    private TdApi.MessageDocument docFromItem(ChatListItem item2) {

        final ChatListItem item = item2;
        final MessageItem item1 = (MessageItem) item;
        return (TdApi.MessageDocument) item1.msg.message;
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        TextMessageVH.newBind(itemView, adapter, item, lastReadOutbox);

        TdApi.MessageDocument doc = docFromItem(item);
        documentView.set(doc.document);
    }
}
