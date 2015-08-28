package ru.korniltsev.telegram.core.emoji;

import android.util.Log;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static rx.Observable.concat;
import static rx.Observable.just;
import static rx.Observable.merge;
import static rx.Observable.zip;
import static rx.android.schedulers.AndroidSchedulers.mainThread;


public class Stickers {
    final RXClient client;
        private List<TdApi.StickerSet> data = new ArrayList<>();
    //maps
    private Map<String, TdApi.Sticker> filePathToStickerInfo = new HashMap<>();


    public Stickers(final RXClient client, final RXAuthState auth) {
        this.client = client;

        handleState(auth.getState());
        auth.listen()
                .subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
                    @Override
                    public void onNext(RXAuthState.AuthState authState) {
                        if (authState instanceof RXAuthState.StateAuthorized) {
                        } else {
                            data.clear();
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

    class Tuple {
        //        final TdApi.Stickers stickers;
        final TdApi.StickerSets stickerSets;
        final List<TdApi.StickerSet> ss;

        public Tuple(TdApi.StickerSets stickerSets, List<TdApi.StickerSet> ss) {
            this.stickerSets = stickerSets;
            this.ss = ss;
        }
    }

    private void requestStickers() {

        //        final Observable<TdApi.TLObject> stickers = client.sendRx(new TdApi.GetStickers());
        client.sendRx(new TdApi.GetStickerSets(true))
                .flatMap(new Func1<TdApi.TLObject, Observable<Tuple>>() {
                    @Override
                    public Observable<Tuple> call(TdApi.TLObject tlObject) {
                        final TdApi.StickerSets sets = (TdApi.StickerSets) tlObject;
                        //                        final Observable<TdApi.StickerSetInfo> from = Observable.from(
                        //                                Arrays.asList(sets.sets));
                        List<Observable<TdApi.StickerSet>> ss = new ArrayList<Observable<TdApi.StickerSet>>();
                        for (TdApi.StickerSetInfo set : sets.sets) {
                            ss.add(client.sendRx(new TdApi.GetStickerSet(set.id)).map(new Func1<TdApi.TLObject, TdApi.StickerSet>() {
                                @Override
                                public TdApi.StickerSet call(TdApi.TLObject tlObject) {
                                    return (TdApi.StickerSet) tlObject;
                                }
                            }));
                        }
                        final Observable<List<TdApi.StickerSet>> stickerSetList = merge(ss).toList();
                        final Observable<TdApi.StickerSets> just = just(sets);
                        return zip(stickerSetList, just, new Func2<List<TdApi.StickerSet>, TdApi.StickerSets, Tuple>() {
                            @Override
                            public Tuple call(List<TdApi.StickerSet> stickerSets, TdApi.StickerSets stickerSets2) {
                                return new Tuple(stickerSets2, stickerSets);
                            }
                        });
                    }
                })
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<Tuple>() {
                    @Override
                    public void onNext(Tuple response) {
                        save(response);
                    }
                });

    }

    private void save(Tuple response) {
        data.clear();
        for (TdApi.StickerSetInfo set : response.stickerSets.sets) {
            for (TdApi.StickerSet s : response.ss) {
                if (s.id == set.id
                        && s.stickers.length != 0){
                    data.add(s);
                    break;
                }
            }
        }
    }


    public List<TdApi.StickerSet> getStickers() {
        return data;
    }

    public void map(String filePath, TdApi.Sticker sticker) {
        filePathToStickerInfo.put(sticker.sticker.persistentId, sticker);
    }

    public TdApi.Sticker getMappedSticker(String persistentId) {
        return filePathToStickerInfo.get(persistentId);
    }
}

