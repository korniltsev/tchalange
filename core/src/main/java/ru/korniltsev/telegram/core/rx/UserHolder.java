package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import org.drinkless.td.libcore.telegram.TdApi;
import static org.drinkless.td.libcore.telegram.TdApi.*;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;

public class UserHolder {
    final ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
    final Context ctx;

    public UserHolder(RXAuthState auth, Context ctx) {
        this.ctx = ctx;

        auth.listen()
                .subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
                    @Override
                    public void onNext(RXAuthState.AuthState authState) {
                        if (authState instanceof RXAuthState.StateLogout) {
                            users.clear();
                        }
                    }
                });
    }

    public User save(User user) {
        return users.put(user.id, user);
    }

    public User getUser(int id) {
        return users.get(id);
    }

    public Context getCtx() {
        return ctx;
    }

    ConcurrentHashMap<Integer, UserFull> userFulls = new ConcurrentHashMap<>();


    public Observable<UserFull> getUserFull(RXClient client, int userId) {
        final UserFull lastKnownUserFull = userFulls.get(userId);
        final Observable<UserFull> request = client.getUserFull(userId).map(new Func1<UserFull, UserFull>() {
            @Override
            public UserFull call(UserFull userFull) {
                userFulls.put(userFull.user.id, userFull);
                return userFull;
            }
        });
        if (lastKnownUserFull == null) {
            return request;
        }
        return Observable.just(lastKnownUserFull)
                .concatWith(request);
    }
}
