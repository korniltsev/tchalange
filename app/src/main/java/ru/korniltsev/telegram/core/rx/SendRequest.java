package ru.korniltsev.telegram.core.rx;

import org.drinkless.td.libcore.telegram.TdApi;
import rx.Observable;
import rx.functions.Func1;

public class SendRequest implements Func1<TdApi.TLObject, Observable<TdApi.TLObject>> {

    private final TdApi.TLFunction function;
    private final RXClient client;

    public SendRequest(TdApi.TLFunction function, RXClient client) {
        this.function = function;
        this.client = client;
    }

    @Override
    public Observable<TdApi.TLObject> call(TdApi.TLObject tlObject) {
        return client.sendRx(function);
    }
}
