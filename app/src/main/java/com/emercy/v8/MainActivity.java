package com.emercy.v8;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.view.View;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        V8.initV8();


        DefaultExtractPolicy extractPolicy = new DefaultExtractPolicy();
        AssetExtractor aE = new AssetExtractor(null);
        String outputDir = getFilesDir().getPath() + File.separator;

        // will force deletion of previously extracted files in app/files directories
        // see https://github.com/NativeScript/NativeScript/issues/4137 for reference
        boolean removePreviouslyInstalledAssets = true;
        aE.extractAssets(this, "app", outputDir, extractPolicy, removePreviouslyInstalledAssets);
        extractPolicy.setAssetsThumb(this);


        findViewById(R.id.bt_run_js).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                V8.scheduleBreak();
                V8.require("/data/data/com.emercy.test/files/app/console.js");

            }
        });

        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    V8Inspector v8Inspector =
                            new V8Inspector(
                                    getFilesDir().getAbsolutePath(),
                                    getPackageName(),
                                    handler);
                    v8Inspector.start();
                    v8Inspector.waitForDebugger(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

}
