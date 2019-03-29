package com.tech2020.packge.bo;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttCustomCb {

    public interface MqttCstmCb{
        void mqttOnDataArrived(String topic, MqttMessage message);
    }

    MqttCstmCb mqttCstmCb;

    public void setMqttCstmCb(MqttCstmCb cstmCb){
        mqttCstmCb = cstmCb;
    }

    public void sendMsgArrived(String topic, MqttMessage message){
        mqttCstmCb.mqttOnDataArrived(topic, message);
    }
}
