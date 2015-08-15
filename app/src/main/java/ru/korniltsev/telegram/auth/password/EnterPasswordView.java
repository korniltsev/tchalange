package ru.korniltsev.telegram.auth.password;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import flow.Flow;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.auth.phone.EnterPhoneFragment;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;

import javax.inject.Inject;

import static android.text.TextUtils.isEmpty;
import static ru.korniltsev.telegram.core.Utils.textFrom;
import static ru.korniltsev.telegram.core.toolbar.ToolbarUtils.initToolbar;

public class EnterPasswordView extends LinearLayout implements HandlesBack{
//    private final String errorMessageUnknown;
//    private final String errorMessageInvalidCode;
//    private final String errorMessageEmptyCode;
    @Inject EnterPassword.Presenter presenter;
    private EditText password;

    public EnterPasswordView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        ObjectGraphService.inject(ctx, this);
//        errorMessageUnknown = ctx.getString(R.string.unknown_error);
//        errorMessageInvalidCode = ctx.getString(R.string.invalid_code);
//        errorMessageEmptyCode = ctx.getString(R.string.invalid_code_empty);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initToolbar(this)
                .setTitle(R.string.password)
                .addMenuItem(
                        R.menu.auth_send_code,
                        R.id.menu_send_code,
                        new Runnable() {
                            @Override
                            public void run() {
                                sendCode();
                            }
                        }
                );
        password = ((EditText) findViewById(R.id.sms_code));
        password.requestFocus();
        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    sendCode();
                    return true;
                }
                return false;
            }
        });
//        password.addTextChangedListener(new TextWatcherAdapter() {
//            @Override
//            public void afterTextChanged(Editable s) {
//                presenter.codeEntered(s);
//            }
//        });

    }

    private void sendCode() {
        presenter.checkPassword(textFrom(password));
    }

//    public EditText getPassword() {
//        return password;
//    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void showError(Throwable th) {
        password.setError(gerErrorMessageForException(th));
        password.requestFocus();
    }

    private String gerErrorMessageForException(Throwable th) {
        String message = th.getMessage();
//        if (isEmpty(message)) {
//            return errorMessageUnknown;
//        } else if (message.contains("PHONE_CODE_INVALID")) {
//            return errorMessageInvalidCode;
//        } else if (message.contains("PHONE_CODE_EMPTY")) {
//            return errorMessageEmptyCode;
//        } else {
            return message;
//        }
    }

    @Override
    public boolean onBackPressed() {
        AppUtils.flowPushAndRemove(this, null, new FlowHistoryStripper() {
            @Override
            public boolean shouldRemovePath(Object path) {
                return !(path instanceof EnterPhoneFragment);
            }
        }, Flow.Direction.BACKWARD);
        return true;
    }
}
