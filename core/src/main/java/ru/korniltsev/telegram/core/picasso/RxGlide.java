package ru.korniltsev.telegram.core.picasso;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.AndroidBackgroundPriorityThreadFactory;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.utils.bitmap.BitmapPool;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.core.views.RoundTransformation;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.Utils.calculateMemoryCacheSize;

@Singleton
public class RxGlide {
    public static final RoundTransformation ROUND = new RoundTransformation();

    public static final String TELEGRAM_FILE = "telegram.file.";
    private final Picasso picasso;
    private final LruCache cache;

    private Context ctx;

    @Inject
    public RxGlide(Context ctx, RxDownloadManager downlaoder, final RXAuthState auth, BitmapPool pool) {
        this.ctx = ctx;

        cache = pool.createPicassoCache(calculateMemoryCacheSize(ctx));
        picasso = new Picasso.Builder(ctx)
                .memoryCache(cache)
                .addRequestHandler(new TDFileRequestHandler(downlaoder, pool))
                .addRequestHandler(new AlbumCoverRequestHandler())
//                .addRequestHandler(new VideoThumbnailRequestHandler())
                .build();

        auth.listen()
                .filter(new Func1<RXAuthState.AuthState, Boolean>() {
                    @Override
                    public Boolean call(RXAuthState.AuthState authState) {
                        return authState instanceof RXAuthState.StateLogout;
                    }
                })
                .subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
                    @Override
                    public void onNext(RXAuthState.AuthState response) {
                        cache.clear();
                    }
                });
        ctx.registerComponentCallbacks(new ComponentCallbacks2() {
            @Override
            public void onTrimMemory(int level) {
                cache.clear();
            }

            @Override
            public void onConfigurationChanged(Configuration newConfig) {
                //do nothing
            }

            @Override
            public void onLowMemory() {
                cache.clear();
            }
        });
    }

    private static final RxGlide.StubAware<TdApi.GroupChat> STUB_AWARE_GROUP_CHAT = new StubAware<TdApi.GroupChat>() {
        @Override
        public String needStub(TdApi.GroupChat o) {
            TdApi.GroupChat chat = o;
            String title = chat.title;
            if (title.length() > 0) {
                return String.valueOf(
                        Character.toUpperCase(title.charAt(0)));
            }
            return "";
        }
    };

    private static final RxGlide.StubAware<TdApi.User> STUB_AWARE_USER = new StubAware<TdApi.User>() {
        @Override
        public String needStub(TdApi.User o) {
            TdApi.User user = o;
            if (o.type instanceof TdApi.UserTypeDeleted) {
                return "D";
            }
            StringBuilder sb = new StringBuilder();
            if (user.firstName.length() > 0) {
                sb.append(
                        Character.toUpperCase(
                                user.firstName.charAt(0)));
            }
            if (user.lastName.length() > 0) {
                sb.append(
                        Character.toUpperCase(
                                user.lastName.charAt(0)));
            }
            return sb.toString();
        }
    };

    public void loadAvatarForUser(TdApi.User u, int size, AvatarView avatarView) {
        TdApi.File file = u.profilePhoto.small;
        if (file.isEmpty()) {
            if (file.id == 0) {
                loadStub(u, size, avatarView);
                return;
            }
        }
        if (avatarView.isNoPlaceholder()){
            loadPhoto(file, false)
                    .transform(ROUND)
                    .noPlaceholder()
                    .into(avatarView);
        } else {
            loadPhoto(file, false)
                    .transform(ROUND)
                    .placeholder(getStubDrawable(u, size))
                    .into(avatarView);
        }

    }

    /**
     * @param u
     * @param size in px
     * @return
     */
    private void loadStub(TdApi.User u, int size, ImageView target) {
        StubDrawable stub = getStubDrawable(u, size);
        target.setImageDrawable(stub);
        picasso.cancelRequest(target);
    }

    private StubDrawable getStubDrawable(TdApi.User u, int size) {
        String chars = STUB_AWARE_USER.needStub(u);

        return getStubDrawable(chars, u.id, size);
    }


    private StubDrawable getStubDrawable(String chars, int id, int size) {
        StubKey key = new StubKey(id, chars, size);
        StubDrawable stub = stubs.get(key);
        if (stub == null) {
            stub = new StubDrawable(key);
            stubs.put(key, stub);
        }
        return stub;
    }

    private void loadStub(TdApi.GroupChatInfo info, int size, ImageView target) {
        StubDrawable stub = getStubDrawable(info, size);
        target.setImageDrawable(stub);
        picasso.cancelRequest(target);
    }

    private StubDrawable getStubDrawable(TdApi.GroupChatInfo info, int size) {
        String chars = STUB_AWARE_GROUP_CHAT.needStub(info.groupChat);
        return getStubDrawable(chars, info.groupChat.id, size);
    }

    public void loadAvatarForChat(TdApi.Chat chat, int size, AvatarView avatarView) {
        if (chat.type instanceof TdApi.PrivateChatInfo) {
            TdApi.User user = ((TdApi.PrivateChatInfo) chat.type).user;
            loadAvatarForUser(user, size, avatarView);
        } else {
            loadAvatarForGroup(chat, size, avatarView);
        }
    }

    private void loadAvatarForGroup(TdApi.Chat chat, int size, AvatarView avatarView) {
        TdApi.GroupChatInfo info = (TdApi.GroupChatInfo) chat.type;
        TdApi.File file = info.groupChat.photo.small;
        if (file.isEmpty()) {
            if (file.id == 0) {
                loadStub(info, size, avatarView);
                return;
            }
        }
        if (avatarView.isNoPlaceholder()){
            loadPhoto(file, false)
                    .transform(ROUND)
                    .placeholder(avatarView.getDrawable())
                    .into(avatarView);
        } else {
            loadPhoto(file, false)
                    .transform(ROUND)
                    .placeholder(getStubDrawable(info, size))
                    .into(avatarView);
        }

    }

    private final Map<StubKey, StubDrawable> stubs = new HashMap<>();



    public class StubKey {
        final int id;
        final String chars;
        final int size;

        public StubKey(int id, String chars, int size) {
            this.id = id;
            this.chars = chars;
            this.size = size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StubKey stubKey = (StubKey) o;

            if (id != stubKey.id) {
                return false;
            }
            if (size != stubKey.size) {
                return false;
            }
            return chars.equals(stubKey.chars);
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + chars.hashCode();
            result = 31 * result + size;
            return result;
        }
    }

    public RequestCreator loadPhoto(TdApi.File f, boolean webp) {
        return loadPhoto(f, webp, null);
    }

    public RequestCreator loadPhoto(TdApi.File f, boolean webp, @Nullable BitmapPool.Size size) {
        assertTrue(f.id != 0);
//        long start = System.nanoTime();
        final TDFileRequestHandler.TDFileUri load = TDFileRequestHandler.load(f, webp, size);
//        long end = System.nanoTime();
//        if ((end - start) != 0){
//            System.out.println();
//        }
//        Utils.logDuration(start, end, "uri creation");

        return picasso.load(load)
                .stableKey(stableKeyForTdApiFile(f, webp));
    }

    private String stableKeyForTdApiFile(TdApi.File f, boolean webp) {
//        return String.format("id=%d&webp=%b", f.id, webp);
        return f.persistentId;
    }

    public interface StubAware<T> {
        String needStub(T o);
    }

    public static String id(TdApi.File f) {
        return TELEGRAM_FILE + f.id;
    }



    //user only to load not td related stuff
    public Picasso getPicasso() {
        return picasso;
    }
}
