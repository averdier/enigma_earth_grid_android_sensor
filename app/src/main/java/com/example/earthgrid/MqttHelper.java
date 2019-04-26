package com.example.earthgrid;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttHelper {
    public MqttAndroidClient client;
    final String host = "tcp://vps505484.ovh.net:1883";
    final String username = "chunk_counter";
    final String password = "chunk_counter";
    final String clientId = "bestClientEver";
    final String subscriptionTopic = "sensors/device02/from_clients";
    final String publishTopic = "sensors/device02/from_device";

    public MqttHelper (Context context) {
        client = new MqttAndroidClient(context, host, clientId);
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.w("mqtt", serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.w("mqtt", message.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        connect();
    }

    public void setCallback (MqttCallbackExtended callback) {
        client.setCallback(callback);
    }

    private void connect () {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(false);
        opts.setUserName(username);
        opts.setPassword(password.toCharArray());

        try {
            client.connect(opts, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    client.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("mqtt", "Failed to connect to: " + host + exception.toString());
                }
            });
        } catch (MqttException ex) {
            Log.e("mqtt", ex.toString());
        }
    }

    private void subscribeToTopic () {
        try {
            client.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("mqtt", "Subscribed");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("mqtt", "Subscription failed");
                }
            });
        } catch (MqttException ex) {
            Log.e("mqtt", "Error during subscription: " + ex.toString());
        }
    }

    public void publish(String data) {
        try {
            MqttMessage message = new MqttMessage(data.getBytes());
            client.publish(publishTopic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
