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
    final RXClient client;
    final ConcurrentHashMap<Integer, User> users = new ConcurrentHashMap<>();
    final Context ctx;
    @Inject
    public UserHolder(RXClient client, RXAuthState auth, Context ctx) {
        this.client = client;
        this.ctx = ctx;
        client.getGlobalObservableWithBackPressure()
                .compose(new RXClient.FilterAndCastToClass<>(TdApi.UpdateUser.class))
                .subscribe(new ObserverAdapter<TdApi.UpdateUser>() {
                    @Override
                    public void onNext(TdApi.UpdateUser response) {
                        save(response.user);
                    }
                });

        auth.listen()
                .subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
                    @Override
                    public void onNext(RXAuthState.AuthState authState) {
                        if (authState instanceof RXAuthState.StateLogout) {
                            users.clear();
                        }
                    }
                });

        auth.getMe(client).subscribe(new ObserverAdapter<RXAuthState.StateAuthorized>(){
            @Override
            public void onNext(RXAuthState.StateAuthorized response) {
                save(response.user);
            }
        });
    }

    private User save(User user) {
        return users.put(user.id, user);
    }

    public User getUser(int id) {
        return users.get(id);
    }

    public Context getCtx() {
        return ctx;
    }
}
