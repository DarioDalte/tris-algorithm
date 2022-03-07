import TicTacToe.Console;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;



public class Main extends Thread {

    private int game;
    private MqttClient sampleClient;
    private static String userName;
    private static String pwd;
    private static int games;
    private String room;
    private boolean myTurn;
    private String enemy;
    private int number;


    private  static String getTextFromMessage(Message message) throws Exception{
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private static String getTextFromMimeMultipart(
            MimeMultipart mimeMultipart)  throws Exception{
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
            }
        }
        return result;
    }


    public Main(int game, String room, boolean myTurn, String enemy, int number) {
        this.game = game;
        this.room = room;
        this.myTurn = myTurn;
        this.enemy = enemy;
        this.number = number;
    }

    @Override
    public void run() {
        new Console(game, room, myTurn, enemy, number);

    }


    public static void main(String[] args) {

        //Dati connessione all'email
        String host = "imap.gmail.com";
        String mailStoreType = "imap";
        String username = "trisser.bot2@gmail.com";
        String password = "trisserbot2!";

        try {


        // create properties
        Properties properties = new Properties();

        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", "993");
        properties.put("mail.imap.starttls.enable", "true");
        properties.put("mail.imap.ssl.trust", host);

        Session emailSession = Session.getDefaultInstance(properties);

        // create the imap store object and connect to the imap server
        Store store = emailSession.getStore("imaps");

        store.connect(host, username, password);

        // create the inbox object and open it
        Folder inbox = store.getFolder("Inbox");
        inbox.open(Folder.READ_WRITE);

        // retrieve the messages from the folder in an array and print it
        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), true));

        int i = 0;
        Message message = messages[i];
        message.setFlag(Flags.Flag.SEEN, true);
        String result = getTextFromMessage(message);
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(result);

            //System.out.println(result);

        //Prendo dall'email il numero di partite che bisogna giocare
        games = Integer.parseInt(json.get("room_instance").toString());


        //Prendo dall'email l'username e la password per conenttermi al broker
        userName = json.get("user").toString();
        pwd = json.get("pwd").toString();


        //Prendo dall'email le room, mi serviranno poi per iscrivermi alle topic
        ArrayList<String> rooms = (ArrayList<String>) json.get("rooms");

        for(i = 0; i< rooms.size(); i++){
            rooms.set(i, rooms.get(i).replaceAll("\\r\\n|\\r|\\n", ""));
        }

            inbox.close(false);
            store.close();



            int qos = 0;
            //CONNESSINE AL BROKER
            String broker = "tcp://localhost:1883";
            String PubId = "130.1.1.1";

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient sampleClient = new MqttClient(broker, PubId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(60);
            connOpts.setKeepAliveInterval(60);
            connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);

            //IN CASO DI PARTITA COL SERVER SCOMMENTARE USERNAME E PASSWORD
            //connOpts.setUserName(userName);
            //connOpts.setPassword(pwd.toCharArray());

            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");

            //Dico al server che sono online
            JSONObject json2 = new JSONObject();
            json2.put("trisser.bot2@gmail.com", "true");

            String topic = "online/trisser.bot2@gmail.com";
            MqttMessage msg = new MqttMessage(json2.toString().getBytes());
            sampleClient.publish(topic, msg);
            sampleClient.subscribe("broadcast");

            sampleClient.setCallback(new MqttCallback() {
                public void connectionLost(Throwable cause) {}

                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println(topic + " says: ");
                    System.out.println(message);
                }
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });



            boolean room1 = false;
            String room1Enemy;
            boolean room2 = false;
            String room2Enemy;


            String[] partsRoom1 = rooms.get(0).split("_");

            room1Enemy = partsRoom1[0];

            if(partsRoom1[0].equals("trisser.bot2@gmail.com")){
                room1Enemy = partsRoom1[1];
                room1 = true;
            }
//            String[] partsRoom2 = rooms.get(1).split("_");
//
//            room2Enemy = partsRoom2[0];
//            if(partsRoom2[0].equals("trisser.bot2@gmail.com")){
//                room2Enemy = partsRoom2[1];
//                room2 = true;
//            }


            //Lancio i thread
            boolean myTurn;
            //NELLE PARTITE CAMBIARE  -----> y < games <------- invece di y < 1
            for(int y = 0; y < 5; y++){

                if(room1){
                    if(y%2 == 0){
                        new Main(y, rooms.get(0), true, room1Enemy, 0).start();
                    }else{
                        new Main(y, rooms.get(0), false, room1Enemy, 0).start();

                    }
                }else{
                    if(y%2 != 0){
                        new Main(y, rooms.get(0), true, room1Enemy, 0).start();
                    }else{
                        new Main(y, rooms.get(0), false, room1Enemy, 0).start();

                    }
                }

                if(room2){
                    if(y%2 == 0){
                       // new Main(y, rooms.get(1), true, room2Enemy, games).start();
                    }else{
                        //new Main(y, rooms.get(1), false, room2Enemy, games).start();

                    }
                }else{
                    if(y%2 != 0){
                        //new Main(y, rooms.get(1), true, room2Enemy, games).start();
                    }else{
                       // new Main(y, rooms.get(1), false, room2Enemy, games).start();

                    }
                }


            }



            //new Main(2, sampleClient).start();


    } catch (Exception e) {
        System.out.println(e.getMessage());
        System.out.println("Non ci sono email");
        System.exit(0);
    }

    }
}

