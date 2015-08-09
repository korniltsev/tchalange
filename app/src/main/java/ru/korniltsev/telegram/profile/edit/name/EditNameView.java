package ru.korniltsev.telegram.profile.edit.name;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import flow.Flow;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.hideKeyboard;
import static ru.korniltsev.telegram.core.Utils.textFrom;

public class EditNameView extends LinearLayout {
    @Inject EditNamePresenter presenter;
    private EditText name;
    private EditText lastName;
    private ToolbarUtils toolbarUtils;

    public EditNameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        name = (EditText) findViewById(R.id.first_name);
        lastName = (EditText) findViewById(R.id.last_name);

        name.requestFocus();
        toolbarUtils = ToolbarUtils.initToolbar(this)
                .pop(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideKeyboard();
                        Flow.get(getContext())
                                .goBack();
                    }
                })
                .setTitle(R.string.edit_name)
                .addMenuItem(
                        R.menu.auth_send_code,
                        R.id.menu_send_code,
                        new Runnable() {
                            @Override
                            public void run() {
                                saveName();
                            }
                        }
                );
        lastName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveName();
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });

    }

    private void hideKeyboard() {
        Utils.hideKeyboard(name);
        Utils.hideKeyboard(lastName);
    }

    private void saveName() {
        presenter.editName(textFrom(name), textFrom(lastName));
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

    public void bindUser(TdApi.User user) {
        name.setText(user.firstName);
        lastName.setText(user.lastName);
        name.requestFocus();
    }
}
