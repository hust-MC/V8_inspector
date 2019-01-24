package com.example.zhanfang.test;

import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        V8.initV8();

        // 移动前端源文件
        final Application app = MainApplication.getInstance();

        DefaultExtractPolicy extractPolicy = new DefaultExtractPolicy();
        AssetExtractor aE = new AssetExtractor(null);
        String outputDir = app.getFilesDir().getPath() + File.separator;

        // will force deletion of previously extracted files in app/files directories
        // see https://github.com/NativeScript/NativeScript/issues/4137 for reference
        boolean removePreviouslyInstalledAssets = true;
        aE.extractAssets(app, "app", outputDir, extractPolicy, removePreviouslyInstalledAssets);
        extractPolicy.setAssetsThumb(app);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                V8.require("/data/data/com.example.zhanfang.test/files/app/test.js");
                V8.scheduleBreak();
            }
        });

        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    V8Inspector v8Inspector =
                            new V8Inspector(
                                    app.getFilesDir().getAbsolutePath(),
                                    app.getPackageName(),
                                    handler);
                    v8Inspector.start();

                    v8Inspector.waitForDebugger(false);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(V8.stringFromJNI() + V8.stringFromJNI2());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
