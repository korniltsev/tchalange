package ru.korniltsev.telegram.profile.edit.passcode;

import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.common.view.AnimatedCheckbox;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;

import java.io.Serializable;

public class EditPasscode extends BasePath implements Serializable{

    public EditPasscode() {
    }

    @Override
    public int getRootLayout() {
        return R.layout.profile_edit_pass_code;
    }

    @Override
    public Object createDaggerModule() {
        return new Module(this);
    }

    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    EditPasscodeView.class,
                    AnimatedCheckbox.class,
            }
    )
    public static final class Module {
        final EditPasscode path;

        public Module(EditPasscode path) {
            this.path = path;
        }

        @Provides
        EditPasscode providePath() {
            return path;
        }
    }
}
