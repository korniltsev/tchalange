package ru.korniltsev.telegram.chat.adapter.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.attach_panel.RecentImagesBottomSheet;
import ru.korniltsev.telegram.attach_panel.AttachPanelPopup;
import ru.korniltsev.telegram.chat.bot.BotCommandsAdapter;
import ru.korniltsev.telegram.chat.keyboard.hack.FrameUnderMessagePanelController;
import ru.korniltsev.telegram.chat.Presenter;
import ru.korniltsev.telegram.chat.keyboard.hack.TrickyBottomFrame;
import ru.korniltsev.telegram.chat.keyboard.hack.TrickyFrameLayout;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.Emoji;
import ru.korniltsev.telegram.core.emoji.EmojiKeyboardView;
import ru.korniltsev.telegram.core.emoji.ObservableLinearLayout;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.adapters.TextWatcherAdapter;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;

import javax.inject.Inject;

import java.util.List;

import static ru.korniltsev.telegram.core.Utils.textFrom;

public class MessagePanel extends FrameLayout {

    public static final int LEVEL_SMILE = 0;
    private static final long SCALE_UP_DURAION = 80;
    private static final long SCALE_DOWN_DURATION = 80;
    private final int dip1;
    private ImageView btnLeft;
    private ImageView btnRight;
    private EditText input;

    @Inject Presenter presenter;
    @Inject ActivityOwner activityOwner;
    @Inject Emoji emoji;
    @Inject DpCalculator calc;

    private EmojiKeyboardView.CallBack emojiKeyboardCallback = new EmojiKeyboardView.CallBack() {
        @Override
        public void backspaceClicked() {
            input.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
        }

        @Override
        public void emojiClicked(long code) {
            String strEmoji = emoji.toString(code);
            Editable text = input.getText();
            text.append(emoji.replaceEmoji(strEmoji));
        }

        @Override
        public void stickerCLicked(String stickerFilePath, TdApi.Sticker sticker) {
            presenter.sendSticker(stickerFilePath, sticker);
        }
    };
    private AttachPanelPopup attachPanelPopup;
    private FrameUnderMessagePanelController bottomFrame;
    private View rightButtons;
    @Nullable private ViewPropertyAnimator currentAnimation;
    @Nullable private ViewPropertyAnimator currentAnimation2;


    @Nullable private List<BotCommandsAdapter.Record> botCommands;
    private ImageView btnBotCommand;
    private Runnable onAnyKeyboardShownListener;
    @Nullable TdApi.Message replyMarkup;
    public boolean doNotHideCommandsOnce;
    //    @Nullable private TdApi.ReplyMarkupShowKeyboard replyMarkup;

    public MessagePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        setWillNotDraw(false);

        dip1 = calc.dp(1);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        btnLeft = (ImageView) findViewById(R.id.btn_left);
        btnLeft.setImageLevel(LEVEL_SMILE);
        btnRight = (ImageView) findViewById(R.id.btn_right);
        //        btnRight.setImageLevel(LEVEL_ATTACH);
        input = (EditText) findViewById(R.id.input);
        input.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                animateButtons(s.length() == 0);
            }
        });
        btnRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = textFrom(input);
                //                if (text.length() == 0) {
                //                    showAttachPopup();
                //                } else {
                listener.sendText(
                        text);
                input.setText("");
                //                }
            }
        });

        btnLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (bottomFrame.isEmojiKeyboardShown()) {
                    input.requestFocus();
                    bottomFrame.showRegularKeyboard();
                } else {
                    bottomFrame.showEmoji(emojiKeyboardCallback);
                }
            }
        });
        rightButtons = findViewById(R.id.right_buttons);


        findViewById(R.id.btn_attach).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showAttachPopup();
            }
        });
        btnBotCommand = (ImageView) findViewById(R.id.btn_bot);
        btnBotCommand.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (botCommands != null) {
                    if (replyMarkup != null){
                        showOrHideBotCommands();
                    } else {


                        final Editable text = input.getText();
                        text.clear();
                        text.insert(0, "/");
                        input.requestFocus();

                        if (bottomFrame.isBotKeyboardShown()
                                || bottomFrame.isEmojiKeyboardShown()){
                            doNotHideCommandsOnce = true;//quick dirty hack
                            bottomFrame.showRegularKeyboard();
                        }


//                        if (bottomFrame.isEmojiKeyboardShown()){
//
//                        } else {
//                            bottomFrame.dismisAnyKeyboard();
//                        }
//                        showKeyboard(input);

                    }
                }
                updateBotButtonState();
            }
        });


    }

    private void showOrHideBotCommands() {
        if (bottomFrame.isBotKeyboardShown()){
            input.requestFocus();
            bottomFrame.showRegularKeyboard();
        } else {
            bottomFrame.showBotKeyboard(replyMarkup);
        }
    }

    boolean lastInputIsEmpty = true;

    private void animateButtons(boolean inputIsEmpty) {
        if (inputIsEmpty && !lastInputIsEmpty) {
            if (currentAnimation != null) {
                currentAnimation.cancel();
            }
            currentAnimation = btnRight.animate()
                    .scaleX(0.1f)
                    .scaleY(0.1f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            btnRight.setVisibility(View.GONE);
                        }
                    });
            if (currentAnimation2 != null) {
                currentAnimation2.cancel();
            }
            currentAnimation2 = rightButtons.animate()
                    .translationX(0f)
                    .alpha(1);
        }
        if (!inputIsEmpty && lastInputIsEmpty) {
            //show send button
            if (currentAnimation != null) {
                currentAnimation.cancel();
            }
            btnRight.setVisibility(View.VISIBLE);
            if (btnRight.getScaleX() == 1f) {
                btnRight.setScaleX(0.1f);
                btnRight.setScaleY(0.1f);
            }

            currentAnimation = btnRight.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setListener(null);

            if (currentAnimation2 != null) {
                currentAnimation2.cancel();
            }

            currentAnimation2 = rightButtons.animate()
                    .translationX(rightButtons.getWidth())
                    .alpha(0f);
        }
        lastInputIsEmpty = inputIsEmpty;
    }

    private ObservableLinearLayout getObservableContainer() {
        return (ObservableLinearLayout) getParent().getParent().getParent();
    }

    private void showAttachPopup() {
        attachPanelPopup = RecentImagesBottomSheet.create(activityOwner.expose(), presenter);
    }

    OnSendListener listener;

    public void setListener(OnSendListener listener) {
        this.listener = listener;
    }

    public boolean onBackPressed() {
        if (attachPanelPopup != null && attachPanelPopup.isShowing()) {
            attachPanelPopup.dismiss();
            attachPanelPopup = null;
            return true;
        }
        attachPanelPopup = null;
        return bottomFrame.dismisAnyKeyboard();
    }

    public void hideAttachPannel() {
        if (attachPanelPopup != null) {
            attachPanelPopup.dismiss();
            attachPanelPopup = null;
        }
    }

    public void initBottomFrame(TrickyBottomFrame bottomFrame, TrickyFrameLayout tricky) {
        this.bottomFrame = new FrameUnderMessagePanelController(bottomFrame, this, getObservableContainer(), tricky, calc, emoji);
        this.bottomFrame.setListener(new Runnable() {
            @Override
            public void run() {
                onAnyKeyboardShownListener.run();
                updateBotButtonState();
            }
        });
    }




    public void setCommands(List<BotCommandsAdapter.Record> cs) {
        this.botCommands = cs;
        updateBotButtonState();
    }

    private void updateBotButtonState() {
        int icon = 0;
        if (replyMarkup != null){
            if (bottomFrame.isBotKeyboardShown()){
                icon = R.drawable.ic_msg_panel_kb;
            } else {
                icon =  R.drawable.ic_command;
            }
        } else if (botCommands != null){
            icon = R.drawable.ic_slash;
        }
        if (icon == 0){
            btnBotCommand.setVisibility(View.GONE);
        } else {
            btnBotCommand.setVisibility(View.VISIBLE);
            btnBotCommand.setImageResource(icon);
        }

        if (bottomFrame.isEmojiKeyboardShown()){
            btnLeft.setImageResource(R.drawable.ic_msg_panel_kb);
        } else {
            btnLeft.setImageResource(R.drawable.ic_smiles);
        }

    }

    public void setOnAnyKeyboardShownListener(Runnable onAnyKeyboardShownListener) {
        this.onAnyKeyboardShownListener = onAnyKeyboardShownListener;
    }


    public void setReplyMarkup(TdApi.Message replyMarkup) {
        this.replyMarkup = replyMarkup;
        updateBotButtonState();
    }

    public interface OnSendListener {
        void sendText(String text);
    }

    final Paint p = new Paint();

    {
        p.setColor(0xffd0d0d0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), dip1, p);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Utils.hideKeyboard(input);
    }

    public EditText getInput() {
        return input;
    }

    public FrameUnderMessagePanelController getBottomFrame() {
        return bottomFrame;
    }
}
