package com.maigo.cloud.client;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.maigo.cloud.context.MaigoCloudService;
import com.maigo.cloud.listener.CloudServiceListener;
import com.maigo.cloud.listener.SetAliasCompleteListener;

public class MainActivity extends AppCompatActivity
{
    String TAG = MainActivity.this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaigoCloudService.start(this);
        MaigoCloudService.setAlias("autoalias", new SetAliasCompleteListener()
        {
            @Override
            public void onSetAliasComplete(boolean isSuccess) {
                Log.e(TAG, "autoalias complete");
            }
        });
        MaigoCloudService.setCloudServiceListener(new CloudServiceListener() {
            @Override
            public void onCloudServiceConnect() {
                Log.e(TAG, "onConnect");
            }

            @Override
            public void onReceivePushNotification(String title, String content) {
                Log.e(TAG, "notification title = " + title + " content = " + content);
            }

            @Override
            public void onReceiveTransparentMessage(String title, String content) {
                Log.e(TAG, "message title = " + title + " content = " + content);
            }
        });
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

    public void start(View view)
    {
        MaigoCloudService.start(this);
    }

    public void stop(View view)
    {
        MaigoCloudService.stop();
    }

    public void alias(View view)
    {
        EditText editText = (EditText) findViewById(R.id.editText);
        String alias = editText.getText().toString();
        MaigoCloudService.setAlias(alias, new SetAliasCompleteListener() {
            @Override
            public void onSetAliasComplete(boolean isSuccess) {
                Log.e(TAG, "manual alias complete");
            }
        });
    }
}
