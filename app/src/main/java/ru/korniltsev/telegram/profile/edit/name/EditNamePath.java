package ru.korniltsev.telegram.profile.edit.name;

import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(EditNamePath.Module.class)
public class EditNamePath extends BasePath implements Serializable{
    public final TdApi.User user;

    public EditNamePath(TdApi.User user) {
        this.user = user;
    }

    @Override
    public int getRootLayout() {
        return R.layout.profile_edit_name;
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
