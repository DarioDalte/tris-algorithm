import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

public class MQTTPubPrint {


    public static void main(String[] args) {

        //EMAIL trisser.bot2@gmail.com
        //PASSWORD trisserbot2!
        try {
            int qos = 1;
            String broker = "tcp://localhost:1883";
            String PubId = "127.0.0.1";

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient sampleClient = new MqttClient(broker, PubId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(60);
            connOpts.setKeepAliveInterval(60);
            connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            connOpts.setUserName("mqtt");
            connOpts.setPassword("test".toCharArray());
            System.out.println("Connecting to broker: " + broker);



            sampleClient.setCallback(new MqttCallback() {
                public void connectionLost(Throwable cause) {}

                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    System.out.println(topic + " says: \n" + message.toString());
                    String msg = message.toString();
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(msg);

                    if(!Objects.isNull(json.get("move"))){
                        System.out.println(json.get("move"));
                    }else{
                        System.out.println("Nulla casso");
                    }
                }

                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            sampleClient.connect(connOpts);
            sampleClient.subscribe("dalterio.dario@einaudicorreggio.it/1"); //STANZA 1, DA CAMBIARE DINAMICAMENTE
            sampleClient.subscribe("broadcast");

            System.out.println("Connected");


            String topic = "online/dalterio.dario@einaudicorreggio.it";
            String msg = "True";
            MqttMessage message = new MqttMessage(msg.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
            System.out.println("Message published");


        }catch(MqttException me) {
            System.out.println("Reason :"+ me.getReasonCode());
            System.out.println("Message :"+ me.getMessage());
            System.out.println("Local :"+ me.getLocalizedMessage());
            System.out.println("Cause :"+ me.getCause());
            System.out.println("Exception :"+ me);
            me.printStackTrace();
        }
    }
}
