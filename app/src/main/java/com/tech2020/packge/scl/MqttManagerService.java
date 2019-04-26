package com.tech2020.packge.scl;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Random;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


public class MqttManagerService extends Service {

    MqttAndroidClient mqttAndroidClient;
    String mqttManagerConnectionStatus="DISCONNECTED";
    SSLSocketFactory socketFactory;

    private final Random mGenerator = new Random();

    String serverUri = "ssl://m10.cloudmqtt.com:15014";
    String username = "qmtslniz";
    String password = "57VE42P2hXAt";

    String clientId = "cl_";
    //final String subscriptionTopic = "example/d";
    //String macaddress = MqttDataHandlrSevice.getMacID(getApplicationContext());
  //  String cmdpublishTopic = "/homeautomation/testing/"+macaddress+"/cmdout";
    String cmdsubscribeTopic = "/homeautomation/testing/";//+macaddress+"/cmdin";
    String lastwilltopic = "/homeautomation/testing/";//+macaddress+"/lastwill";
   // String unsolicpublishTopic = "/homeautomation/testing/"+macaddress+"/unsolic";

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
        String hostname=null;
        String port=null;
        SharedPreferences preferences=getSharedPreferences("MqttPref",MODE_PRIVATE);
        if(preferences!=null) {
            hostname = preferences.getString("host", null);
            port = preferences.getString("port", null);
            username = preferences.getString("username", null);
            password = preferences.getString("password", null);
        }
        //temporary credentials below for ssl test
      /*  hostname = "m10.cloudmqtt.com";
        port = "25014";
        username = preferences.getString("username", null);
        password = preferences.getString("password", null);*/
        //end here
        serverUri = "ssl://"+hostname+":"+port;
        Log.i("uri", ""+serverUri);
        clientId +=  MqttDataHandlrSevice.getMacID(getApplicationContext());
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
        String macaddress = MqttDataHandlrSevice.getMacID(getApplicationContext());
        mqttConnectOptions.setWill(lastwilltopic+macaddress+"/lastwill",new String("mqtt disconnected from_:"+macaddress).getBytes(),0,true);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(new String(password).toCharArray());
      //  try {
           // SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();
           // socketFactory = SSLContext.getDefault().getSocketFactory();
       //     Log.i("ssl", "try block");

        try {

            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            // From https://www.washington.edu/itconnect/security/ca/load-der.crt
            InputStream is = getApplicationContext().getResources().getAssets().open("m2mqtt_ca.crt");
            InputStream caInput = new BufferedInputStream(is);
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                // System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);
            mqttConnectOptions.setSocketFactory(context.getSocketFactory());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
                    //subscribeToTopic(cmdsubscribeTopic+MqttDataHandlrSevice.getMacID(getApplicationContext())+"/cmdin");
                    subscribeToTopic(cmdsubscribeTopic+"server/"+MqttDataHandlrSevice.getMacID(getApplicationContext()));
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
