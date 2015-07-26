package ru.korniltsev.telegram.core.rx;

import org.drinkless.td.libcore.telegram.TdApi;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

@Singleton
public class ContactsHelper {
    final RXClient client;
    private final Observable<TdApi.TLObject> cachedContacts;


    @Inject
    public ContactsHelper(RXClient client) {
        this.client = client;
        cachedContacts = client.sendCachedRX(new TdApi.GetContacts());

    }

    public Observable<TdApi.TLObject> getCachedContacts() {
        return cachedContacts;
    }
}
