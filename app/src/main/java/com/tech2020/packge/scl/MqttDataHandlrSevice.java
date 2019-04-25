package com.tech2020.packge.scl;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MqttDataHandlrSevice extends Service implements MqttCustomCb.MqttCstmCb {
    MqttCustomCb mqttCustomCb;
    MqttManagerService mService;
    boolean mBound = false;
    Thread thread;
    Intent intnt;
    Boolean restartMqttmService=false;
    Timer timer;
    String credentialsURL="https://automationwebapi.streamtoweb.com/get_mqtt_cred.php?mac=";
//    String macaddress = getMacID(getApplicationContext());
    String cmdpublishTopic = "/homeautomation/testing/";//+macaddress+"/cmdout";
    String unsolicpublishTopic = "/homeautomation/testing/";//+macaddress+"/unsolic";
    String cmdsubscribeTopic = "/homeautomation/testing/";//+macaddress+"/cmdin";

    public class TimerTaskHelper extends TimerTask {
        @Override
        public void run() {
            Log.d("mqdata","getMqttCredentials failed re- requesting");
            getMqttCredentials(credentialsURL+getMacID(getApplicationContext()));
        }
    }

    public class AlarmReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            Updater();
        }
    }

    public MqttDataHandlrSevice() {
    }

/*    //TimerTask overide method run() below
    @Override
    public void run() {
        Log.d("mqdata","getMqttCredentials failed re- requesting");
        getMqttCredentials(credentialsURL+getMacID(getApplicationContext()));
        timer.cancel();
    }
*/
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
        intnt = new Intent(this, MqttManagerService.class);
        try{
            //startService(intent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                restartMqttmService = false; //SET THIS VARIABLE false to start service as first time
                getMqttCredentials(credentialsURL+getMacID(getApplicationContext()));
               // startForegroundService(intnt);
               // bindService(intnt, connection, Context.BIND_AUTO_CREATE);
                //mqttCustomCb = new MqttCustomCb();
                //ttCustomCb.setMqttCstmCb(this);
            }else{
                restartMqttmService = false; //SET THIS VARIABLE false to start service as first time
                getMqttCredentials(credentialsURL+getMacID(getApplicationContext()));
                //bindService(intnt, connection, Context.BIND_AUTO_CREATE);
            }
            Log.d("mqdata", "after_bindService");
        } catch (Exception e) {
            e.printStackTrace();
        }
        autoUpdateMqtt(true); //set updater to execute every 2 hour
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
                        mService.publishMessage(unsolicpublishTopic+getMacID(getApplicationContext())+"/unsolic", "hello_"+ts+"device_:"+getMacID(getApplicationContext()));
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

    public void restartMqttManagerService(Boolean restart){
        if(restart) {
            stopService(intnt);
            bindService(intnt,connection,Context.BIND_AUTO_CREATE);
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intnt);
            }
            bindService(intnt,connection,Context.BIND_AUTO_CREATE);
            restartMqttmService = true;
        }
    }

    public void onHttpStatus(Boolean success){
        timer = new Timer();
        if(!success){
            TimerTask task = new TimerTaskHelper();
            timer.schedule(task, 5000, 5000);
        }else{
            timer.cancel();
        }
    }

    public void onHresponse(String res){
        //parse json mqtt credentials here and restart mqttManagerService by using
        Log.d("mqdata","httpResponse_\n"+res);
        try {
            JSONObject reader = new JSONObject(res);
            String host=reader.getString("host");
            String port=reader.getString("port");
            String username=reader.getString("username");
            String pass=reader.getString("password");
            //get compared the credentials with already existing and decide whether to restart mqtt service or not below
            SharedPreferences pref = getApplicationContext().getSharedPreferences("MqttPref", 0); // 0 - for private mode

            if(pref.contains("host") && pref.contains("port") && pref.contains("username") && pref.contains("password")){
                if (pref.getString("host", null).equals(host) || pref.getString("port", null).equals(port)
                        || pref.getString("username", null).equals(username) || pref.getString("password", null).equals(pass)) {
                    Log.d("mqdata", "no new Mqtt credentials\n" + res);
                    if(!restartMqttmService){ //if mqtt not started previously
                        restartMqttManagerService(restartMqttmService);
                    }
                    return;
                } else {
                    //if new credentials here restart MqttManagerService by using new credentials
                    Log.d("mqdata", "new Mqtt credentials\n" + res);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("host", host);
                    editor.putString("port", port);
                    editor.putString("username", username);
                    editor.putString("password", pass);
                    editor.commit();
                    Log.d("mqdata", "restart check MqttManagerService_\n" + res);
                    restartMqttManagerService(restartMqttmService);
                }
            }else{
                //if prefernces are not existed
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("host", host);
                editor.putString("port", port);
                editor.putString("username", username);
                editor.putString("password", pass);
                editor.commit();
                Log.d("mqdata", "restart check MqttManagerService_\n" + res);
                restartMqttManagerService(restartMqttmService);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getMqttCredentials(String url){
        // Instantiate the RequestQueue.
        Log.d("mqdata","mqttCredentialsURL: "+url);
        RequestQueue queue = Volley.newRequestQueue(this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        // textView.setText("Response is: "+ response.substring(0,500));
                        //parseMqttCredentials(response);
                        onHresponse(response);
                        onHttpStatus(true);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //textView.setText("That didn't work!");
                Log.d("mqdata", "http request failed\n");
                onHttpStatus(false);
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void Updater(){
        //http url hit code here and pass response to onHresponse method
        Toast.makeText(getBaseContext(),"Updater function",Toast.LENGTH_SHORT).show();
        //String url = "http://10.0.1.91/getInfo";
        restartMqttmService = true;
        getMqttCredentials(credentialsURL+getMacID(getApplicationContext()));
    }

    public void autoUpdateMqtt(Boolean flag)
    {
        AlarmManager alarmManager=(AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this,AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        //boolean on = ((ToggleButton) view).isChecked();
        if (flag)
        {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis(),30000,pendingIntent);
            Toast.makeText(getBaseContext(),"Check-In will be done every 2 hours",Toast.LENGTH_SHORT).show();
        }
        else
        {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(getBaseContext(),"Manual Check-In enabled",Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void mqttOnDataArrived(String topic, MqttMessage message) {
        Log.d("mqdata","msg_arrived-mqtt"+new String(message.getPayload()));
        //Log.d("mqdata",""+mService.getRandomNumber());

        if(topic.equals(cmdsubscribeTopic+getMacID(getApplicationContext())+"/cmdin")){
            String clin=new String(message.getPayload());
            String strArray[] = clin.split("\\s");
            mService.publishMessage(cmdpublishTopic+getMacID(getApplicationContext())+"/cmdout",sudoResult(clin));
        }
        /*else if (topic.equals("http")){
            //String http = new String(message.getPayload());
            getMqttCredentials(credentialsURL+getMacID(getApplicationContext()));
        }*/
    }

    public static String sudoResult(String cmd){
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(reader);
            //int numRead;
            //char[] buffer = new char[5000];
            StringBuffer commandOutput = new StringBuffer();
            /*
            while ((numRead = bufferedReader.read(buffer)) > 0) {
                commandOutput.append(buffer, 0, numRead);
            }*/
            /*below updated*/
            String line;
            while((line = bufferedReader.readLine()) != null){
                commandOutput.append(line+System.getProperty("line.separator"));
            }
            Log.d("cmdout",""+commandOutput);
            bufferedReader.close();
            process.waitFor();

            return commandOutput.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /* get mac address functions below two methods*/
    public static String loadFileAsString(String filePath) throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    public static String getMacID(Context context) {
        String android_id = null;
        try {
            android_id = loadFileAsString("/sys/class/net/eth0/address").toUpperCase().substring(0, 17);
            if (!android_id.equals("") && android_id.contains(":"))
                android_id = android_id.replaceAll(":", "");
            Log.i("LA:MacAddress:firstStep", "" + android_id);
        } catch (Exception e) {
            try {
                Log.i("LA", "finding wifi mac");
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo wInfo = wifiManager.getConnectionInfo();
                String macAddress = wInfo.getMacAddress();
                android_id = macAddress;
                if (!android_id.equals("") && android_id.contains(":"))
                    android_id = android_id.replaceAll(":", "");
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        if (android_id.equals("020000000000")) {
            try {
                List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface nif : all) {
                    if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }
                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(Integer.toHexString(b & 0xFF) + ":");
                    }
                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    android_id = res1.toString();
                    if (!android_id.equals("") && android_id.contains(":"))
                        android_id = android_id.replaceAll(":", "");
                    return android_id;
                }
            } catch (Exception ex) {
            }
            return "020000000000";
        }
        android_id = android_id.replace(":", "");
        return android_id;
    }
    /*end of get mac address methods*/

}
