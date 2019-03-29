package com.tech2020.packge.scl;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.tech2020.packge.MainActivity;
import com.tech2020.packge.R;
import com.tech2020.packge.bo.MqttCustomCb;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;
import java.util.Random;


public class MqttManagerService extends Service {

    MqttAndroidClient mqttAndroidClient;
    String mqttManagerConnectionStatus="DISCONNECTED";

    private final Random mGenerator = new Random();

    final String serverUri = "tcp://m10.cloudmqtt.com:15014";

    String clientId = "cl_";
    final String subscriptionTopic = "example/d";
    final String publishTopic = "example/p";
    final String publishMessage = "Hello World!";

    List lst;


    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    MqttCustomCb mCallBack;


    public MqttManagerService() {
    }


    /** method for clients */
    public int getRandomNumber() {
        return mGenerator.nextInt(100);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */



    public class LocalBinder extends Binder {
        public MqttManagerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MqttManagerService.this;
        }
    }

    // Callback Setter
    public void setMqttCustomCallBack(MqttCustomCb callBack) {
        mCallBack = callBack;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("mqttms","oncreate");
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
            Log.d("myserviceone","not oreo service");
        }
        Toast.makeText(this, "service oncreate", Toast.LENGTH_SHORT).show();


        ////////////////////////////////mqtt code below/////////////////////////
        clientId +=  android.os.Build.MODEL;
        Log.d("mqttms", "client_id:"+clientId);
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            //MqttCustomCb mqcCb = new MqttCustomCb();

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                   // addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    mqttManagerConnectionStatus = "CONNECTED";
                  //  subscribeToTopic(subscriptionTopic);
                    //subscribeToTopic("clin");
                    Log.d("mqttms","reconnect-mqtt");
                } else {
                   // addToHistory("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
               // addToHistory("The Connection was lost.");
                mqttManagerConnectionStatus = "DISCONNECTED";
                Log.d("mqttms","connectlost-mqtt");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
               // addToHistory("Incoming message: " + new String(message.getPayload()));

                Log.d("mqttms","msg_arrived-mqtt"+new String(message.getPayload()));
                mCallBack.sendMsgArrived(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("mqttms","token-mqtt");
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        mqttConnectOptions.setUserName("qmtslniz");
        mqttConnectOptions.setPassword(new String("57VE42P2hXAt").toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic(subscriptionTopic);
                    subscribeToTopic("clin");
                    Log.d("mqttms","connect-success-mqtt");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                   // addToHistory("Failed to connect to: " + serverUri);
                    Log.d("mqttms","connect-failure-mqtt");
                }
            });
        } catch (MqttException ex){
            ex.printStackTrace();
        }

    }

    public void subscribeToTopic(String subscriptionTopic){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                 //   addToHistory("Subscribed!");
                    Log.d("mqttms","subscribed topic-mqtt");
                    //stopSelf();
                    //System.exit(0);
                   // android.os.Process.killProcess(android.os.Process.myPid());

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                  //  addToHistory("Failed to subscribe");
                    Log.d("mqttms","subscribe failure-mqtt");
                }
            });

            // THIS DOES NOT WORK!
      /*      mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
                }
            });*/

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }


    public void publishMessage(String topic, String pubmsg){

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(pubmsg.getBytes());
            Log.d("mqttms", ""+mqttAndroidClient);
            mqttAndroidClient.publish(topic, message);
           // addToHistory("Message Published");
            Log.d("mqttms","msg_published-mqtt");
            if(!mqttAndroidClient.isConnected()){
            //    addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
                Log.d("mqttms","client_not_connected-mqtt");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getMqttManagerConnectStatus(){
        if(mqttAndroidClient.isConnected()){
            return "CONNECTED";
        }
        else{
            return mqttManagerConnectionStatus;
        }
        //return mqttAndroidClient
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("mqttms","onstartcommand");
        //return START_REDELIVER_INTENT;
        return  START_STICKY;
        //return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("mqttms","onbind");
        // TODO: Return the communication channel to the service.
        return binder;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d("mqttms","ondestroy");
        try {
            mqttAndroidClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        Log.d("mqttms","onlowmemory");
        super.onLowMemory();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("mqttms","onunbind");
        return super.onUnbind(intent);
    }
}
