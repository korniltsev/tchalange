package ru.korniltsev.telegram.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.mortar.ActivityResult;

public class ActivityResultHack extends Activity {

    public static final String EXTRA_REQUEST = "extra_request";
    public static final String EXTRA_REQUEST_CODE = "extra_request_code";

    public static void startActivityForResult(Activity ctx, Intent it, int requestCode){
        final Intent intent = new Intent(ctx, ActivityResultHack.class);
        intent.putExtra(EXTRA_REQUEST, it);
        intent.putExtra(EXTRA_REQUEST_CODE, requestCode);
        ctx.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null){
            Intent i = getIntent().getParcelableExtra(EXTRA_REQUEST);
            int requestCode = getIntent().getIntExtra(EXTRA_REQUEST_CODE, 0);
            startActivityForResult(i, requestCode);
        } else {
//            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MyApp.from(this).activityResult.onNext(new ActivityResult(requestCode, resultCode, data));
        finish();
    }
}
