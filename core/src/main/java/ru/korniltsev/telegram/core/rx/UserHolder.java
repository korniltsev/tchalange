package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import org.drinkless.td.libcore.telegram.TdApi;
import static org.drinkless.td.libcore.telegram.TdApi.*;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class UserHolder {
    final ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
    final Context ctx;

    @Inject
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
}
