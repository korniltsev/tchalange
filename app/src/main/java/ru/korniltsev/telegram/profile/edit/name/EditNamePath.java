package ru.korniltsev.telegram.profile.edit.name;

import dagger.Provides;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.ModuleFactory2;

import java.io.Serializable;


public class EditNamePath extends BasePath implements Serializable, ModuleFactory2{

    public EditNamePath() {
    }

    @Override
    public int getRootLayout() {
        return R.layout.profile_edit_name;
    }

    @Override
    public Object createDaggerModule() {
        return new Module(this);
    }

    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    EditNameView.class,
                    FakeToolbar.class,
            }
    )
    public static final class Module {
        final EditNamePath path;

        public Module(EditNamePath path) {
            this.path = path;
        }

        @Provides
        EditNamePath providePath() {
            return path;
        }
    }
}
