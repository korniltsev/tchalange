package ru.korniltsev.telegram.profile.edit.chat.title;

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
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import javax.inject.Inject;

import static ru.korniltsev.telegram.core.Utils.textFrom;

public class EditChatTitleView extends LinearLayout {
    @Inject EditChatTitlePresenter presenter;
    private EditText title;
//    private EditText lastName;
    private ToolbarUtils toolbarUtils;

    public EditChatTitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        title = (EditText) findViewById(R.id.title);

        title.requestFocus();
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
                                saveTitle();
                            }
                        }
                );
        title.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard();
                    saveTitle();
                    return true;
                }
                return false;
            }
        });

    }

    private void hideKeyboard() {
        Utils.hideKeyboard(this);
    }

    private void saveTitle() {
        presenter.editName(textFrom(title));
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

    public void bindTitle(TdApi.Chat user) {
        title.setText(((TdApi.GroupChatInfo) user.type).groupChat.title);
        title.setSelection(title.getText().length());
        title.requestFocus();
    }
}
