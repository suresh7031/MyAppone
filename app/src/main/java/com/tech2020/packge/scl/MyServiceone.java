package com.tech2020.packge.scl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.tech2020.packge.MainActivity;
import com.tech2020.packge.R;
import com.tech2020.packge.bo.MySthread;

import java.util.Random;

public class MyServiceone extends Service {

    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    // Random number generator
    private final Random mGenerator = new Random();

    public MyServiceone() {

    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MyServiceone getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyServiceone.this;
        }
    }

    /** method for clients */
    public int getRandomNumber() {
        return mGenerator.nextInt(100);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return binder;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification =
                    new Notification.Builder(this, "1")
                            .setContentTitle(getText(R.string.notification_title))
                            .setContentText(getText(R.string.notification_message))
                            //.setSmallIcon(R.drawable.icon)
                            .setContentIntent(pendingIntent)
                            .setTicker(getText(R.string.ticker_text))
                            .build();

            startForeground(1, notification);
        }else {
            Log.d("myserviceone","not oreo service");
        }
        Toast.makeText(this, "service oncreate", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("myserviceone","onStartService");
         Toast.makeText(this, "service onstart", Toast.LENGTH_LONG).show();
        //MySthread mst=new MySthread();
        //mst.start();
        //mst.stop();
        for(int i=0; i<5; i++){
                if(i>=4) {
                    Log.d("myserviceone", "hello" + i);
                    //i=0;
                }
        }
        return START_NOT_STICKY;// super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("myserviceone","onUnbindService");
        Toast.makeText(this, "service onUnbind", Toast.LENGTH_LONG).show();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d("myserviceone","onDestroyService");
        Toast.makeText(this, "service destroyed", Toast.LENGTH_LONG).show();
        super.onDestroy();
    }


}
