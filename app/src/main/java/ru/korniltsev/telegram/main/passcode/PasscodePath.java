package ru.korniltsev.telegram.main.passcode;

import dagger.Provides;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarflow.NamedPath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(PasscodePath.Module.class)
public class PasscodePath extends BasePath implements Serializable, NamedPath{
    public static final int TYPE_LOCK = 0;
    public static final int TYPE_SET = 1;
    public static final int TYPE_LOCK_TO_CHANGE = 2;
    public static final int UNSPECIFIED = -1;

    public final int actionType;
    public final int setPasswordType;


    public PasscodePath(int type) {
        this.actionType  = type;
        this.setPasswordType = UNSPECIFIED;
    }
    public PasscodePath(int type, int setPasswordType) {
        this.actionType = type;
        this.setPasswordType = setPasswordType;
    }

    @Override
    public int getRootLayout() {
        return R.layout.passcode_view;
    }

    @Override
    public String name() {
        return String.format("%d-%d", actionType, setPasswordType);
    }

    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    PasscodeView.class,
                    FakeToolbar.class,
            }
    )
    public static final class Module {
        final PasscodePath path;

        public Module(PasscodePath path) {
            this.path = path;
        }

        @Provides
        PasscodePath providePath() {
            return path;
        }
    }
}
