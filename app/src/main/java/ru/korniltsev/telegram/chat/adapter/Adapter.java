package ru.korniltsev.telegram.chat.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import com.crashlytics.android.core.CrashlyticsCore;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Chat;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.debug.CustomCeilLayout;
import ru.korniltsev.telegram.core.recycler.BaseAdapter;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.rx.items.BotInfoItem;
import ru.korniltsev.telegram.core.rx.items.ChatListItem;
import ru.korniltsev.telegram.core.rx.items.DaySeparatorItem;
import ru.korniltsev.telegram.core.rx.items.MessageItem;
import ru.korniltsev.telegram.core.rx.items.NewMessagesItem;

public class Adapter extends BaseAdapter<ChatListItem, RealBaseVH> {

    public static final int VIEW_TYPE_PHOTO = 0;
    public static final int VIEW_TYPE_TEXT = 1;
    public static final int VIEW_TYPE_STICKER = 2;
    public static final int VIEW_TYPE_VOICE = 3;
    public static final int VIEW_TYPE_GEO = 4;
    public static final int VIEW_TYPE_VIDEO = 5;
    public static final int VIEW_TYPE_SINGLE_TEXT_VIEW = 6;
    public static final int VIEW_TYPE_CHAT_PHOTO_CHANGED = 7;
    public static final int VIEW_TYPE_DOCUMENT = 8;
    public static final int VIEW_TYPE_DAY_SEPARATOR = 9;
    public static final int VIEW_TYPE_TEXT_FORWARD = 10;
    public static final int VIEW_TYPE_TEXT_FORWARD2 = 11;
    public static final int VIEW_TYPE_GIF = 12;
    public static final int VIEW_TYPE_CONTACT = 13;
    public static final int VIEW_TYPE_NEW_MESSAGES = 14;
    public static final int VIEW_TYPE_WEB_PAGE = 15;
    public static final int VIEW_TYPE_BOT_INFO = 16;
    public static final int VIEW_TYPE_AUDIO = 17;
    public static final int VIEW_TYPE_AUDIO2 = 18;

    final RxGlide picasso;
    private final Chat chatPath;
    private long lastReadOutbox;

    RxChat chat;
    public final int myId;
    final UserHolder userHodler;

    public Adapter(Context ctx, RxGlide picasso, long lastReadOutbox, int myId, Chat chat, UserHolder userHodler) {
        super(ctx);
        this.picasso = picasso;
        this.lastReadOutbox = lastReadOutbox;
        this.myId = myId;
        this.userHodler = userHodler;
        setHasStableIds(true);
        this.chatPath = chat;
    }

    public void setLastReadOutbox(long lastReadOutbox) {
        this.lastReadOutbox = lastReadOutbox;
        notifyDataSetChanged();//todo
    }

    @Override
    public long getItemId(int position) {
        ChatListItem item = getItem(position);
        if (item instanceof MessageItem) {
            TdApi.Message msg = ((MessageItem) item).msg;
            return getIdForMessageItem(msg);
        } else if (item instanceof DaySeparatorItem) {
            return ((DaySeparatorItem) item).id;
        } else if (item instanceof NewMessagesItem) {
            return ((NewMessagesItem) item).id;
        } else if (item instanceof BotInfoItem) {
            return ((BotInfoItem) item).id;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public long getIdForMessageItem(TdApi.Message msg) {
        TdApi.UpdateMessageId upd = chat.getUpdForNewId(msg.id);
        if (upd != null) {
            return upd.oldId;
        }
        return msg.id;
    }

    @Override
    public int getItemViewType(int position) {
        ChatListItem item = getItem(position);
        if (item instanceof MessageItem) {
            MessageItem rawMsg = (MessageItem) item;
            TdApi.MessageContent message = rawMsg.msg.message;
            if (message instanceof TdApi.MessagePhoto) {
                return VIEW_TYPE_PHOTO;
            } else if (message instanceof TdApi.MessageSticker) {
                return VIEW_TYPE_STICKER;
            } else if (message instanceof TdApi.MessageVoice) {
                return VIEW_TYPE_VOICE;
            } else if (message instanceof TdApi.MessageLocation) {
                return VIEW_TYPE_GEO;
            } else if (message instanceof TdApi.MessageVideo) {
                return VIEW_TYPE_VIDEO;
            } else if (message instanceof TdApi.MessageText) {
                if (rawMsg.msg.forwardFromId == 0) {
                    return VIEW_TYPE_TEXT;
                } else {
                    if (position == getItemCount() - 1) {
                        return VIEW_TYPE_TEXT_FORWARD;
                    }
                    ChatListItem nextItem = getItem(position + 1);
                    if (!(nextItem instanceof MessageItem)) {
                        return VIEW_TYPE_TEXT_FORWARD;
                    }
                    TdApi.Message nextMessage = ((MessageItem) nextItem).msg;
                    if (nextMessage.message instanceof TdApi.MessageText) {
                        if (nextMessage.fromId == rawMsg.msg.fromId
                                && nextMessage.forwardFromId != 0
                                && nextMessage.date == rawMsg.msg.date) {
                            return VIEW_TYPE_TEXT_FORWARD2;
                        }
                    }
                    return VIEW_TYPE_TEXT_FORWARD;
                }
            } else if (message instanceof TdApi.MessageChatChangePhoto) {
                return VIEW_TYPE_CHAT_PHOTO_CHANGED;
            } else if (message instanceof TdApi.MessageDocument) {
                TdApi.Document doc = ((TdApi.MessageDocument) message).document;
                if (doc.mimeType.equals("image/gif")) {
                    return VIEW_TYPE_GIF;
                } else {
                    return VIEW_TYPE_DOCUMENT;
                }
            } else if (message instanceof TdApi.MessageContact) {
                return VIEW_TYPE_CONTACT;
            } else if (message instanceof TdApi.MessageWebPage) {
                return VIEW_TYPE_WEB_PAGE;
            } else if (message instanceof TdApi.MessageAudio) {
                try {
                    return getAudioViewType( position);
                } catch (Exception e) {
                    CrashlyticsCore.getInstance().logException(e);
                    return VIEW_TYPE_AUDIO;
                }
            } else {
                return VIEW_TYPE_SINGLE_TEXT_VIEW;
            }
        } else if (item instanceof NewMessagesItem) {
            return VIEW_TYPE_NEW_MESSAGES;
        } else if (item instanceof BotInfoItem) {
            return VIEW_TYPE_BOT_INFO;
        } else {
            return VIEW_TYPE_DAY_SEPARATOR;
        }
    }

    int getAudioViewType( int position) {
        final MessageItem currentItem= (MessageItem) getItem(position);
        if (position + 1 < getItemCount() - 1) {
            final ChatListItem item = getItem(position + 1);
            if (item instanceof MessageItem) {
                final MessageItem item1 = (MessageItem) item;
                if (item1.msg.message instanceof TdApi.MessageAudio) {
                    if (currentItem.msg.fromId == item1.msg.fromId) {
                        return VIEW_TYPE_AUDIO;
                    } else {
                        return VIEW_TYPE_AUDIO2;
                    }
                } else {
                    return VIEW_TYPE_AUDIO;
                }
            } else {
                return VIEW_TYPE_AUDIO;
            }
        } else {
            return VIEW_TYPE_AUDIO;
        }
    }

    private View inflate(int id, ViewGroup parent) {
        return getViewFactory().inflate(id, parent, false);
    }

    @Override
    public RealBaseVH onCreateViewHolder(ViewGroup p, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_PHOTO: {
                return new PhotoMessageVH(cell(), this);
            }
            case VIEW_TYPE_STICKER: {
                return new StickerVH(cell(), this);
            }
            case VIEW_TYPE_VOICE: {
                return new AudioVH(cell(), this);
            }
            case VIEW_TYPE_GEO: {
                return new GeoPointVH(cell(), this);
            }
            case VIEW_TYPE_VIDEO: {

                return new VideoVH(cell(), this);
            }
            case VIEW_TYPE_TEXT: {
                return new TextMessageVH(cell(), this);
            }
            case VIEW_TYPE_TEXT_FORWARD: {
                return new ForwardedTextMessageVH(cell(), this);
            }
            case VIEW_TYPE_TEXT_FORWARD2: {
                View view = inflate(R.layout.chat_item_message_forward2, p);
                return new ForwardedTextMessage2VH(view, this);
            }
            case VIEW_TYPE_CHAT_PHOTO_CHANGED: {
                View view = inflate(R.layout.chat_item_photo_changed, p);
                return new ChatPhotoChangedVH(view, this);
            }
            case VIEW_TYPE_DOCUMENT: {
                return new DocumentVH(cell(), this);
            }
            case VIEW_TYPE_GIF: {
                return new GifDocumentVH(cell(), this);
            }
            case VIEW_TYPE_CONTACT: {
                return new ContactVH(cell(), this);
            }
            case VIEW_TYPE_DAY_SEPARATOR: {
                View view = inflate(R.layout.chat_item_day_separator, p);
                return new DaySeparatorVH(view, this);
            }
            case VIEW_TYPE_NEW_MESSAGES: {
                View view = inflate(R.layout.chat_item_new_messages, p);
                return new NewMessagesVH(view, this);
            }
            case VIEW_TYPE_WEB_PAGE: {
                return new WebPagePreviewVH(cell(), this);
            }
            case VIEW_TYPE_BOT_INFO: {
                View view = inflate(R.layout.chat_item_bot_info, p);
                return new BotInfoVH(view, this);
            }
            default: {
                View view = inflate(R.layout.chat_item_single_text_view, p);
                return new SingleTextViewVH(view, this);
            }
        }
    }

    @NonNull
    private CustomCeilLayout cell() {
        return new CustomCeilLayout(getCtx());
    }

    @Override
    public void onBindViewHolder(RealBaseVH holder, int position) {
        ChatListItem item1 = getItem(position);
        holder.bind(item1, lastReadOutbox);
    }

    public void setChat(RxChat chat) {
        this.chat = chat;
    }

    public UserHolder getUserHolder() {
        return userHodler;
    }

    public Chat getChatPath() {
        return chatPath;
    }
}
