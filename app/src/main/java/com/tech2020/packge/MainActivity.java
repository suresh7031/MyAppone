package com.tech2020.packge;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.tech2020.packge.scl.MqttDataHandlrSevice;
import com.tech2020.packge.scl.MyServiceone;

public class MainActivity extends Activity {
    MyServiceone mService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      //  setContentView(R.layout.activity_main);
      //  Toolbar toolbar = findViewById(R.id.toolbar);
      //  setSupportActionBar(toolbar);

        //startService(new Intent(getBaseContext(), MyServiceone.class));
        Log.d("MainAct", "onCreate");

        //startForegroundService(new Intent(getBaseContext(), MyServiceone.class));
        /*FloatingActionButton fab = findViewById(R.id.fab);
          fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
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


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainAct", "onStart");
        // Bind to LocalService
        //Intent intent = new Intent(this, MyServiceone.class);
        //Intent intent = new Intent(this, MqttManagerService.class);
        Intent intent = new Intent(this, MqttDataHandlrSevice.class);
        try{
            //startService(intent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            }else{
                startService(intent);
            }
            //      bindService(intent, connection, Context.BIND_AUTO_CREATE);
            Log.d("MainAct", "after_bindService");
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        //stopService(new Intent(getBaseContext(), MyServiceone.class));
        Log.d("MainAct", "onStop");
        super.onStop();
      //  unbindService(connection);
    }



    /** Defines callbacks for service binding, passed to bindService() */
    /*
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.d("MainAct", "service_connected");
            MyServiceone.LocalBinder binder = (MyServiceone.LocalBinder) service;
            mService = binder.getService();
            Log.d("MainAct", ""+mService.getRandomNumber());
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("MainAct", "service_disconnected");
            mBound = false;
        }
    };
    */
}
