package ru.korniltsev.telegram.main.passcode;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.hideKeyboard;
import static ru.korniltsev.telegram.core.Utils.showKeyboard;
import static ru.korniltsev.telegram.core.Utils.textFrom;

public class PasscodeView extends FrameLayout implements HandlesBack {
    @Inject PasscodePresenter presenter;
    private ToolbarUtils toolbar;
    private EditText passcodeField;
    private TextView passCodeHint;

    private String firstPassword;
    private PasscodePath lock;
    private View logo;

    public PasscodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbar = ToolbarUtils.initToolbar(this)
                .setTitle(R.string.passcode_password)
                .addMenuItem(R.menu.passcode, R.id.menu_enter_passcode, new Runnable() {
                    @Override
                    public void run() {
                        enterPasscode();
                    }
                })
                .pop();

        passcodeField = ((EditText) findViewById(R.id.passcode_field));
        passCodeHint = ((TextView) findViewById(R.id.passcode_hint));
        logo = findViewById(R.id.logo);

        Drawable wrappedDrawable = DrawableCompat.wrap(passcodeField.getBackground());
        DrawableCompat.setTint(wrappedDrawable, Color.WHITE);
        passcodeField.setBackgroundDrawable(wrappedDrawable);
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
        showKeyboard(passcodeField);
    }

    private void enterPasscode() {
        switch (lock.type) {
            case PasscodePath.TYPE_LOCK:
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                final boolean unlocked = presenter.unlock(textFrom(passcodeField));
                if (!unlocked){
                    passcodeField.setError(getContext().getString(R.string.wrong_password));
                } else {
                    hideKeyboard();
                }
                break;
            case PasscodePath.TYPE_SET:
                if (firstPassword == null){
                    final String text = textFrom(passcodeField);
                    if (text.isEmpty()){
                        passcodeField.setError(getContext().getString(R.string.password_cannot_be_empty));
                        return;
                    }
                    firstPassword = text;
                    passcodeField.getText().clear();
                    passCodeHint.setText(R.string.choose_your_password_2);
                } else {
                    if (firstPassword.equals(textFrom(passcodeField))){
                        presenter.setNewPassword(firstPassword);
                        hideKeyboard();
                    } else {
                        passcodeField.setError("Passwords does not match");
                    }
                }
                break;

        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    @Override
    public boolean onBackPressed() {
        return presenter.onBackPressed();
    }

    public void bindPasscode(PasscodePath lock) {
        this.lock = lock;
        switch (lock.type) {
            case PasscodePath.TYPE_LOCK:
                toolbar.toolbar.setVisibility(View.GONE);
                passCodeHint.setText(R.string.enter_your_password);
                break;
            case PasscodePath.TYPE_SET:
                logo.setVisibility(View.GONE);
                passCodeHint.setText(R.string.choose_your_password);
                break;
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                logo.setVisibility(View.GONE);
                passCodeHint.setText(R.string.enter_your_password);
                break;
        }
    }

    public void hideKeyboard() {
        Utils.hideKeyboard(passcodeField);
    }
}
