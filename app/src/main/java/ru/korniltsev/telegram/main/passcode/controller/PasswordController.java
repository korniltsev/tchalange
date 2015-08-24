package ru.korniltsev.telegram.main.passcode.controller;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import flow.Flow;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.main.passcode.PasscodePath;
import ru.korniltsev.telegram.main.passcode.PasscodeView;
import ru.korniltsev.telegram.profile.edit.passcode.EditPasscode;

import static junit.framework.Assert.assertNotNull;
import static ru.korniltsev.telegram.core.Utils.textFrom;

public class PasswordController extends Controller {
    private final EditText passcodeField;
    private final TextView passCodeHint;
    private final View logo;
    private final PasscodePath lock;
    final PasscodeManager passcodeManager;
    private final Context ctx;
    private final PasscodeView passcodeView;
    private String firstPassword;

    public PasswordController(PasscodeView passcodeView, PasscodePath lock, PasscodeManager manager) {
        this.passcodeView = passcodeView;
        ctx = passcodeView.getContext();
        this.lock = lock;
        this.passcodeManager = manager;
        LayoutInflater.from(passcodeView.getContext())
                .inflate(R.layout.passcode_view_password, passcodeView, true);

        passcodeField = ((EditText) passcodeView.findViewById(R.id.passcode_field));
        passCodeHint = ((TextView) passcodeView.findViewById(R.id.passcode_hint));
        logo = passcodeView.findViewById(R.id.logo);

        Drawable wrappedDrawable = DrawableCompat.wrap(passcodeField.getBackground());
        DrawableCompat.setTint(wrappedDrawable, Color.WHITE);
        passcodeField.setBackgroundDrawable(wrappedDrawable);
        final DpCalculator calc = MyApp.from(ctx).calc;
        passcodeField.setPadding(0, calc.dp(4f), 0, calc.dp(8f));
        passcodeField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    enterPasscode();
                    return true;
                }
                return false;
            }
        });
        passcodeField.setSingleLine(true);
        passcodeField.requestFocus();
        passcodeField.setTransformationMethod(PasswordTransformationMethod.getInstance());

        switch (lock.actionType) {
            case PasscodePath.TYPE_LOCK:
                logo.setVisibility(View.VISIBLE);
                passCodeHint.setText(R.string.enter_your_password);
                break;
            case PasscodePath.TYPE_SET:
                logo.setVisibility(View.GONE);
                passCodeHint.setText(R.string.choose_your_password);
                break;
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                logo.setVisibility(View.VISIBLE);
                passCodeHint.setText(R.string.enter_your_password);
                break;
        }
    }

    @Override
    public void drop() {

    }

    @Override
    public void enterPasscode() {
        switch (lock.actionType) {
            case PasscodePath.TYPE_LOCK:
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                final boolean unlocked = unlock(textFrom(passcodeField));
                if (!unlocked) {
                    passcodeField.setError(ctx.getString(R.string.wrong_password));
                } else {
                    //                    hideKeyboard();
                }
                break;
            case PasscodePath.TYPE_SET:
                if (firstPassword == null) {
                    final String text = textFrom(passcodeField);
                    if (text.isEmpty()) {
                        passcodeField.setError(ctx.getString(R.string.password_cannot_be_empty));
                        return;
                    }
                    firstPassword = text;
                    passcodeField.getText().clear();
                    passCodeHint.setText(R.string.choose_your_password_2);
                } else {
                    if (firstPassword.equals(textFrom(passcodeField))) {
                        setNewPassword(firstPassword);
                        //                        hideKeyboard();
                    } else {
                        passcodeField.setError("Passwords does not match");
                    }
                }
                break;
        }
    }

    public void setNewPassword(@NonNull String firstPassword) {
        passcodeManager.setPassword(PasscodeManager.TYPE_PASSWORD, firstPassword);
        passcodeManager.setPasscodeEnabled(true);
        Flow.get(ctx)
                .goBack();
    }

    public boolean unlock(String s) {
        if (passcodeManager.unlock(PasscodeManager.TYPE_PASSWORD, s)) {
            passcodeView.unlocked();
            return true;
        }
        return false;
    }
}
