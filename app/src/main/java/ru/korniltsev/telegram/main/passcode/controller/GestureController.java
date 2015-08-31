package ru.korniltsev.telegram.main.passcode.controller;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.core.CrashlyticsCore;
import flow.Flow;
import org.json.JSONArray;
import org.json.JSONException;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.main.passcode.PasscodePath;
import ru.korniltsev.telegram.main.passcode.PasscodeView;

import java.util.List;

public class GestureController extends Controller {
    //    private final TextView passcodeField;
    private final TextView passCodeHint;
    private final View logo;
    private final PasscodePath lock;
    final PasscodeManager passcodeManager;
    private final Context ctx;
    private final PasscodeView passcodeView;
    private final DpCalculator calc;
    private final GestureView patternView;
    @Nullable private JSONArray firstPassword;
    //    private String firstPassword;

    public GestureController(PasscodeView passcodeView, final PasscodePath lock, PasscodeManager manager) {
        this.passcodeView = passcodeView;
        ctx = passcodeView.getContext();
        this.lock = lock;
        this.passcodeManager = manager;
        LayoutInflater.from(passcodeView.getContext())
                .inflate(R.layout.passcode_view_gesture, passcodeView, true);

        passCodeHint = ((TextView) passcodeView.findViewById(R.id.passcode_hint));
        logo = passcodeView.findViewById(R.id.logo);
        patternView = ((GestureView) passcodeView.findViewById(R.id.pattern));
        patternView.setCb(new GestureView.Callback() {
            @Override
            public void gestureSelected(GestureView.Gesture g) {
//                if (lock.actionType == PasscodePath.TYPE_LOCK
//                        || lock.actionType == PasscodePath.TYPE_LOCK_TO_CHANGE) {
//                    enterPasscode();
//                }
            }
        });

        calc = MyApp.from(ctx).calc;

        switch (lock.actionType) {
            case PasscodePath.TYPE_LOCK:
                logo.setVisibility(View.VISIBLE);
                passCodeHint.setText(R.string.enter_your_gesture);
                break;
            case PasscodePath.TYPE_SET:
                logo.setVisibility(View.GONE);
                passCodeHint.setText(R.string.choose_your_gesture);
                break;
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                logo.setVisibility(View.VISIBLE);
                passCodeHint.setText(R.string.enter_your_gesture);
                break;
        }
    }

    @Override
    public void drop() {

    }

    @Override
    public void enterPasscode() {
//        final JSONArray passcode = null;//passcodeFromPattern(patternView.getSelectedPoints());
//        switch (lock.actionType) {
//            case PasscodePath.TYPE_LOCK:
//            case PasscodePath.TYPE_LOCK_TO_CHANGE:
//                final boolean unlocked = unlock(passcode);
//                if (!unlocked) {
//                    error(R.string.wrong_pattern);
//                }
//                break;
//            case PasscodePath.TYPE_SET:
//
//                if (firstPassword == null) {
//                    if (passcode.length() < 4) {
//                        error(R.string.pattern_cannot_be_empty);
//                        return;
//                    }
//                    firstPassword = passcode;
//                    passCodeHint.setText(R.string.choose_your_pattern_2);
//                    patternView.clear();
//                } else {
//                    if (firstPassword.equals(passcode)) {
//                        setNewPassword(firstPassword);
//                    } else {
//                        passCodeHint.setError("Pattern does not match");
//                    }
//                }
//                break;
//        }
    }

    private void error(int wrong_pattern) {
        final String string = ctx.getString(wrong_pattern);
        Toast.makeText(ctx, string, Toast.LENGTH_LONG).show();
        passCodeHint.setError("error");
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
