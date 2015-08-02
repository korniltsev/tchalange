package ru.korniltsev.telegram.chat.keyboard.hack;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.Emoji;
import ru.korniltsev.telegram.core.emoji.EmojiKeyboardView;
import ru.korniltsev.telegram.core.emoji.ObservableLinearLayout;
import ru.korniltsev.telegram.core.rx.EmojiParser;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class FrameUnderMessagePanelController {
    final TrickyBottomFrame root;
    final MessagePanel messagePanel;
    private final ObservableLinearLayout observableContainer;
    private final TrickyLinearyLayout tricky;
    private int lastKeyboardHeight = 0;
    final DpCalculator calc;
    final Emoji emoji;
    private Runnable listener;

    public FrameUnderMessagePanelController(final TrickyBottomFrame root, final MessagePanel messagePanel, final ObservableLinearLayout observableContainer, final TrickyLinearyLayout tricky, DpCalculator calc, Emoji emoji) {
        this.root = root;
        this.calc = calc;
        this.emoji = emoji;
        root.init(observableContainer);
        this.messagePanel = messagePanel;
        this.observableContainer = observableContainer;
        this.tricky = tricky;
        observableContainer.setCallback(new ObservableLinearLayout.CallBack() {
            @Override
            public void onLayout(int keyboardHeight) {
                if (keyboardHeight == 0 && lastKeyboardHeight != 0 && root.getChildCount() == 0) {
                    tricky.resetFixedheight();
                }
                lastKeyboardHeight = keyboardHeight;
            }
        });
        messagePanel.getInput().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (root.getChildCount() == 0) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    tricky.fixHeight();
                    removeViewsAndTrickyMargins();
                    Utils.showKeyboard(messagePanel.getInput());
                }
                return false;
            }
        });
    }

    public void showBotKeyboard(TdApi.ReplyMarkupShowKeyboard replyMarkup) {
        final int keyboardHeight = observableContainer.getKeyboardHeight();
        int viewHeight;
        if (keyboardHeight > 0) {
            viewHeight = keyboardHeight;
        } else {
            viewHeight =observableContainer.guessKeyboardHeight();
        }

        removeViewsAndTrickyMargins();
        String[][] rows = replyMarkup.rows;
        if (rows == null) {//todo delete
            rows = createRandomKeyboard();
        }
        View targetView = createBotKeyboardView(rows, viewHeight);


        if (keyboardHeight > 0) {
            root.addView(targetView, MATCH_PARENT, viewHeight);
            fixHeight();
            Utils.hideKeyboard(messagePanel.getInput());
        } else {
            root.addView(targetView, MATCH_PARENT, viewHeight);
            tricky.setTrickyMargin(viewHeight);
        }
        listener.run();
    }

    @NonNull
    private View createBotKeyboardView(String[][] rows, int viewHeight) {
        int rowHeight = calc.dp(56);
        int leftRightPadding = calc.dp(15);
        int topBottomPadding = calc.dp(6);
        boolean scrollable = rows.length  * rowHeight > viewHeight;
        final Context ctx = root.getContext();
        LinearLayout botReplyKeyboard = new LinearLayout(ctx);
        botReplyKeyboard.setPadding(leftRightPadding, topBottomPadding, leftRightPadding, topBottomPadding);

        botReplyKeyboard.setOrientation(LinearLayout.VERTICAL);
        for (String[] rowStr : rows) {
            final LinearLayout row = new LinearLayout(ctx);
            final int dp5 = calc.dp(5);
            row.setPadding(0, dp5, 0, dp5);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0, rowStrLength = rowStr.length; i < rowStrLength; i++) {
                String s = rowStr[i];
                final Button button = new Button(ctx);
                button.setText(
                        emoji.replaceEmoji(s));
                final LinearLayout.LayoutParams lp;
                if (scrollable) {
                    lp = new LinearLayout.LayoutParams(0, WRAP_CONTENT);
                } else {
                    lp = new LinearLayout.LayoutParams(0, MATCH_PARENT);
                }
                lp.weight = 1;
                button.setBackgroundResource(R.drawable.btn_bot_reply);
                int leftPadding;
                int rightPadding;
                if (i == 0 ) {
                    leftPadding = 0;
                } else {
                    leftPadding = dp5;
                }
                if (i == rowStrLength - 1){
                    rightPadding = 0;
                } else {
                    rightPadding = dp5;
                }
                lp.leftMargin = leftPadding;
                lp.rightMargin = rightPadding;
//                button.setPadding(leftPadding, 0, rightPadding, 0);
                row.addView(button, lp);
            }
            if (scrollable) {
                botReplyKeyboard.addView(row, MATCH_PARENT, rowHeight);
            } else {
                final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1);
                botReplyKeyboard.addView(row, lp);
            }
        }

        View result;
        if (scrollable) {
            final ScrollView scroll = new ScrollView(ctx);
            scroll.addView(botReplyKeyboard, MATCH_PARENT, WRAP_CONTENT);
            result = scroll;
        } else {
            result = botReplyKeyboard;
        }
        result.setBackgroundColor(0xffF5F6F7);
        return result;
    }

    private String[][] createRandomKeyboard() {
        return new String[][]{
                {"A", "B"},
                {"C", "D"},
                {"A", "B","A", "B"},
                {"C", "D","D"},
//                {"A", "B"},
//                {"C", "D"},
        };
    }

    public boolean dismisAnyKeyboard() {
        if (removeViewsAndTrickyMargins()) {
            tricky.resetFixedheight();
            return true;
        }
        return false;
    }

    public boolean removeViewsAndTrickyMargins() {
        if (root.getChildCount() != 0) {
            root.removeAllViews();
            tricky.setTrickyMargin(0);
            return true;
        } else {
            tricky.setTrickyMargin(0);
            return false;
        }
    }

    public void showEmoji(EmojiKeyboardView.CallBack emojiKeyboardCallback) {
        final int keyboardHeight = observableContainer.getKeyboardHeight();
        removeViewsAndTrickyMargins();

        final LayoutInflater viewFactory = LayoutInflater.from(root.getContext());
        EmojiKeyboardView view = (EmojiKeyboardView) viewFactory.inflate(R.layout.view_emoji_keyboard, root, false);
        view.setCallback(emojiKeyboardCallback);

        if (keyboardHeight > 0) {
            root.addView(view, MATCH_PARENT, keyboardHeight);
            fixHeight();
            Utils.hideKeyboard(messagePanel.getInput());
        } else {
            final int height = observableContainer.guessKeyboardHeight();
            root.addView(view, MATCH_PARENT, height);
            tricky.setTrickyMargin(height);
        }
        listener.run();
    }

    public void fixHeight() {
        tricky.fixHeight();
    }

    public void setListener(Runnable listener) {
        this.listener = listener;
    }

    public Runnable getListener() {
        return listener;
    }
}
