package ru.korniltsev.telegram.main.passcode;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import flow.Flow;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.flow.pathview.NoAnimationTraversal;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.main.passcode.controller.Controller;
import ru.korniltsev.telegram.main.passcode.controller.PasswordController;
import ru.korniltsev.telegram.profile.media.DropdownPopup;

import javax.inject.Inject;

import java.util.ArrayList;

import static ru.korniltsev.telegram.core.Utils.hideKeyboard;

public class PasscodeView extends FrameLayout implements HandlesBack, NoAnimationTraversal {
    private final DpCalculator calc;
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
        calc = MyApp.from(context).calc;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        toolbar = ToolbarUtils.initToolbar(this)
                //                .setTitle(R.string.passcode_password)
                .customView(R.layout.passcode_custom_view)
                .addMenuItem(R.menu.passcode, R.id.menu_enter_passcode, new Runnable() {
                    @Override
                    public void run() {
                        controller.enterPasscode();
                    }
                })
                .pop();

        final TextView title = (TextView) toolbar.getCustomView();
        assert title != null;

        final Drawable d = getResources().getDrawable(R.drawable.ic_arrow_dropdown);
        assert d != null;
        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        title.setCompoundDrawables(null, null, d, null);
        title.setText(R.string.passcode_title_password);
        title.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDropdown(title);
            }
        });
    }

    private void showDropdown(TextView title) {
        final ArrayList<DropdownPopup.Item> list = new ArrayList<>();
        final Resources res = getResources();
        list.add(new DropdownPopup.Item(res.getString(R.string.password), new Runnable() {
            @Override
            public void run() {
                toggle(PasscodeManager.TYPE_PASSWORD);
            }
        }));
        list.add(new DropdownPopup.Item(res.getString(R.string.pin), new Runnable() {
            @Override
            public void run() {
                toggle(PasscodeManager.TYPE_PIN);
            }
        }));
        final DropdownPopup popup = new DropdownPopup(getContext(), list);
        popup.showAtLocation(title, 0, calc.dp(48), calc.dp(28));
    }

    private void toggle(int typePassword) {
        if (this.lock.setPasswordType == typePassword) {
            return;
        }

        AppUtils.flowPushAndRemove(this,
                new PasscodePath(PasscodePath.TYPE_SET, typePassword),
                new FlowHistoryStripper() {
                    @Override
                    public boolean shouldRemovePath(Object path) {
                        return path instanceof PasscodePath;
                    }
                },
                Flow.Direction.REPLACE);
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
        if (controller != null) {
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
        switch (lock.actionType) {
            case PasscodePath.TYPE_LOCK:
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                type = passcodeManager.getPasswordType();
                toolbar.toolbar.setVisibility(View.GONE);
                break;
            case PasscodePath.TYPE_SET:
                type = PasscodeManager.TYPE_PASSWORD;//todo set
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
        return presenter.path.actionType == PasscodePath.TYPE_LOCK;
    }
}
