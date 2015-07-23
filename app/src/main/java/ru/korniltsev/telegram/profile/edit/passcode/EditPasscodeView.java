package ru.korniltsev.telegram.profile.edit.passcode;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.view.AnimatedCheckbox;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class EditPasscodeView extends LinearLayout implements HandlesBack{
    @Inject EditPasscodePresenter presenter;
    private AnimatedCheckbox passCodeEnabledCheckbox;
    private List<View> timingViews;
    private View btn_passcode_change;
    private TextView timingValue;

    public EditPasscodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        passCodeEnabledCheckbox = ((AnimatedCheckbox) findViewById(R.id.pass_code_enabled));
        findViewById(R.id.btn_passcode_switcher)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        presenter.toggleClicked();
                    }
                });
        btn_passcode_change = findViewById(R.id.btn_passcode_change);
        btn_passcode_change
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        presenter.changePasscodeClicked();
                    }
                });
        findViewById(R.id.btn_passcode_timing)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        presenter.changeAutoLockTiming();
                    }
                });
        timingValue = ((TextView) findViewById(R.id.btn_passcode_timing_value));

        ToolbarUtils.initToolbar(this)
                .pop()
                .setTitle(R.string.edit_name);

        timingViews = Arrays.asList(
                findViewById(R.id.timing_howto),
                findViewById(R.id.timing_shadow),
                findViewById(R.id.btn_passcode_timing),
                findViewById(R.id.timing_shadow_top));

        final Resources res = getContext().getResources();
        mapping.put(0l, res.getString(R.string.passcode_disabled));
        mapping.put(TimeUnit.MINUTES.toMillis(1), res.getQuantityString(R.plurals.auto_lock_n_minutes, 1, 1));
        mapping.put(TimeUnit.MINUTES.toMillis(5), res.getQuantityString(R.plurals.auto_lock_n_minutes, 5,5));
        mapping.put(TimeUnit.HOURS.toMillis(1), res.getQuantityString(R.plurals.auto_lock_n_hour, 1,1));
        mapping.put(TimeUnit.HOURS.toMillis(5), res.getQuantityString(R.plurals.auto_lock_n_hour, 5,5));
    }

    private LinkedHashMap<Long, String> mapping = new LinkedHashMap<>();

    public Set<Map.Entry<Long, String>> mapping() {
        return mapping.entrySet();
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

    public void bind(boolean enabled, boolean animate, long timing) {
        passCodeEnabledCheckbox.check(enabled, animate);
        btn_passcode_change.setEnabled(enabled);
        bindAutoLockTiming(timing);
        if (enabled) {
            for (View view : timingViews) {
                view.setVisibility(View.VISIBLE);
            }
        } else {
            for (View view : timingViews) {
                view.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        return presenter.hidePopup();
    }

    public void bindAutoLockTiming(long value) {
        timingValue.setText(
                mapping.get(value));
    }
}
