package ru.korniltsev.telegram.core.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.services.concurrency.DependencyPriorityBlockingQueue;
import io.fabric.sdk.android.services.concurrency.PriorityThreadPoolExecutor;
import mortar.MortarScope;
import mortar.dagger1support.ObjectGraphService;
import net.danlew.android.joda.JodaTimeAndroid;
import ru.korniltsev.telegram.chat.BuildConfig;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.Formatters;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.audio.AudioPLayer;
import ru.korniltsev.telegram.core.audio.VoicePlayer;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.emoji.Stickers;
import ru.korniltsev.telegram.core.emoji.images.Emoji;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import ru.korniltsev.telegram.core.mortar.ActivityResult;
import ru.korniltsev.telegram.core.passcode.PasscodeManager;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.ChatDB;
import ru.korniltsev.telegram.core.rx.EmojiParser;
import ru.korniltsev.telegram.core.rx.NotificationManager;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.rx.SharedMediaHelper;
import ru.korniltsev.telegram.core.rx.StaticLayoutCache;
import ru.korniltsev.telegram.core.rx.UserHolder;
import ru.korniltsev.telegram.core.rx.VoiceRecorder;
import rx.subjects.PublishSubject;

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: anatoly
 * Date: 20.04.15
 * Time: 23:45
 */
public class MyApp extends Application {
    private MortarScope rootScope;
    public volatile int displayWidth;
//    public DpCalculator dpCalculator;
//    public RxGlide rxGlide;
    public StaticLayoutCache staticLayoutCache;
    public DpCalculator calc;
    public RxGlide rxGlide;
    public AudioPLayer audioPLayer;
    private ExecutorService emojiExecutorService;
    public Emoji emoji;
    public EmojiParser emojiParser;
    public RXAuthState rxAuthState;
    public RXClient rxClient;
    public UserHolder userHolder;
    public SharedMediaHelper sharedMediaHelper;
    public RxDownloadManager downloadManager;
    public Stickers stickers;
    public NotificationManager notificationManager;
    public ChatDB chatDb;
    public PasscodeManager passcodeManager;
    public ActivityOwner activityOwner;
    public VoiceRecorder voiceRecorder;
    public VoicePlayer voicePlayer;
    public int statusBarHeightResId;
    public int navBarHeightResId;
    public Formatters formatters;

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG){
            ObserverAdapter.ctx = this;
            ObserverAdapter.MAIN_THREAD_HANDLER = AppUtils.MAIN_HANDLER;
        }
//        Debug.startMethodTracing("startup");
        System.setProperty("rx.scheduler.jdk6.purge-force", "true");
        maximizeCurrentThreadPriority();
        //        final CrashlyticsCore.Builder builder = new CrashlyticsCore.Builder();

        final PriorityThreadPoolExecutor threadPoolExecutor = createThreadPoolExecutor();
        Fabric.with(
                new Fabric.Builder(this)
                        .threadPoolExecutor(threadPoolExecutor)
                        .kits(new CrashlyticsCore(), new Answers())
                        .build());
        JodaTimeAndroid.init(this);

        refreshDisplay();

        final Resources res = getResources();
        float density = res.getDisplayMetrics().density;
        calc = new DpCalculator(density);
        formatters = new Formatters(this);

        staticLayoutCache = new StaticLayoutCache();
        final AndroidBackgroundPriorityThreadFactory factory = new AndroidBackgroundPriorityThreadFactory("Emoji/AudioPlayer singleton executor");
        emojiExecutorService = Executors.newSingleThreadExecutor(factory);
        emoji = new Emoji(this, calc, emojiExecutorService);
        emojiParser = new EmojiParser(emoji);
        rxAuthState = new RXAuthState(this);
        userHolder = new UserHolder(rxAuthState, this);
        rxClient = new RXClient(this, rxAuthState, userHolder);
        sharedMediaHelper = new SharedMediaHelper(rxClient);
        downloadManager = new RxDownloadManager(this, rxClient, rxAuthState);
        audioPLayer = new AudioPLayer(this, rxClient, downloadManager);
        rxGlide = new RxGlide(this, downloadManager, rxAuthState);
        stickers = new Stickers(rxClient, rxAuthState);
        notificationManager = new NotificationManager(rxClient, this, rxAuthState);
        chatDb = new ChatDB(this, rxClient, notificationManager, rxAuthState, userHolder, calc, emojiParser,
                new MessageLayoutGenerator(staticLayoutCache, this, calc, userHolder), formatters);
        //todo we can loose some updates between rxClient and chatDB creation
        passcodeManager = new PasscodeManager(this, rxAuthState);
        activityOwner = new ActivityOwner();
        voiceRecorder = new VoiceRecorder(this);
        voicePlayer = new VoicePlayer(this, rxAuthState);

        ObjectGraph graph = ObjectGraph.create(
                new RootModule(this, calc, rxClient, rxAuthState, userHolder, downloadManager, rxGlide, stickers,
                        notificationManager, chatDb, passcodeManager, activityOwner, voiceRecorder, voicePlayer));
        rootScope = MortarScope.buildRootScope()
                .withService(ObjectGraphService.SERVICE_NAME, graph)
                .build("Root");


        statusBarHeightResId = res.getIdentifier("status_bar_height", "dimen", "android");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            navBarHeightResId = res.getIdentifier("navigation_bar_height", "dimen", "android");
        }

    }

    public final PublishSubject<ActivityResult> activityResult = PublishSubject.create();



    private void maximizeCurrentThreadPriority() {
        try {
            final int tid = Process.myTid();
            final int beforeProcessPriority = Process.getThreadPriority(tid);
            final int priority = Thread.currentThread().getPriority();
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
            final int afterProcessPriority = Process.getThreadPriority(tid);
            final int after = Thread.currentThread().getPriority();
            System.out.println();
        } catch (IllegalArgumentException | SecurityException e) {
            logException(e, "err");
        }
    }

    @NonNull
    private PriorityThreadPoolExecutor createThreadPoolExecutor() {
        try {

            final Constructor<PriorityThreadPoolExecutor> c = PriorityThreadPoolExecutor.class.getDeclaredConstructor(int.class, int.class, long.class, TimeUnit.class, DependencyPriorityBlockingQueue.class, ThreadFactory.class);
            c.setAccessible(true);
            return c.newInstance(1, 1, 1L, TimeUnit.SECONDS, new DependencyPriorityBlockingQueue<>(), new AndroidBackgroundPriorityThreadFactory("PriorityThreadPoolExecutor"));
        } catch (Exception e) {
            logException(e, "reflection hack failed");
            return PriorityThreadPoolExecutor.create(1);
        }
    }

    private int logException(Exception e, String msg) {
        return Log.e("MyApp", msg, e);
    }

    @Override
    public Object getSystemService(String name) {
        if (rootScope !=null && rootScope.hasService(name)) {
            return rootScope.getService(name);
        }

        return super.getSystemService(name);
    }

    private void refreshDisplay() {
        this.displayWidth  = getResources().getDisplayMetrics().widthPixels;
    }

//    public MyDsiplay getDisplay() {
//        return display;
//    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        refreshDisplay();
        super.onConfigurationChanged(newConfig);

    }

    public static MyApp from(Context ctx) {
        return (MyApp) ctx.getApplicationContext();
    }

    public static MyApp from(View v) {
        return from(v.getContext());
    }
}
