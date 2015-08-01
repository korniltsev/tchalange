package ru.korniltsev.telegram.chat.keyboard.hack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.MessagePanel;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.emoji.EmojiKeyboardView;
import ru.korniltsev.telegram.core.emoji.ObservableLinearLayout;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class FrameUnderMessagePanelController {
    final TrickyBottomFrame root;
    final MessagePanel messagePanel;
    private final ObservableLinearLayout observableContainer;
    private final TrickyLinearyLayout tricky;
    private int lastKeyboardHeight = 0;

    public FrameUnderMessagePanelController(final TrickyBottomFrame root, MessagePanel messagePanel, final ObservableLinearLayout observableContainer, final TrickyLinearyLayout tricky) {
        this.root = root;
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
                final View child = root.getChildAt(0);
                if (event.getAction() == MotionEvent.ACTION_DOWN
                        && child instanceof EmojiKeyboardView) {
                    tricky.fixHeight();
                    removeViewsAndTrickyMargins();
                }
                return false;
            }
        });
    }

    public void showBotKeyboard(TdApi.ReplyMarkupShowKeyboard replyMarkup) {
        removeViewsAndTrickyMargins();
        String[][] rows = replyMarkup.rows;
        if (rows == null) {
            rows = createRandomKeyboard();
        }
        final Context ctx = root.getContext();
        LinearLayout botReplyKeyboard = new LinearLayout(ctx);
        botReplyKeyboard.setOrientation(LinearLayout.VERTICAL);
        for (String[] rowStr : rows) {
            final LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (String s : rowStr) {
                final Button button = new Button(ctx);
                button.setText(s);
                final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.weight = 1;
                row.addView(button, lp);
            }
            botReplyKeyboard.addView(row, MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        root.addView(botReplyKeyboard, MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Utils.hideKeyboard(messagePanel.getInput());
    }

    private String[][] createRandomKeyboard() {
        return new String[][]{
                {"A", "B"},
                {"C", "D"}
        };
    }

    public boolean dismisAnyKeyboard(){
        if (removeViewsAndTrickyMargins()){
            tricky.resetFixedheight();
            return true;
        }
        return false;
    }
    public boolean removeViewsAndTrickyMargins() {
        if (root.getChildCount() != 0){
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


        Utils.hideKeyboard(messagePanel.getInput());
        final LayoutInflater viewFactory = LayoutInflater.from(root.getContext());
        EmojiKeyboardView view = (EmojiKeyboardView) viewFactory.inflate(R.layout.view_emoji_keyboard, root, false);
        view.setCallback(emojiKeyboardCallback);




        if (keyboardHeight > 0) {
            root.addView(view, MATCH_PARENT, keyboardHeight);
            tricky.fixHeight();
        } else {
            final int height = observableContainer.guessKeyboardHeight();
            root.addView(view, MATCH_PARENT, height);
            tricky.setTrickyMargin(height);
        }
    }
}
