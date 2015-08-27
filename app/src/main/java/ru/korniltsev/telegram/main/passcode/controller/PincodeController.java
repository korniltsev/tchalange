package ru.korniltsev.telegram.main.passcode.controller;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import flow.Flow;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.views.RobotoMediumTextView;
import ru.korniltsev.telegram.main.passcode.PasscodePath;
import ru.korniltsev.telegram.main.passcode.PasscodeView;

import java.util.Arrays;
import java.util.List;

import static android.text.TextUtils.isEmpty;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static ru.korniltsev.telegram.core.Utils.textFrom;

public class PincodeController extends Controller {
    private final TextView passcodeField;
    private final TextView passCodeHint;
    private final View logo;
    private final PasscodePath lock;
    final PasscodeManager passcodeManager;
    private final Context ctx;
    private final PasscodeView passcodeView;
    private final DpCalculator calc;
    private String firstPassword;

    public PincodeController(PasscodeView passcodeView, PasscodePath lock, PasscodeManager manager) {
        this.passcodeView = passcodeView;
        ctx = passcodeView.getContext();
        this.lock = lock;
        this.passcodeManager = manager;
        LayoutInflater.from(passcodeView.getContext())
                .inflate(R.layout.passcode_view_pincode, passcodeView, true);

        passcodeField = (TextView) passcodeView.findViewById(R.id.passcode_field);
        passCodeHint = ((TextView) passcodeView.findViewById(R.id.passcode_hint));
        logo = passcodeView.findViewById(R.id.logo);

        Drawable wrappedDrawable = DrawableCompat.wrap(passcodeField.getBackground());
        DrawableCompat.setTint(wrappedDrawable, Color.WHITE);
        passcodeField.setBackgroundDrawable(wrappedDrawable);
        calc = MyApp.from(ctx).calc;
        passcodeField.setPadding(0, calc.dp(4f), 0, calc.dp(8f));
        passcodeField.setTransformationMethod(PasswordTransformationMethod.getInstance());


        switch (lock.actionType) {
            case PasscodePath.TYPE_LOCK:
                logo.setVisibility(View.VISIBLE);
                passCodeHint.setText(R.string.enter_your_pincode);
                break;
            case PasscodePath.TYPE_SET:
                logo.setVisibility(View.GONE);
                passCodeHint.setText(R.string.choose_your_pin);
                break;
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                logo.setVisibility(View.VISIBLE);
                passCodeHint.setText(R.string.enter_your_pincode);
                break;
        }
        createPINKeyboard();
    }

    private void createPINKeyboard() {

        final List<List<Btn>> keypad = Arrays.asList(
                Arrays.asList(
                        new Btn("1", "", 0),
                        new Btn("2", "ABC", 0),
                        new Btn("3", "DEF", 0)
                ),
                Arrays.asList(
                        new Btn("4", "GHI", 0),
                        new Btn("5", "JKL", 0),
                        new Btn("6", "MNO", 0)
                ),
                Arrays.asList(
                        new Btn("7", "PQRS", 0),
                        new Btn("8", "TUV", 0),
                        new Btn("9", "WXYZ", 0)
                ),
                Arrays.asList(
                        new Btn("", "", 0),
                        new Btn("0", "+", 0),
                        new Btn("", "", R.drawable.ic_passcode_delete)
                )
        );

        final LinearLayout keyboard = new LinearLayout(ctx);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        final int dip8 = calc.dp(8f);
        for (List<Btn> btns : keypad) {
            final LinearLayout line = new LinearLayout(ctx);
            line.setOrientation(LinearLayout.HORIZONTAL);

            for (final Btn b : btns) {
                final LinearLayout btn = new LinearLayout(ctx);
                btn.setBackgroundResource(R.drawable.bg_keyboard_tab_white);
                btn.setGravity(Gravity.CENTER);
                btn.setOrientation(LinearLayout.VERTICAL);


                if (b.icon != 0){
                    final ImageView icon = new ImageView(ctx);
                    icon.setImageResource(b.icon);
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            erase();
                        }
                    });
                    btn.addView(icon);
                } else {
                    final RobotoMediumTextView digit = new RobotoMediumTextView(ctx, null);
                    digit.setTextColor(Color.WHITE);
                    digit.setText(b.digit);
                    digit.setTextSize(28f);


                    final TextView abc = new TextView(ctx);
                    abc.setText(b.abc);
                    abc.setTextColor(Color.WHITE);
                    btn.addView(digit, WRAP_CONTENT, WRAP_CONTENT);
                    btn.addView(abc, WRAP_CONTENT, WRAP_CONTENT);



                    if (!isEmpty(b.digit)){
                        btn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                insert(b);
                            }
                        });
                    }
                }

                final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, MATCH_PARENT);
                lp.weight = 1f;
                btn.setLayoutParams(lp);
                line.addView(btn);
                btn.setPadding(dip8, 0, dip8, 0);
            }
            keyboard.addView(line, MATCH_PARENT, calc.dp(74f));
        }

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        lp.bottomMargin = calc.dp(48f);
        keyboard.setPadding(dip8, 0, dip8, 0);
        passcodeView.addView(keyboard, lp);
    }

    private void erase() {
        final String s = textFrom(passcodeField);
        if (isEmpty(s)){
            return;
        }
        passcodeField.setText(s.substring(0, s.length() - 1 ));
    }

    private void insert(Btn b) {
        String prev = textFrom(passcodeField);
        passcodeField.setText(prev + b.digit);
        if (prev.length() + 1 == 4){
            enterPasscode();
        }
    }

    class Btn {
        final String digit;
        final String abc;
        final int icon;

        Btn(String digit, String abc, int icon) {
            this.digit = digit;
            this.abc = abc;
            this.icon = icon;
        }
    }

    @Override
    public void drop() {

    }

    @Override
    public void enterPasscode() {
        switch (lock.actionType) {
            case PasscodePath.TYPE_LOCK:
            case PasscodePath.TYPE_LOCK_TO_CHANGE:
                final boolean unlocked = unlock(textFrom(passcodeField));
                if (!unlocked) {
                    error(R.string.wrong_pin);
                }
                break;
            case PasscodePath.TYPE_SET:
                if (firstPassword == null) {
                    final String text = textFrom(passcodeField);
                    if (text.length() < 4) {
                        error(R.string.pin_cannot_be_empty);
                        return;
                    }
                    firstPassword = text;
                    passcodeField.setText("");
                    passCodeHint.setText(R.string.choose_your_PIN_2);
                } else {
                    if (firstPassword.equals(textFrom(passcodeField))) {
                        setNewPassword(firstPassword);
                    } else {
                        passcodeField.setError("PIN does not match");
                    }
                }
                break;
        }
    }

    private void error(int strResId) {
        final String err = ctx.getString(strResId);
        Toast.makeText(ctx, err, Toast.LENGTH_LONG).show();
        passcodeField.setError("error");
    }

    public void setNewPassword(@NonNull String firstPassword) {
        passcodeManager.setPassword(PasscodeManager.TYPE_PIN, firstPassword);
        passcodeManager.setPasscodeEnabled(true);
        Flow.get(ctx)
                .goBack();
    }

    public boolean unlock(String s) {
        if (passcodeManager.unlock(PasscodeManager.TYPE_PIN, s)) {
            passcodeView.unlocked();
            return true;
        }
        return false;
    }
}
