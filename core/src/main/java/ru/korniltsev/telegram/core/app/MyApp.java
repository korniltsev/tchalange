package ru.korniltsev.telegram.core.app;

import android.app.Application;
import android.content.Context;
import android.support.v4.app.Fragment;
import com.crashlytics.android.Crashlytics;
import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;
import mortar.MortarScope;
import mortar.dagger1support.ObjectGraphService;
import net.danlew.android.joda.JodaTimeAndroid;
import ru.korniltsev.telegram.core.emoji.Stickers;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;

/**
 * User: anatoly
 * Date: 20.04.15
 * Time: 23:45
 */
public class MyApp extends Application {
    private MortarScope rootScope;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        JodaTimeAndroid.init(this);
    }

    @Override public Object getSystemService(String name) {
        if (rootScope == null) {
            ObjectGraph graph = ObjectGraph.create(new RootModule(this));
            rootScope = MortarScope.buildRootScope()
                    .withService(ObjectGraphService.SERVICE_NAME, graph)
                    .build("Root");
            graph.get(Stickers.class);//todo better solution
        }

        if (rootScope.hasService(name)) return rootScope.getService(name);

        return super.getSystemService(name);
    }


}
