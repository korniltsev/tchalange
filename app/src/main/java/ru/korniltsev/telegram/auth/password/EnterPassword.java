package ru.korniltsev.telegram.auth.password;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import dagger.Provides;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.auth.code.EnterCode;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.Serializable;

import static ru.korniltsev.telegram.core.Utils.hideKeyboard;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

/**
 * Created by korniltsev on 21/04/15.
 */
@WithModule(EnterPassword.Module.class)
public class EnterPassword extends BasePath implements Serializable {


    public EnterPassword() {
    }

    @dagger.Module(injects = EnterPasswordView.class, addsTo = RootModule.class)
    public static class Module {
        final EnterPassword code;

        public Module(EnterPassword code) {
            this.code = code;
        }

        @Provides
        EnterPassword provideEnterCode() {
            return code;
        }
    }

    @Override
    public int getRootLayout() {
        return R.layout.auth_set_password;
    }

    @Singleton
    static class Presenter extends ViewPresenter<EnterPasswordView> {
        private final EnterPassword path;
        private final RXClient client;
        private final RXAuthState auth;
        private Observable<TdApi.User> request;
        private Subscription subscription = Subscriptions.empty();
        private ProgressDialog pd;
        //        private ProgressDialog pd;
//        private boolean atLeastOneRequestSent = false;

        @Inject
        public Presenter(EnterPassword path, RXClient client, RXAuthState auth) {
            this.path = path;
            this.client = client;
            this.auth = auth;
        }

        public EnterPassword getPath() {
            return path;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            AppUtils.logEvent("EnterCode.onLoad");
            if (request != null) {
                subscribe();
            }
        }



        @Override
        public void dropView(EnterPasswordView view) {
            super.dropView(view);
            subscription.unsubscribe();
            if (pd != null) {
                pd.dismiss();
            }
        }

        public void checkPassword(String code) {
            if (request != null) {
                return;
            }
//            atLeastOneRequestSent = true;
            TdApi.CheckAuthPassword f = new TdApi.CheckAuthPassword(code);
            request = EnterCode.authorizeAndGetMe(client, f);
            subscribe();
        }

        private void subscribe() {

            pd = new ProgressDialog(getView().getContext());
            subscription = request.subscribe(new ObserverAdapter<TdApi.User>() {
                @Override
                public void onError(Throwable th) {
                    pd.dismiss();
                    request = null;
                    getView().showError(th);
                }

                @Override
                public void onNext(TdApi.User response) {
                    auth.authorized(response);
                    hideKeyboard(getView());
                    pd.dismiss();
                    request = null;
                }
            });

            pd.setMessage(getView().getResources().getString(R.string.please_wait));
            pd.setCanceledOnTouchOutside(false);
            pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    subscription.unsubscribe();
                    request = null;
                }
            });
            pd.show();
        }

//        public void codeEntered(Editable s) {
//            if (atLeastOneRequestSent) {
//                return;
//            }
//            if (s.length() == 5) {
//                checkPassword(s.toString());
//            }
//        }
    }

//    public static Observable<TdApi.User> authorizeAndGetMe(final RXClient client, TdApi.TLFunction f) {
//        return client.sendRx(f)
//                .map(new Func1<TdApi.TLObject, TdApi.AuthStateOk>() {
//                    @Override
//                    public TdApi.AuthStateOk call(TdApi.TLObject tlObject) {
//                        return (TdApi.AuthStateOk) tlObject;
//                    }
//                })
//                .flatMap(new Func1<TdApi.AuthStateOk, Observable<TdApi.TLObject>>() {
//                    @Override
//                    public Observable<TdApi.TLObject> call(TdApi.AuthStateOk authStateOk) {
//                        return client.sendRx(new TdApi.GetMe());
//                    }
//                }).map(new Func1<TdApi.TLObject, TdApi.User>() {
//                    @Override
//                    public TdApi.User call(TdApi.TLObject tlObject) {
//                        return (TdApi.User) tlObject;
//                    }
//                })
//                .cache()
//                .observeOn(mainThread());
//    }
//
//    private static class PasswordException extends RuntimeException {//todo this is fucking awful
//    }
}
