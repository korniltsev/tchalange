package ru.korniltsev.telegram.profile.my;

import dagger.Provides;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.toolbar.FakeToolbar;
import ru.korniltsev.telegram.core.app.RootModule;
import ru.korniltsev.telegram.core.flow.pathview.BasePath;
import ru.korniltsev.telegram.core.mortar.mortarscreen.WithModule;

import java.io.Serializable;

@WithModule(MyProfilePath.Module.class)
public class MyProfilePath extends BasePath implements Serializable{

    public MyProfilePath() {
    }

    @Override
    public int getRootLayout() {
        return R.layout.profile_my_view;
    }


    @dagger.Module(
            addsTo = RootModule.class,
            injects = {
                    MyProfileView.class,
                    FakeToolbar.class,
            }
    )
    public static final class Module {
        final MyProfilePath path;

        public Module(MyProfilePath path) {
            this.path = path;
        }

        @Provides
        MyProfilePath providePath() {
            return path;
        }
    }
}
