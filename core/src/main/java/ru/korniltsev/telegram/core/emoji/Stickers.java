package ru.korniltsev.telegram.core.emoji;

import android.util.Log;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class Stickers {
    final RXClient client;
    private List<TdApi.Sticker> ss = new ArrayList<>();
    //maps
    private Map<String, TdApi.Sticker> filePathToStickerInfo = new HashMap<>();

    @Inject
    public Stickers(final RXClient client, final RXAuthState auth) {
        this.client = client;

        handleState(auth.getState());
        auth.listen()
                .subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
                    @Override
                    public void onNext(RXAuthState.AuthState authState) {
                        if (authState instanceof RXAuthState.StateAuthorized) {
                            //                            client.sendSilently(new TdApi.GetContacts());
                        } else {
                            ss.clear();
                        }
                    }
                });

        client.stickerUpdates()
                .subscribe(new ObserverAdapter<TdApi.UpdateStickers>() {
                    @Override
                    public void onNext(TdApi.UpdateStickers response) {
                        requestStickers();
                    }
                });
    }

    private void handleState(RXAuthState.AuthState auth) {
        if (auth instanceof RXAuthState.StateAuthorized) {
            requestStickers();
        }
    }

    private void requestStickers() {
        //   public static class GetStickerSet extends TLFunction {
        //   public static class GetStickerSets extends TLFunction {
        //   public static class GetStickers extends TLFunction {
        //   public static class SearchStickerSet extends TLFunction {
        //   public static class UpdateStickerSet extends TLFunction {
//        client.sendRx(new TdApi.GetContacts())
//                .flatMap(new Func1<TdApi.TLObject, Observable<TdApi.TLObject>>() {
//                    @Override
//                    public Observable<TdApi.TLObject> call(TdApi.TLObject tlObject) {
//                        return client.sendRx(new TdApi.GetStickers(null));
//                    }
//                })
//                .observeOn(mainThread())
//                .subscribe(new ObserverAdapter<TdApi.TLObject>() {
//                    @Override
//                    public void onNext(TdApi.TLObject response) {
//                        updateStickers((TdApi.Stickers) response);
//                    }
//                });
    }

    private void updateStickers(TdApi.Stickers newStickers) {
        ss.clear();
        Log.e("FindStickerBug", "got stickers" + newStickers.stickers.length);
        Collections.addAll(ss, newStickers.stickers);
    }

    public List<TdApi.Sticker> getStickers() {
        return ss;
    }

    public void map(String filePath, TdApi.Sticker sticker) {
        filePathToStickerInfo.put(sticker.sticker.persistentId, sticker);
    }

    public TdApi.Sticker getMappedSticker(String persistentId) {
        return filePathToStickerInfo.get(persistentId);
    }
}

