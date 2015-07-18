package ru.korniltsev.telegram.profile.edit.name;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.textFrom;

public class EditNameView extends LinearLayout {
    @Inject EditNamePresenter presenter;
    private EditText name;
    private EditText lastName;

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
        ToolbarUtils.initToolbar(this)
                .pop()
                .setTitle(R.string.edit_name)
                .addMenuItem(
                        R.menu.auth_send_code,
                        R.id.menu_send_code,
                        new Runnable() {
                            @Override
                            public void run() {
                                presenter.editName(textFrom(name), textFrom(lastName));
                            }
                        }
                );;

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
