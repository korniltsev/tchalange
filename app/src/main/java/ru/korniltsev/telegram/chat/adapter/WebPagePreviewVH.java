package ru.korniltsev.telegram.chat.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import com.crashlytics.android.core.CrashlyticsCore;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.PhotoMessageView;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;

class WebPagePreviewVH extends BaseAvatarVH {
    private final PhotoMessageView image;
    private final TextView link;


    public WebPagePreviewVH(View itemView, final Adapter adapter) {
        super(itemView, adapter);
        image = (PhotoMessageView) itemView.findViewById(R.id.image);

        ((View) image.getParent()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLink();
            }
        });
        link = (TextView) itemView.findViewById(R.id.link);
        TextMessageVH.applyTextStyle(link);
    }

    private void openLink() {
        final ChatListItem item = adapter.getItem(getAdapterPosition());
        final MessageItem msg = (MessageItem) item;
        final TdApi.MessageWebPage webPage = (TdApi.MessageWebPage) msg.msg.message;

        try {
            final Uri uri = Uri.parse(webPage.webPage.url);
            final Intent it = new Intent(Intent.ACTION_VIEW, uri);
            itemView.getContext().startActivity(it);
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }
    }

    @Override
    public void bind(ChatListItem item, long lastReadOutbox) {
        super.bind(item, lastReadOutbox);
        TdApi.Message msg = ((MessageItem) item).msg;
        final TdApi.MessageWebPage webPage = (TdApi.MessageWebPage) msg.message;

        if (webPage.webPage.photo.id == 0){
            image.setVisibility(View.GONE);
        } else {
            image.setVisibility(View.VISIBLE);
            image.load(webPage.webPage.photo, null);
        }
        link.setText(webPage.text);
    }
}