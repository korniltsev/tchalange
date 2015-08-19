package ru.korniltsev.telegram.main.passcode;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.flow.pathview.NoAnimationTraversal;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.main.passcode.controller.Controller;
import ru.korniltsev.telegram.main.passcode.controller.PasswordController;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.hideKeyboard;

public class PasscodeView extends FrameLayout implements HandlesBack , NoAnimationTraversal {
    @Inject PasscodePresenter presenter;
    @Inject PasscodeManager passcodeManager;
    private ToolbarUtils toolbar;
//    private EditText passcodeField;
//    private TextView passCodeHint;

//    private String firstPassword;
    private PasscodePath lock;
    private Controller controller;
    //    private View logo;

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
                        controller.enterPasscode();
                    }
                })
                .pop();



    }

//    private void enterPasscode() {
//        switch (lock.type) {
//            case PasscodePath.TYPE_LOCK:
//            case PasscodePath.TYPE_LOCK_TO_CHANGE:
//                final boolean unlocked = presenter.unlock(textFrom(passcodeField));
//                if (!unlocked){
//                    passcodeField.setError(getContext().getString(R.string.wrong_password));
//                } else {
//                    hideKeyboard();
//                }
//                break;
//            case PasscodePath.TYPE_SET:
//                if (firstPassword == null){
//                    final String text = textFrom(passcodeField);
//                    if (text.isEmpty()){
//                        passcodeField.setError(getContext().getString(R.string.password_cannot_be_empty));
//                        return;
//                    }
//                    firstPassword = text;
//                    passcodeField.getText().clear();
//                    passCodeHint.setText(R.string.choose_your_password_2);
//                } else {
//                    if (firstPassword.equals(textFrom(passcodeField))){
//                        presenter.setNewPassword(firstPassword);
//                        hideKeyboard();
//                    } else {
//                        passcodeField.setError("Passwords does not match");
//                    }
//                }
//                break;
//
//        }
//    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
        if (controller != null){
            controller.drop();
        }
    }

    @Override
    public boolean onBackPressed() {
        return presenter.onBackPressed();
    }

    public void bindPasscode(PasscodePath lock) {
        this.lock = lock;

        int type;
        switch (lock.type) {
            case PasscodePath.TYPE_LOCK:
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                type = passcodeManager.getPasswordType();
                toolbar.toolbar.setVisibility(View.GONE);
                break;
            case PasscodePath.TYPE_SET:
                type = PasscodeManager.TYPE_PASSWORD;
                break;
            default:
                return;
        }
        createControllerFor(type, lock);
    }

    private void createControllerFor(int type, PasscodePath lock) {
        if (type == PasscodeManager.TYPE_PASSWORD) {
            controller = new PasswordController(this, lock, passcodeManager);
        }
    }

    public void hideKeyboard() {
//        Utils.hideKeyboard(passcodeField);
    }

    @Override
    public boolean shouldSkipAnimation() {
        return presenter.path.type == PasscodePath.TYPE_LOCK;
    }
}
