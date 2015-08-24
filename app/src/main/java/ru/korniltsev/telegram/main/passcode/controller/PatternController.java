package ru.korniltsev.telegram.main.passcode.controller;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import org.json.JSONArray;
import org.json.JSONException;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.common.FlowHistoryStripper;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.views.RobotoMediumTextView;
import ru.korniltsev.telegram.main.passcode.PasscodePath;
import ru.korniltsev.telegram.main.passcode.PasscodeView;
import ru.korniltsev.telegram.profile.edit.passcode.EditPasscode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.text.TextUtils.isEmpty;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static ru.korniltsev.telegram.core.Utils.textFrom;

public class PatternController extends Controller {
//    private final TextView passcodeField;
    private final TextView passCodeHint;
    private final View logo;
    private final PasscodePath lock;
    final PasscodeManager passcodeManager;
    private final Context ctx;
    private final PasscodeView passcodeView;
    private final DpCalculator calc;
    private final PatternView patternView;
    @Nullable private JSONArray firstPassword;
    //    private String firstPassword;

    public PatternController(PasscodeView passcodeView, final PasscodePath lock, PasscodeManager manager) {
        this.passcodeView = passcodeView;
        ctx = passcodeView.getContext();
        this.lock = lock;
        this.passcodeManager = manager;
        LayoutInflater.from(passcodeView.getContext())
                .inflate(R.layout.passcode_view_pattern, passcodeView, true);


        passCodeHint = ((TextView) passcodeView.findViewById(R.id.passcode_hint));
        logo = passcodeView.findViewById(R.id.logo);
        patternView = ((PatternView) passcodeView.findViewById(R.id.pattern));
        patternView.setCallback(new PatternView.CallBack() {
            @Override
            public void selected(List<PatternView.Point> points) {
                if (lock.actionType == PasscodePath.TYPE_LOCK
                        || lock.actionType == PasscodePath.TYPE_LOCK_TO_CHANGE){
                    enterPasscode();
                }
            }
        });

        calc = MyApp.from(ctx).calc;



        switch (lock.actionType) {
            case PasscodePath.TYPE_LOCK:
                logo.setVisibility(View.VISIBLE);
                passCodeHint.setText(R.string.enter_your_pattern);
                break;
            case PasscodePath.TYPE_SET:
                logo.setVisibility(View.GONE);
                passCodeHint.setText(R.string.choose_your_pattern);
                break;
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                logo.setVisibility(View.VISIBLE);
                passCodeHint.setText(R.string.enter_your_pattern);
                break;
        }
    }




    @Override
    public void drop() {

    }

    @Override
    public void enterPasscode() {
        final JSONArray passcode = passcodeFromPattern(patternView.getSelectedPoints());
        switch (lock.actionType) {
            case PasscodePath.TYPE_LOCK:
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                final boolean unlocked = unlock(passcode);
                if (!unlocked) {
                    passCodeHint.setError(ctx.getString(R.string.wrong_pattern));
                }
                break;
            case PasscodePath.TYPE_SET:

                if (firstPassword == null) {
                    if (passcode.length() < 4) {
                        passCodeHint.setError(ctx.getString(R.string.pattern_cannot_be_empty));
                        return;
                    }
                    firstPassword = passcode;
                    passCodeHint.setText(R.string.choose_your_pattern_2);
                    patternView.clear();
                } else {
                    if (firstPassword.equals(passcode)) {
                        setNewPassword(firstPassword);
                    } else {
                        passCodeHint.setError("Pattern does not match");
                    }
                }
                break;
        }
    }

    private JSONArray passcodeFromPattern(List<PatternView.Point> points) {
        final JSONArray arr = new JSONArray();
        for (PatternView.Point p : points) {
            arr.put(p.position);
        }
        return arr;
    }
    public void setNewPassword(JSONArray newPasscode) {
        try {
            passcodeManager.setPassword(PasscodeManager.TYPE_PATTERN, newPasscode.toString(0));
            passcodeManager.setPasscodeEnabled(true);
            Flow.get(ctx)
                    .goBack();
        } catch (JSONException e) {
            CrashlyticsCore.getInstance().logException(e);
            passcodeManager.setPasscodeEnabled(false);
        }
    }

    public boolean unlock(JSONArray passcode) {
        try {
            if (passcodeManager.unlock(PasscodeManager.TYPE_PATTERN, passcode.toString(0))) {
                passcodeView.unlocked();
                return true;
            }
        } catch (JSONException e) {
            CrashlyticsCore.getInstance().logException(e);
            passcodeManager.setPasscodeEnabled(false);
        }
        return false;
    }
}
