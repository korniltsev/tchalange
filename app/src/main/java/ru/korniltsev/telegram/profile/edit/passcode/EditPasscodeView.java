package ru.korniltsev.telegram.profile.edit.passcode;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.view.AnimatedCheckbox;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import javax.inject.Inject;

public class EditPasscodeView extends LinearLayout {
    @Inject EditPasscodePresenter presenter;
    private AnimatedCheckbox passCodeEnabledCheckbox;
    //    private RecyclerView list;

    public EditPasscodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

//        list = ((RecyclerView) findViewById(R.id.list));
//        list.setLayoutManager(new LinearLayoutManager(getContext()));
        passCodeEnabledCheckbox = ((AnimatedCheckbox) findViewById(R.id.pass_code_enabled));
        findViewById(R.id.btn_passcode_switcher)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        passCodeEnabledCheckbox.toggle();
                    }
                });
        findViewById(R.id.btn_passcode_change)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
        findViewById(R.id.btn_passcode_timing)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });

        ToolbarUtils.initToolbar(this)
                .pop()
                .setTitle(R.string.edit_name);

    }

    private void setAdapter(boolean passcodeEnabled) {

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

}
